import com.mpatric.mp3agic.*;
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
                        st = c.prepareStatement("insert into music values(?, ?, ?, ?, ?, ?, ?, ?)");
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

                            Mp3File mp3file = new Mp3File(f);
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
                            st.setString(7, path);
                            st.setString(8, finalDirectory.toPath().toString());
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

    ResultSet loadLibraries()
    {
        ResultSet savedLibraries;
        Statement st;
        try
        {
            st = c.createStatement();
            savedLibraries = st.executeQuery("select * from libraries");

            if(savedLibraries.getString(1) == null)
            {
                return null;
            }

            return savedLibraries;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

    }

    ResultSet getMusicList(String filepath)
    {
        try
        {
            PreparedStatement st = c.prepareStatement("select title,album, filepath from music where folderpath=?");
            st.setString(1, filepath);

            return st.executeQuery();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

    }
}
