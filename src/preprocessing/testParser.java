package preprocessing;

import java.io.File;
import java.io.IOException;

public class testParser {

    public static void main(String[] args) throws IOException {
        File inputFile = new File(args[0]);

        InputParser inputParser = new InputParser(inputFile);
        while(true){
            Question q = inputParser.parse();
            System.out.println("Id:\t\t\t"+q.id);
            System.out.println("Title:\t\t"+q.title);
            System.out.println("Text:\t\t"+q.text);
            System.out.println("Tags:\t\t"+q.tags);
        }
    }
}
