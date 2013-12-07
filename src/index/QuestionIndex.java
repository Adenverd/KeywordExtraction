package index;

import comparison.Ranker;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import preprocessing.Question;
import preprocessing.QuestionParser;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QuestionIndex {
    protected File indexFile;
    protected Directory directory;
    protected Analyzer analyzer;
    protected IndexWriterConfig indexWriterConfig;
    protected IndexWriter indexWriter;
    protected IndexReader indexReader;

    public Set<Question> buildIndex(File questionsFile, int numQuestions, int batchSize) throws IOException{
        if(!questionsFile.exists()){
            throw new RuntimeException("Training file doesn't exist");
        }

        QuestionParser questionParser = new QuestionParser(questionsFile);
        Set<Question> questions = new HashSet<Question>();
        Set<Question> failedQuestions = new HashSet<Question>();

        double startTime = System.currentTimeMillis();
        for(int i = 0; i <= numQuestions/batchSize; i++){
            for(int j = 0; j < batchSize; j++){
                questions.add(questionParser.parse());
            }
            failedQuestions.addAll(index(questions));
            questions.clear();
        }

        return failedQuestions;
    }

    /***
     * Creates a new QuestionIndex.
     * @param indexOutputDirectory Location for Lucene to store the index
     */
    public QuestionIndex(File indexOutputDirectory, boolean memFlag, double ramBufferSizeMb) throws IOException{
        if(!indexOutputDirectory.isDirectory() || !indexOutputDirectory.exists()){
            throw new RuntimeException("Output directory doesn't exist");
        }
        indexFile = indexOutputDirectory;
        analyzer = new StandardAnalyzer(Version.LUCENE_46);
        indexWriterConfig = new IndexWriterConfig(Version.LUCENE_46, analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexWriterConfig.setRAMBufferSizeMB(ramBufferSizeMb);

        if(memFlag){
            directory = new MMapDirectory(indexFile);
        }
        else {
            directory = FSDirectory.open(indexFile);
        }

        openIndexWriter();
    }

    /***
     * Adds a set of Questions to the index.
     * @param questions
     * @return A set of Questions that were not properly indexed.
     * @throws IOException
     */
    private Set<Question> index(Set<Question> questions) throws IOException {
        Set<Question> failedQuestions = new HashSet<Question>();
        int counter = 0;

        for(Question question : questions){
            try{
                Document doc = buildDocument(question);
                indexWriter.addDocument(doc);
                counter++;
            } catch (Exception e){
                System.out.println("Failed to add Question " + question.id);
                failedQuestions.add(question);
            }
        }

        try{
            indexWriter.commit();
        } catch (Exception e){
            System.out.println("Failed to commit to IndexWriter: " + e.getMessage());
        }

        return failedQuestions;
    }

    public List<String> getTags(Question question, int numTopDocs, double scoreThreshold) throws IOException{
        openIndexReader();
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        MoreLikeThis moreLikeThis = new MoreLikeThis(indexReader);
        moreLikeThis.setFieldNames(new String[] {"title", "body"}); //search only using title and body fields
        int queryDocId = indexQuery(question); //index the query question and get the Lucene docId
        Query query = moreLikeThis.like(queryDocId); //construct the query

        //Now that query is constructed, delete query question from index
        deleteFromIndex(question);

        TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(numTopDocs, true);

        //search
        indexSearcher.search(query, topScoreDocCollector);

        ScoreDoc[] hits = topScoreDocCollector.topDocs().scoreDocs;

        Ranker<String> ranker = new Ranker<String>();

        //calculate scores for tags of similar documents
        for (int i = 0; i<hits.length; i++){
            Document doc = indexSearcher.doc(hits[i].doc);
            String[] tags = doc.get("tags").split(" "); //get the tags and split on
            //increase the score for this tag by the score of the document
            for(String tag : tags){
                ranker.increase(tag, (double)hits[i].score);
            }
        }

        //if a tag's score is above the specified score threshold, add it to a list to return
        List<String> tags = new ArrayList<String>();
        Map<String, Double> tagScores = ranker.getMap();

        for(String tag : tagScores.keySet()){
            if(ranker.getProportion(tag) >= scoreThreshold){
                tags.add(tag);
            }
        }

        closeIndexReader();
        return tags;
    }

    private Document buildDocument(Question question){
        Document doc = new Document();

        FieldType idFieldType = new FieldType();
        idFieldType.setIndexed(false);
        idFieldType.setStored(true);
        idFieldType.setTokenized(false);
        idFieldType.setStoreTermVectors(false);
        idFieldType.setOmitNorms(true);
        doc.add(new Field("id", String.valueOf(question.id), idFieldType));

        FieldType titleFieldType = new FieldType();
        titleFieldType.setIndexed(true);
        titleFieldType.setStored(true);
        titleFieldType.setTokenized(true);
        titleFieldType.setStoreTermVectors(true);
        titleFieldType.setOmitNorms(false);
        doc.add(new Field("title", question.title, titleFieldType));

        FieldType bodyFieldType = new FieldType();
        bodyFieldType.setIndexed(true);
        bodyFieldType.setStored(true);
        bodyFieldType.setTokenized(true);
        bodyFieldType.setStoreTermVectors(true);
        bodyFieldType.setOmitNorms(false);
        doc.add(new Field("body", question.text, bodyFieldType));

        if(question.tags!= null && !question.tags.equals("")){
            FieldType tagsFieldType = new FieldType();
            tagsFieldType.setIndexed(true);
            tagsFieldType.setStored(true);
            tagsFieldType.setTokenized(false);
            tagsFieldType.setStoreTermVectors(true);
            tagsFieldType.setOmitNorms(true);
            doc.add(new Field("tags", question.tags, tagsFieldType));
        }

        return doc;
    }

    /***
     * Indexes a query and returns the docId because MoreLikeThis is fucking stupid that way. Be sure to delete this docId if it is a query Question.
     * @param question
     * @return
     * @throws IOException
     */
    private int indexQuery(Question question) throws IOException {
        Document doc = buildDocument(question);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexWriter.addDocument(doc);
        indexWriter.commit();

        TopDocs results = indexSearcher.search(new TermQuery(new Term("id", String.valueOf(question.id))), 1);
        return results.scoreDocs[0].doc;
    }

    private void deleteFromIndex(Question question) throws IOException{
        Term term = new Term("id", String.valueOf(question.id));
        closeIndexReader();
        indexWriter.deleteDocuments(term);
        indexWriter.commit();
        openIndexReader();
    }

    public void closeIndexWriter() throws IOException{
        indexWriter.close();
    }

    private void openIndexWriter() throws IOException{
        indexWriter = new IndexWriter(directory, indexWriterConfig);
    }

    private void closeIndexReader() throws IOException{
        indexReader.close();
    }

    private void openIndexReader() throws IOException{
        indexReader = DirectoryReader.open(directory);
    }
}
