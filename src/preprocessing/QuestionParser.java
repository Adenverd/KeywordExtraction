package preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QuestionParser {
    private File input;
    private FileReader inputFileReader;
    private BufferedReader bufferedReader;

    public QuestionParser(File input) throws IOException {
        this.input = input;
        this.inputFileReader = new FileReader(this.input);
        this.bufferedReader = new BufferedReader(this.inputFileReader);
        bufferedReader.readLine(); //skip the first line of csv info
    }

    public Question parse() throws IOException {
        Question question = new Question();

        String id = parseField();
        question.id = id == null? -1 : Integer.parseInt(id);

        String title = parseField();
        question.title = title;

        String text = parseField();
        question.text = text;

        String tags = parseField();
        question.tags = tags;

        if(id == null || id.isEmpty() || title == null || title.isEmpty() || text == null|| text.isEmpty() || tags == null || tags.isEmpty()){
            return null;
        }

        return question;
    }

    private String parseField() throws IOException {
        boolean inField = false;
        boolean openQuote = false;
        StringBuilder stringBuilder = new StringBuilder();

        //Move bufferedReader to the next occurrence of "
        while(!inField){
            int r = bufferedReader.read();
            if(r == -1){
                return null;
            }
            char c = (char) r;
            if(c == '\"'){
                inField = true;
                openQuote = true;
            }
        }

        while(inField){
            char c;
            try{
                c = (char) bufferedReader.read();
            } catch(IOException e){
                return stringBuilder.toString();
            }

            if(c == '\"'){
                openQuote = !openQuote;
                if(!openQuote){
                    char c2;
                    try{
                        c2 = (char) bufferedReader.read();
                    }
                    catch(IOException e){
                        return stringBuilder.toString();
                    }

                    if(c2 == ',' || c2 == '\r'){
                        inField = false;
                    }
                    else{
                        if (c2 == '\"'){
                            openQuote = !openQuote;
                        }
                        stringBuilder.append(c);
                        stringBuilder.append(c2);
                    }
                }
            }
            else {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

//    private static List<String> parseTags(String tags){
//        List<String> tagList = new ArrayList<String>();
//
//        String[] tagArray = tags.split(" ");
//        for(String t : tagArray){
//            tagList.add(t);
//        }
//
//        return tagList;
//    }
}
