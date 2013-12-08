import index.QuestionIndex;
import preprocessing.Question;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static int BATCH_SIZE = 500000;
    public static int NUM_QUESTIONS = 6034195; //6034195
    public static int NUM_TOP_DOCS = 100;
    public static double RAM_BUFFER_SIZE_MB = 8192;
    public static double SCORE_THRESHOLD = .07;

    public static void main(String[] args) throws IOException {
        File trainFile = new File(args[0]);
        File outputDirectory = new File(args[1]);

//        QuestionIndex questionIndex = createIndex(outputDirectory, trainFile);

        QuestionIndex questionIndex = new QuestionIndex(outputDirectory, false, RAM_BUFFER_SIZE_MB);
        double startTime = System.currentTimeMillis();
        List<String> tags = testGetTags(questionIndex);
        System.out.println("Runtime: " + (System.currentTimeMillis()-startTime));
    }

    public static QuestionIndex createIndex(File outputDirectory, File trainFile) throws IOException{
        QuestionIndex questionIndex = new QuestionIndex(outputDirectory, false, RAM_BUFFER_SIZE_MB);
        questionIndex.buildIndex(trainFile, NUM_QUESTIONS, BATCH_SIZE);
        return questionIndex;
    }

    public static List<String> testGetTags(QuestionIndex questionIndex) throws IOException{
        Question testQuestion = new Question();
        testQuestion.id = 10000007;
        testQuestion.title = "How to print out a string in Java";
        testQuestion.text = "I've tried printf(str), print(str), and echo(str) but none of them seem to be working. Can someone tell me how to print out a string in Java?";

        List<String> tags = questionIndex.getTags(testQuestion, NUM_TOP_DOCS, SCORE_THRESHOLD);
        return tags;
    }
}
