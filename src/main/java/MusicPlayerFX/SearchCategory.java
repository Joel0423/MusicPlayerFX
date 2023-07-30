/*
 * Repository link- https://github.com/Joel0423/upskill_campus/
 * File link-       https://github.com/Joel0423/upskill_campus/blob/master/src/main/java/MusicPlayerFX/SearchCategory.java
 */

package MusicPlayerFX;

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
