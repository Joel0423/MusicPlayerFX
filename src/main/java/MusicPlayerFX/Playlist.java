/*
 * Repository link- https://github.com/Joel0423/upskill_campus/
 * File link-       https://github.com/Joel0423/upskill_campus/blob/master/src/main/java/MusicPlayerFX/Playlist.java
 */

package MusicPlayerFX;

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
