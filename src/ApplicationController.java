import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class ApplicationController
{

    @FXML
    Button PlayButton;
    @FXML
    ComboBox libraryBox;
    @FXML
    TableView<Song> musicTable;
    @FXML
    TableColumn<Song, String> titleColumn;
    @FXML
    TableColumn<Song, String> albumColumn;
    @FXML
    TableColumn<Song, String> filepathColumn;
    @FXML
    Slider progressbar;
    @FXML
    CheckBox shuffle;

    int lastIndex;
    Media media;
    MediaPlayer mediaPlayer;
    Connector obj = new Connector();
    Timeline tm;

    @FXML
    void importMusic()
    {
        obj.importMedia();

    }

    @FXML
    public void initialize()
    {
        refreshLibraries();

        titleColumn.setCellValueFactory(new PropertyValueFactory<Song, String>("Title"));
        albumColumn.setCellValueFactory(new PropertyValueFactory<Song, String>("Album"));
        filepathColumn.setCellValueFactory(new PropertyValueFactory<Song, String>("Filepath"));

        musicTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) ->
        {
            //checking old selection so that code only runs when selected song is changed in the same list
            if(newSel != null && oldSel!=null)
            {
                int x = musicTable.getSelectionModel().getSelectedIndex();
                stopMusic();
                loadMedia();
                playMusic();
                musicTable.scrollTo(x-2);
            }
        });

        tm = new Timeline(
                new KeyFrame(Duration.seconds(1.0),e ->{
                    progressbar.increment();
                })
        );
        tm.setCycleCount(Timeline.INDEFINITE);

    }

    @FXML
    void playButtonPress()
    {
        if (mediaPlayer != null)
        {

            if(mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING)
            {
                mediaPlayer.pause();
                tm.stop();
                PlayButton.setText("Play");
            }
            else if(mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED)
            {
                playMusic();
                tm.play();

            }
            else
            {
                musicTable.getSelectionModel().select(0);
                loadMedia();
                playMusic();
            }
        }

    }
    @FXML
    void previousButtonPress()
    {
        if(mediaPlayer != null && !musicTable.getSelectionModel().isSelected(0))
        {
            stopMusic();
            musicTable.getSelectionModel().selectPrevious();

        }

    }
    @FXML
    void nextButtonPress()
    {
        if(mediaPlayer != null)
        {
            if(shuffle.isSelected())
            {
                selectRandomSong();
            }
            else if(!musicTable.getSelectionModel().isSelected(lastIndex))
            {
                stopMusic();
                musicTable.getSelectionModel().selectNext();
            }
            else
            {
                stopMusic();
            }
        }

    }

    @FXML
    void libraryComboBoxItemClicked() //update music tableview
    {
        Libraries l = (Libraries) libraryBox.getSelectionModel().getSelectedItem();

        ResultSet songResults = obj.getMusicList(l.getFolderpath());
        ObservableList<Song> musicList = FXCollections.observableArrayList();
        try
        {
            while(songResults.next())
            {
                musicList.add( new Song(songResults.getString(1),songResults.getString(2), songResults.getString(3)));
            }
        }
        catch(SQLException e)
        {

        }

        musicTable.setItems(musicList);

        setMusicPlayer();
    }

    void setMusicPlayer()
    {
        if(mediaPlayer != null)
        {
            mediaPlayer.stop();
        }

        musicTable.getSelectionModel().select(0);
        lastIndex = musicTable.getItems().size()-1;
        loadMedia();

    }

    void stopMusic()
    {
        mediaPlayer.stop();
        PlayButton.setText("Play");
        tm.stop();
    }

    void playMusic()
    {
        mediaPlayer.play();
        PlayButton.setText("Pause");

        progressbar.setDisable(false);

        tm.play();
    }

    void loadMedia()
    {

        media = new Media(new File(musicTable.getSelectionModel().getSelectedItem().getFilepath()).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnReady(new Runnable()
        {
            @Override
            public void run()
            {
                setProgressbar();
            }
        });

        mediaPlayer.setOnEndOfMedia(new Runnable()
        {
            @Override
            public void run()
            {

                if(shuffle.isSelected())
                {
                    selectRandomSong();
                }
                else
                    nextButtonPress();
            }
        });

    }

    void selectRandomSong()
    {
        musicTable.getSelectionModel().select(new Random().nextInt(lastIndex+1));
    }

    @FXML
    void progressbarClicked()
    {
        mediaPlayer.seek(Duration.seconds(progressbar.getValue()));
    }

    void setProgressbar()
    {
        progressbar.setValue(0.00);
        double d = media.getDuration().toSeconds();
        progressbar.setMax(d);
        progressbar.setMajorTickUnit(d/4);

    }

    @FXML
    void refreshLibraries()
    {
        if(obj.loadLibraries() != null)
        {
            ResultSet libraryList = obj.loadLibraries();
            ObservableList<Libraries> olist = FXCollections.observableArrayList();

            try
            {
                while (libraryList.next())
                {
                    olist.add(new Libraries(libraryList.getString(1), libraryList.getString(2)));
                }
            }
            catch(SQLException e)
            {

            }
            libraryBox.setItems(olist);

        }
    }
}
