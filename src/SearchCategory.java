public class SearchCategory
{
    private String text;
    private String category;

    public SearchCategory(String text, String category)
    {
        this.text = text;
        this.category = category;
    }

    public String getText()
    {
        return text;
    }

    public String getCategory()
    {
        return category;
    }

    @Override
    public String toString()
    {
        return this.getText();
    }

}
