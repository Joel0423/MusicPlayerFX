import com.mpatric.mp3agic.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.*;

public class Connector
{
    Connection c = null;

    public Connector()
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\joela\\Desktop\\test.db");
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

    }

    void importMedia()
    {
        File directory = new File("C://Users//");

        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(directory);

        //can't do this in a new thread because it connects to GUI(FX app.) thread
        directory = dc.showDialog(new Stage());

        File file = new File(directory.toURI());
        File[] fileArray = file.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                if (name.toLowerCase().endsWith(".mp3"))
                {
                    return true;
                }

                else
                {
                    return false;

                }
            }
        });

        if(fileArray.length != 0)
        {
            //mp3 files exist in the selected folder
            File finalDirectory = directory;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PreparedStatement st = null;
                    try
                    {
                        st = c.prepareStatement("insert into music values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        PreparedStatement st2 = c.prepareStatement("insert into libraries values(?, ?)");
                        st2.setString(1, finalDirectory.getName());
                        st2.setString(2, finalDirectory.toPath().toString());
                        try
                        {
                            st2.executeUpdate();
                        }
                        catch(SQLException e)
                        {

                        }
                        st2.close();

                        for (File f : fileArray)
                        {
                            System.out.println(f.toURI().toString());
                            String path = f.toPath().toString();
                            String title = "";
                            String artist = "";
                            String album = "";
                            String genre = "";
                            String year = "";
                            String lyrics = "";
                            String duration = "";

                            Mp3File mp3file = new Mp3File(f);
                            duration = Long.toString(mp3file.getLengthInSeconds());

                            if (mp3file.hasId3v1Tag())
                            {

                                ID3v1 id3v1Tag = mp3file.getId3v1Tag();
                                title = id3v1Tag.getTitle();
                                artist = id3v1Tag.getArtist();
                                album = id3v1Tag.getAlbum();
                                genre = id3v1Tag.getGenreDescription();
                                year = id3v1Tag.getYear();
                            }
                            if (mp3file.hasId3v2Tag())
                            {
                                //overwriting with id3v2 if they exist
                                ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                                if(id3v2Tag.getTitle() != null)
                                    title = id3v2Tag.getTitle();
                                if(id3v2Tag.getArtist() != null)
                                    artist = id3v2Tag.getArtist();
                                if(id3v2Tag.getAlbum() != null)
                                    album = id3v2Tag.getAlbum();
                                if(id3v2Tag.getGenreDescription() != null)
                                    genre = id3v2Tag.getGenreDescription();
                                if(id3v2Tag.getYear() != null)
                                    year = id3v2Tag.getYear();
                                if(id3v2Tag.getLyrics() != null)
                                    lyrics = id3v2Tag.getLyrics();
                            }

                            st.setString(1, title);
                            st.setString(2, artist);
                            st.setString(3, album);
                            st.setString(4, genre);
                            st.setString(5, year);
                            st.setString(6, lyrics);
                            st.setString(7, duration);
                            st.setString(8, path);
                            st.setString(9, finalDirectory.toPath().toString());
                            try{
                                st.executeUpdate();
                            }
                            catch(SQLException e)
                            {

                            }
                            finally
                            {
                                st.clearParameters();
                            }
                        }

                    }
                    catch (InvalidDataException | IOException | UnsupportedTagException e)
                    {
                        throw new RuntimeException(e);
                    }
                    catch(SQLException e)
                    {

                    }
                    finally
                    {
                        try
                        {
                            st.close();
                            //c.close();
                        }
                        catch (SQLException e)
                        {
                            throw new RuntimeException(e);
                        }
                        System.out.println("Music imported!");


                    }
                }
            }).start();

        }
        else
        {
            System.out.println("No music here");
        }

    }

    ObservableList<Libraries> loadLibraries()
    {
        ResultSet libraryResultSet = null;
        ObservableList<Libraries> olist = FXCollections.observableArrayList();

        Statement st = null;
        try
        {
            st = c.createStatement();
            libraryResultSet = st.executeQuery("select * from libraries");

            if(libraryResultSet.getString(1) == null)
            {
                st.close();
                return null;
            }
            while (libraryResultSet.next())
            {
                olist.add(new Libraries(libraryResultSet.getString(1), libraryResultSet.getString(2)));
            }

            
            return olist;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                libraryResultSet.close();
                st.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
            
        }

    }

    ObservableList<Song> getMusicList(String filepath)
    {

        ResultSet musicResultSet = null;
        PreparedStatement st = null;
        ObservableList<Song> musicList = FXCollections.observableArrayList();
        try
        {
            st = c.prepareStatement("select title,artist,album, filepath from music where folderpath=?");
            st.setString(1, filepath);

            musicResultSet = st.executeQuery();

            while(musicResultSet.next())
            {
                musicList.add( new Song(musicResultSet.getString(1),musicResultSet.getString(2), musicResultSet.getString(3), musicResultSet.getString(4)));
            }

        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                musicResultSet.close();
                st.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }

        }
        return  musicList;
    }

    ObservableList<Song> getMusicList(int playlistID)
    {
        ResultSet musicResultSet = null;
        PreparedStatement st = null;
        ObservableList<Song> musicList = FXCollections.observableArrayList();

        try
        {
            st = c.prepareStatement("select title,artist, album, music.filepath from music, playlists, [playlist-songs] where playlists.playlistID=? and [playlist-songs].playlistID = playlists.playlistID and [playlist-songs].filepath = music.filepath;");
            st.setInt(1, playlistID);

            musicResultSet = st.executeQuery();

            while(musicResultSet.next())
            {
                musicList.add( new Song(musicResultSet.getString(1),musicResultSet.getString(2), musicResultSet.getString(3), musicResultSet.getString(4)));
            }

        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                musicResultSet.close();
                st.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }

        }
        return  musicList;
    }

    Song getSongMetadata(String filepath)
    {
        PreparedStatement st = null;
        ResultSet rs = null;
        try{

            st = c.prepareStatement("select title,artist,album,genre,year,lyrics from music where filepath = ?");
            st.setString(1, filepath);
            rs = st.executeQuery();

            return new Song(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6));
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                rs.close();
                st.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }

        }
    }

    void enterCustomTag(String tag, String filepath)
    {
        PreparedStatement st = null;
        try
        {
            st =  c.prepareStatement("insert into [custom-tags] values(?, ?)");
            st.setString(1, filepath);
            st.setString(2, tag);
            st.execute();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                st.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }

    }

    String getCustomTags(String filepath)
    {
        PreparedStatement st = null;
        ResultSet rs = null;
        String tags = "";
        try
        {
            st = c.prepareStatement("select tags from [custom-tags] where filepath=?");
            st.setString(1,filepath);

            rs = st.executeQuery();
            while(rs.next())
            {
                tags = tags+rs.getString(1)+",";
            }
            return tags;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                rs.close();
                st.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    void createPlaylist(String playlistname) throws SQLException
    {
        PreparedStatement st = null;
        st = c.prepareStatement("insert into playlists (playlist) values(?)");
        st.setString(1,playlistname);
        st.execute();
        st.close();
    }

    ObservableList<Playlist> getPlaylists()
    {
        PreparedStatement st = null;
        ResultSet rs = null;
        ObservableList<Playlist> olist = FXCollections.observableArrayList();

        try
        {
            st = c.prepareStatement("select * from playlists");
            rs = st.executeQuery();

            if(rs.getString(1)==null)
            {
                st.close();
                return null;
            }
            while(rs.next())
            {
                olist.add(new Playlist(rs.getInt(1),rs.getString(2)));
            }

            return olist;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {

            try
            {
                st.close();
                rs.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    Boolean addtoPlaylist(String filepath, int playlistID)
    {

        PreparedStatement st = null;
        PreparedStatement st2 = null;
        ResultSet rs = null;
        ObservableList<Playlist> olist = FXCollections.observableArrayList();

        try
        {
            st = c.prepareStatement("insert into [playlist-songs] (PlaylistID, filepath) values(?, ?)");
            st2 = c.prepareStatement("select count(*) from [playlist-songs] where PlaylistID=? and filepath=?");

            st.setInt(1, playlistID);
            st.setString(2, filepath);
            st2.setInt(1, playlistID);
            st2.setString(2, filepath);

            rs = st2.executeQuery();
            if(rs.getInt(1)==1)
            {
                rs.close();
                st2.close();
                return false;
            }
            else
            {
                rs.close();
                st2.close();
                st.execute();
                return true;
            }
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                st.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    void removeFromPlaylist(int id, String filepath)
    {
        PreparedStatement st = null;

        try
        {
            st = c.prepareStatement("delete from [playlist-songs] where PlaylistID=? and filepath=?");
            st.setInt(1, id);
            st.setString(2, filepath);
            st.execute();
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                st.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    void deletePlaylist(int id)
    {
        PreparedStatement st = null;
        try
        {
            st = c.prepareStatement("delete from playlists where PlaylistID=?");
            st.setInt(1,id);
            st.execute();
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                st.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
