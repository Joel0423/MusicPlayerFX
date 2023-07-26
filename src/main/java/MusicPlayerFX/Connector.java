package MusicPlayerFX;

import com.mpatric.mp3agic.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Connector
{
    Connection c = null;
    private static final Logger logger = Logger.getLogger("connector.log");

    public Connector()
    {
        logger.setLevel(Level.ALL);
        FileHandler fh;
        try
        {
            fh = new FileHandler(System.getProperty("user.home")+ File.separator+"MusicPlayerFX"+ File.separator +"MusicPlayerFX.log");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setUseParentHandlers(false);


        try
        {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+System.getProperty("user.home")+ File.separator+"MusicPlayerFX"+ File.separator + "MusicPlayerFX.db");
        }
        catch (ClassNotFoundException e)
        {
            logger.severe("SQLite JDBC driver is not found");
            throw new RuntimeException(e);

        }
        catch (SQLException e)
        {
            logger.severe("Could not connect to database");
            throw new RuntimeException(e);
        }

    }

    void importMedia(File directory, File[] fileArray)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                PreparedStatement st = null;
                try
                {
                    st = c.prepareStatement("insert or ignore into music values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    PreparedStatement st2 = c.prepareStatement("insert or ignore into libraries values(?, ?)");
                    st2.setString(1, directory.getName());
                    st2.setString(2, directory.toPath().toString());

                    st2.executeUpdate();
                    st2.close();
                    logger.info("Start: "+LocalDateTime.now().toString());

                    for (File f : fileArray)
                    {
                        String path = f.toPath().toString();
                        String title = "";
                        String artist = "";
                        String album = "";
                        String genre = "";
                        String year = "";
                        String lyrics = "";
                        String duration = "";

                        Mp3File mp3file = null;
                        try
                        {
                            mp3file = new Mp3File(f);
                        }
                        catch (InvalidDataException e)
                        {
                            logger.log(Level.SEVERE,"MP3 frame data problem",e);
                            continue;
                        }
                        catch (UnsupportedTagException e)
                        {
                            logger.log(Level.SEVERE,"Tag is not supported",e);
                            continue;
                        }
                        catch (IOException e)
                        {
                            logger.log(Level.SEVERE,"IO exception",e);
                            continue;
                        }
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
                        st.setString(9, directory.toPath().toString());
                        try
                        {
                            st.executeUpdate();
                        }
                        catch(SQLException e)
                        {
                            logger.log(Level.SEVERE,"Error while importing a music file: "+f.toString(),e);
                        }
                        finally
                        {
                            st.clearParameters();
                        }
                    }
                    logger.info("End: "+LocalDateTime.now().toString());

                }
                catch(SQLException e)
                {
                    logger.log(Level.SEVERE,"Error while connecting to database to import music files",e);
                }
                finally
                {
                    try
                    {
                        st.close();
                    }
                    catch (SQLException e)
                    {
                        logger.log(Level.INFO,"statement could not be closed",e);
                    }
                }
            }
        }).start();
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
            logger.log(Level.SEVERE,"Libraries could not be loaded",e);
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
                logger.log(Level.INFO,"statement could not be closed",e);
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
            logger.log(Level.SEVERE,"Music files could not be retrieved",e);
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
                logger.log(Level.INFO,"statement or result set could not be closed",e);
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
            st = c.prepareStatement("select title,artist, album, music.filepath,[order] from music, playlists, [playlist-songs] where playlists.playlistID=? and [playlist-songs].playlistID = playlists.playlistID and [playlist-songs].filepath = music.filepath order by [order];");
            st.setInt(1, playlistID);

            musicResultSet = st.executeQuery();

            while(musicResultSet.next())
            {
                musicList.add( new Song(musicResultSet.getString(1),musicResultSet.getString(2), musicResultSet.getString(3), musicResultSet.getString(4), Integer.toString(musicResultSet.getInt(5))));
            }

        }
        catch (SQLException e)
        {
            logger.log(Level.SEVERE,"Music files could not be retrieved",e);
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
                logger.log(Level.INFO,"statement or result set could not be closed",e);
            }

        }
        return  musicList;
    }

    ObservableList<Song> getMusicList(String category,String text)
    {

        ResultSet musicResultSet = null;
        PreparedStatement st = null;
        ObservableList<Song> musicList = FXCollections.observableArrayList();
        try
        {
            if(!category.equals("tags"))
            {
                st = c.prepareStatement(String.format( "select title,artist,album, filepath from music where %s like ?",category));
                st.setString(1, "%"+text+"%");
            }
            else
            {
                st = c.prepareStatement("select title,artist,album, music.filepath from music,[custom-tags] where music.filepath=[custom-tags].filepath and tags like ?");
                st.setString(1,"%"+text+"%");
            }

            musicResultSet = st.executeQuery();

            while(musicResultSet.next())
            {
                musicList.add( new Song(musicResultSet.getString(1),musicResultSet.getString(2), musicResultSet.getString(3), musicResultSet.getString(4)));
            }

        }
        catch (SQLException e)
        {
            logger.log(Level.SEVERE,"Music files could not be retrieved",e);
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
                logger.log(Level.INFO,"statement or result set could not be closed",e);
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
            logger.log(Level.SEVERE,"Metadata could not be retrieved",e);
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
                logger.log(Level.INFO,"statement or result set could not be closed",e);
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
            logger.log(Level.SEVERE,"Custom tag could not be entered",e);
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
                logger.log(Level.INFO,"statement or result set could not be closed",e);
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
            logger.log(Level.SEVERE,"Custom tags could not be retrieved",e);
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
                logger.log(Level.INFO,"statement or result set could not be closed",e);
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
            logger.log(Level.SEVERE,"Playlists could not be retrieved",e);
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
                logger.log(Level.INFO,"statement or result set could not be closed",e);
            }
        }
    }

    Boolean addtoPlaylist(String filepath, int playlistID)
    {

        PreparedStatement st = null;
        PreparedStatement st2 = null;
        PreparedStatement st3 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        ObservableList<Playlist> olist = FXCollections.observableArrayList();

        try
        {
            st = c.prepareStatement("insert into [playlist-songs] values(?, ?, ?)");
            st2 = c.prepareStatement("select count(*) from [playlist-songs] where PlaylistID=? and filepath=?");
            st3 = c.prepareStatement("select MAX([order]) from [playlist-songs] where PlaylistID=?");

            st.setInt(1, playlistID);
            st.setString(2, filepath);
            st2.setInt(1, playlistID);
            st2.setString(2, filepath);
            st3.setInt(1, playlistID);

            rs2 = st3.executeQuery();
            //getInt() returns 0 if sql value is null
            int count = rs2.getInt(1);
            st.setInt(3,count+1);
            rs2.close();
            st3.close();

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
            logger.log(Level.SEVERE,"Problem while adding music to playlist filepath:"+filepath+" ,playlistID:"+ playlistID,e);
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
                logger.log(Level.INFO,"statement could not be closed",e);
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
            logger.log(Level.SEVERE,"Problem while removing music from playlist filepath:"+filepath+" ,playlistID:"+ id,e);
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
                logger.log(Level.INFO,"statement could not be closed",e);
            }
        }
    }

    void removeFromMusicList(String filepath)
    {
        PreparedStatement st = null;

        try
        {
            st = c.prepareStatement("delete from music where filepath=?");
            st.setString(1, filepath);
            st.execute();
        }
        catch(SQLException e)
        {
            logger.log(Level.SEVERE,"Problem while removing music from music list, filepath:"+filepath,e);
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
                logger.log(Level.INFO,"statement could not be closed",e);
            }
        }
    }

    void removeFromLibraries(String folderpath)
    {
        PreparedStatement st = null;

        try
        {
            st = c.prepareStatement("delete from libraries where folderpath=?");
            st.setString(1, folderpath);
            st.execute();
        }
        catch(SQLException e)
        {
            logger.log(Level.SEVERE,"Problem while deleting library, folderpath:"+folderpath,e);
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
                logger.log(Level.INFO,"statement could not be closed",e);
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
            logger.log(Level.SEVERE,"Problem while deleting playlist, playlistID:"+id,e);
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
                logger.log(Level.INFO,"statement could not be closed",e);
            }
        }
    }

    void changeOrder(int playlistID,String filepath, int newOrder)
    {
        PreparedStatement st = null;
        try
        {
            st = c.prepareStatement("update [playlist-songs] set [order]=? where PlaylistID=? and filepath=?");
            st.setInt(1,newOrder);
            st.setInt(2,playlistID);
            st.setString(3,filepath);
            st.execute();
        }
        catch(SQLException e)
        {
            logger.log(Level.SEVERE,"Problem while changing song order in playlist, playlistID:"+playlistID+" , filepath:"+filepath,e);
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
                logger.log(Level.INFO,"statement could not be closed",e);
            }
        }
    }
}
