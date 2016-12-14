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
    
    public Question(String str)
    {
        String[] elems = str.split(";");
        this.theme = elems[0];
        this.text = elems[1];
        this.complexity = Integer.parseInt(elems[2]);
    }
    
    @Override
    public String toString()
    {
        return theme + ";" + text + ";" + complexity;
    }
}
