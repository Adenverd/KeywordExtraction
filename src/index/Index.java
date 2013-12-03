package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import preprocessing.Question;
import preprocessing.QuestionParser;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Index {

    protected File indexFile;
    protected Directory directory;
    protected Analyzer analyzer;
    protected IndexWriterConfig indexWriterConfig;

    public static void main(String[] args) throws IOException{
        File trainFile = new File(args[0]);
        File outputDir = new File(args[1]);

        if(!trainFile.exists()){
            throw new RuntimeException("Training file doesn't exist");
        }
        if(!outputDir.isDirectory() || !outputDir.exists()){
            throw new RuntimeException("Output directory doesn't exist");
        }

        QuestionParser questionParser = new QuestionParser(trainFile);
        Set<Question> questions = new HashSet<Question>();

        double startTime = System.currentTimeMillis();
        for(int i = 0; i < 100000; i++){
            questions.add(questionParser.parse());
        }

        Index index = new Index(outputDir);
        index.index(questions, false);
        System.out.println("Indexing took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /***
     * Creates a new Index.
     * @param indexOutputDirectory Location for Lucene to store the index
     */
    public Index(File indexOutputDirectory){
        indexFile = indexOutputDirectory;
        analyzer = new StandardAnalyzer(Version.LUCENE_46);
        indexWriterConfig = new IndexWriterConfig(Version.LUCENE_46, analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexWriterConfig.setRAMBufferSizeMB(8192);
    }

    /***
     * Adds a set of Questions to the index.
     * @param questions
     * @param memFlag True to index in memory, false otherwise.
     * @return A set of Questions that were not properly indexed.
     * @throws IOException
     */
    public Set<Question> index(Set<Question> questions, boolean memFlag) throws IOException {
        Set<Question> failedQuestions = new HashSet<Question>();
        if(memFlag){
            directory = new MMapDirectory(indexFile);
        }
        else {
            directory = FSDirectory.open(indexFile);
        }

        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

        for(Question question : questions){
            try{
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

                FieldType tagsFieldType = new FieldType();
                tagsFieldType.setIndexed(true);
                tagsFieldType.setStored(true);
                tagsFieldType.setTokenized(false);
                tagsFieldType.setStoreTermVectors(true);
                tagsFieldType.setOmitNorms(true);
                doc.add(new Field("tags", question.tags, tagsFieldType));

                indexWriter.addDocument(doc);
            } catch (Exception e){
                System.out.println("Failed to add Question " + question.id);
                failedQuestions.add(question);
            }
        }

        try{
            indexWriter.commit();
            indexWriter.close();
        } catch (Exception e){
            System.out.println("Failed to close IndexWriter: " + e.getMessage());
        }

        return failedQuestions;
    }
}
