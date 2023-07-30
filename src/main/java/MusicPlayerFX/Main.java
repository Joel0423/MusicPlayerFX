/*
 * Repository link- https://github.com/Joel0423/upskill_campus/
 * File link-       https://github.com/Joel0423/upskill_campus/blob/master/src/main/java/MusicPlayerFX/Main.java
 */

package MusicPlayerFX;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Main extends Application
{

    @Override
    public void start(Stage stage) throws IOException
    {
        File playerDirectory = new File(System.getProperty("user.home")+ File.separator+"MusicPlayerFX");
        if(playerDirectory.exists())
        {
            createDatabase();
            createLog();
        }
        else
        {
            playerDirectory.mkdir();
            createDatabase();
            createLog();
        }

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/player.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("MusicPlayerFX");
        stage.setScene(scene);
        stage.setResizable(false);
        Image icon = new Image("/PlayerIcon.png");
        stage.getIcons().add(icon);

        stage.show();

    }

    void createDatabase()
    {
        Connection conn = null;
        Statement st = null;
        try
        {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:"+System.getProperty("user.home")+ File.separator+"MusicPlayerFX"+ File.separator + "MusicPlayerFX.db");

            st = conn.createStatement();
            st.addBatch("CREATE TABLE IF NOT EXISTS \"music\" (\n" +
                    "\t\"title\"\tTEXT,\n" +
                    "\t\"artist\"\tTEXT,\n" +
                    "\t\"album\"\tTEXT,\n" +
                    "\t\"genre\"\tTEXT,\n" +
                    "\t\"year\"\tTEXT,\n" +
                    "\t\"lyrics\"\tTEXT,\n" +
                    "\t\"duration\"\tTEXT,\n" +
                    "\t\"filepath\"\tTEXT,\n" +
                    "\t\"folderpath\"\tTEXT,\n" +
                    "\tPRIMARY KEY(\"filepath\"),\n" +
                    "\tFOREIGN KEY(\"folderpath\") REFERENCES \"libraries\"(\"folderpath\") ON DELETE CASCADE\n" +
                    ");");

            st.addBatch("CREATE TABLE IF NOT EXISTS \"libraries\" (\n" +
                    "\t\"folder\"\tTEXT,\n" +
                    "\t\"folderpath\"\tTEXT,\n" +
                    "\tPRIMARY KEY(\"folderpath\")\n" +
                    ");");

            st.addBatch("CREATE TABLE IF NOT EXISTS \"playlists\" (\n" +
                    "\t\"playlistID\"\tINTEGER,\n" +
                    "\t\"playlist\"\tTEXT UNIQUE,\n" +
                    "\tPRIMARY KEY(\"playlistID\" AUTOINCREMENT)\n" +
                    ");");

            st.addBatch("CREATE TABLE IF NOT EXISTS \"playlist-songs\" (\n" +
                    "\t\"PlaylistID\"\tINTEGER,\n" +
                    "\t\"filepath\"\tTEXT,\n" +
                    "\t\"order\"\tINTEGER,\n" +
                    "\tFOREIGN KEY(\"PlaylistID\") REFERENCES \"playlists\"(\"playlistID\") ON DELETE CASCADE,\n" +
                    "\tFOREIGN KEY(\"filepath\") REFERENCES \"music\"(\"filepath\") ON DELETE CASCADE\n" +
                    ");");

            st.addBatch("CREATE TABLE IF NOT EXISTS \"custom-tags\" (\n" +
                    "\t\"filepath\"\tTEXT,\n" +
                    "\t\"tags\"\tTEXT,\n" +
                    "\tFOREIGN KEY(\"filepath\") REFERENCES \"music\"(\"filepath\") ON DELETE CASCADE\n" +
                    ");");
            st.executeBatch();
        }
        catch (SQLException | ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                st.close();
                conn.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    void createLog()
    {
        File log = new File(System.getProperty("user.home")+ File.separator+"MusicPlayerFX"+ File.separator +"MusicPlayerFX.log");
        try
        {
            log.createNewFile();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)
    {
        launch();
    }
}