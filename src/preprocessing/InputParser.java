package preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class InputParser {
    private File input;
    private FileReader inputFileReader;
    private BufferedReader bufferedReader;

    public InputParser(File input) throws IOException {
        this.input = input;
        this.inputFileReader = new FileReader(this.input);
        this.bufferedReader = new BufferedReader(this.inputFileReader);
    }

    public Question parse() throws IOException {
        Question question = new Question();
        String id = parseField();
        question.id = Integer.parseInt(id);

        String title = parseField();
        question.title = title;

        String text = parseField();
        question.text = text;

        String tags = parseField();
        question.tags = tags;

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

            char c = (char) bufferedReader.read();
            if(c == '\"'){
                openQuote = !openQuote;
                if(!openQuote){
                    char c2 = (char) bufferedReader.read();
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
}
