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
        int tmp = Integer.parseInt(elems[0].trim());
        this.theme = elems[0].trim();
        this.text = elems[1].trim();
        this.complexity = Integer.parseInt(elems[2].trim());
    }
    
    @Override
    public String toString()
    {
        return theme + ";" + text + ";" + complexity;
    }
}
