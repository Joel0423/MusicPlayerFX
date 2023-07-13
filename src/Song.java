public class Song
{
    private String title;
    private String album;
    private String filepath;


    public Song(String title, String album, String filepath)
    {
        this.title = title;
        this.album = album;
        this.filepath = filepath;
    }

    public String getTitle()
    {
        return title;
    }
    public String getAlbum()
    {
        return album;
    }
    public String getFilepath()
    {
        return filepath;
    }

}
