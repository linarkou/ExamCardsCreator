package examcardscreator;

public class Question
{
    String theme;
    String text;
    int complexity;
    
    public Question(String theme, String text, int complexity)
    {
        this.theme = theme;
        this.text = text;
        this.complexity = complexity;
    }
    
    @Override
    public String toString()
    {
        return theme + ";" + text + ";" + complexity;
    }
    
    public static Question parse(String str)
    {
        String[] elems = str.split(";");
        return new Question(elems[0], elems[1], Integer.parseInt(elems[2]));
    }
}
