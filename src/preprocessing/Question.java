package preprocessing;

import java.util.List;

public class Question {
    public int id;
    public String title;
    public String text;
    public String tags;

    public Question(){

    }

    public Question(int id, String title, String text, String tags){
        this.id = id;
        this.title = title;
        this.text = text;
        this.tags = tags;
    }
}
