import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

public class ApplicationController implements Initializable
{
    @FXML
    Label testlabel;
    @FXML
    Button PlayButton;

    int a;
    Media media;
    MediaPlayer Mp;

    String s = "C:\\Users\\joela\\Music\\test\\";
    File cat;

    ArrayList<File> fileList;
    File[] f;
    @Override
    public void initialize(URL arg0, ResourceBundle arg1)
    {
        cat = new File(s);
        fileList = new ArrayList<File>();
        f = cat.listFiles();

        if(f != null)
        {
            fileList.addAll(Arrays.asList(f));
        }
        media = new Media(fileList.get(a).toURI().toString());
        Mp = new MediaPlayer(media);

    }



    @FXML
    void playButtonPress()
    {
        testlabel.setText(media.getMetadata().get("title").toString());
        testlabel.setVisible(!testlabel.isVisible());


        if(Mp.getStatus() == MediaPlayer.Status.PLAYING)
        {
            Mp.pause();
            PlayButton.setText("Play");
        }
        else
        {

            PlayButton.setText("Pause");
            //Mp.setStartTime(Duration.seconds(20.00));
            Mp.play();
           // System.out.println(cat.toURI().toString());
        }



    }
    @FXML
    void previousButtonPress()
    {
        Mp.stop();
        testlabel.setVisible(false);
        PlayButton.setText("Play");
        media = new Media(fileList.get(--a).toURI().toString());
        Mp = new MediaPlayer(media);

    }
    @FXML
    void nextButtonPress()
    {
        Mp.stop();
        testlabel.setVisible(false);
        PlayButton.setText("Play");
        media = new Media(fileList.get(++a).toURI().toString());
        Mp = new MediaPlayer(media);
    }

}
