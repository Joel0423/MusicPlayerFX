public class Playlist
{
    private String playlistName;

    public String getPlaylistName()
    {
        return playlistName;
    }

    public int getPlaylistID()
    {
        return playlistID;
    }

    private int playlistID;

    public Playlist(int playlistID, String playlistName)
    {
        this.playlistID = playlistID;
        this.playlistName = playlistName;
    }

    @Override
    public String toString()
    {
        return this.getPlaylistName();
    }

}
