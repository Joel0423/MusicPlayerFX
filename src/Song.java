public class Song
{
    private String title;
    private String album;
    private String artist;
    private String filepath;
    private String genre;
    private String year;
    private String lyrics;
    private String order;

    public Song(String title, String artist, String album, String filepath)
    {
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.filepath = filepath;
    }

    public Song(String title, String artist, String album, String filepath, String order)
    {
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.filepath = filepath;
        this.order = order;
    }

    public void setOrder(String order)
    {
        this.order = order;
    }

    public Song(String title, String artist, String album, String genre, String year, String lyrics)
    {
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.genre = genre;
        this.year = year;
        this.lyrics = lyrics;
    }

    public String getTitle()
    {
        return title;
    }
    public String getAlbum()
    {
        return album;
    }
    public String getArtist() {return artist; }
    public String getFilepath()
    {
        return filepath;
    }

    public String getOrder() { return order; }

    public String getGenre()
    {
        return genre;
    }

    public String getYear()
    {
        return year;
    }

    public String getLyrics()
    {
        return lyrics;
    }

}
