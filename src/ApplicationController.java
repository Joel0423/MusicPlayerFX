import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.sql.SQLException;
import java.util.Random;

public class ApplicationController
{

    @FXML
    Button PlayButton;
    @FXML
    ComboBox<Libraries> libraryBox;
    @FXML
    TableView<Song> musicTable;
    @FXML
    TableColumn<Song, String> titleColumn;
    @FXML
    TableColumn<Song, String> albumColumn;
    @FXML
    TableColumn<Song, String> filepathColumn;
    @FXML
    TableColumn<Song, String> artistColumn;
    @FXML
    TableColumn<Song, String> orderColumn;
    @FXML
    Slider progressbar;
    @FXML
    CheckBox shuffle;
    @FXML
    Slider volumeSlider;
    @FXML
    Label titleLabel;
    @FXML
    Label artistLabel;
    @FXML
    Label albumLabel;
    @FXML
    Label genreLabel;
    @FXML
    Label yearLabel;
    @FXML
    TextArea lyricsBox;
    @FXML
    Label customTagLabel;
    @FXML
    Button createCustomTagButton;
    @FXML
    Button stopPlaybackButton;
    @FXML
    ComboBox<Playlist> playlistBox;
    @FXML
    ContextMenu musicContextMenu;
    @FXML
    Button orderButton;
    @FXML
    TextField orderText;
    @FXML
    CheckBox autoplayCheckBox;
    @FXML
    Button searchButton;
    @FXML
    ComboBox<SearchCategory> searchBox;
    @FXML
    TextField searchText;

    int lastIndex;
    Media media;
    MediaPlayer mediaPlayer;
    Connector obj = new Connector();
    Timeline tm;
    MenuItem removeFromPlaylist;
    ObservableList<Song> playlistSongs;

    @FXML
    void importMusic()
    {
        obj.importMedia();

    }

    @FXML
    public void initialize()
    {
        refreshLibraries();
        refreshPlaylists();

        titleColumn.setCellValueFactory(new PropertyValueFactory<Song, String>("Title"));
        albumColumn.setCellValueFactory(new PropertyValueFactory<Song, String>("Album"));
        artistColumn.setCellValueFactory(new PropertyValueFactory<Song, String>("Artist"));
        filepathColumn.setCellValueFactory(new PropertyValueFactory<Song, String>("Filepath"));
        orderColumn.setCellValueFactory(new PropertyValueFactory<Song, String>("Order"));

        musicTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) ->
        {
            //checking old selection so that code only runs when selected song is changed in the same list
            if(newSel != null && oldSel!=null)
            {
                stopMusic();
                loadMedia();

                if(autoplayCheckBox.isSelected())
                {
                    playMusic();
                }
            }
        });

        tm = new Timeline(
                new KeyFrame(Duration.seconds(1.0),e ->{
                    progressbar.increment();
                })
        );
        tm.setCycleCount(Timeline.INDEFINITE);

        musicTable.setPlaceholder(new Label("Select a library or playlist"));

        removeFromPlaylist = new MenuItem("Remove From Playlist");
        removeFromPlaylist.setOnAction(e -> {
            removeFromPlaylist();
        });

        searchBox.getItems().addAll(
                new SearchCategory("By Title","title"),
                new SearchCategory("By Artist","artist"),
                new SearchCategory("By Album","album"),
                new SearchCategory("By Genre","genre"),
                new SearchCategory("By Release Year","year"),
                new SearchCategory("By Custom Tag","tags")
        );

        progressbar.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double durationdub)
            {
                int sec =(int) Math.floor(durationdub);

                int min = sec/60;
                int remsec = (sec%60);

                return String.format("%d:%02d",min,remsec);
            }

            @Override
            public Double fromString(String s)
            {
                return null;
            }

        });

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
            else if(mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY)
            {
                playMusic();
                tm.play();

            }
            else
            {
                if(musicTable.getSelectionModel().getSelectedItem() == null)
                {
                    musicTable.getSelectionModel().select(0);
                }
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
        }

    }

    @FXML
    void libraryComboBoxItemClicked() //update music tableview
    {
        if(!libraryBox.getSelectionModel().isEmpty())
        {
            musicTable.setPrefWidth(500.00);
            orderText.clear();
            orderButton.setVisible(false);
            orderText.setVisible(false);
            orderColumn.setVisible(false);
            if(musicContextMenu.getItems().size() == 2)
            {
                musicContextMenu.getItems().remove(1);
            }
            playlistBox.getSelectionModel().clearSelection();
            musicTable.setItems(obj.getMusicList(libraryBox.getSelectionModel().getSelectedItem().getFolderpath()));
            setMusicPlayer();
        }

    }

    @FXML
    void playlistComboBoxItemClicked() //update music tableview
    {
        if(!playlistBox.getSelectionModel().isEmpty())
        {
            musicTable.setPrefWidth(590.00);
            orderText.clear();
            orderButton.setVisible(true);
            orderText.setVisible(true);
            orderColumn.setVisible(true);
            libraryBox.getSelectionModel().clearSelection();

            playlistSongs = obj.getMusicList(playlistBox.getSelectionModel().getSelectedItem().getPlaylistID());

            if(musicContextMenu.getItems().size() ==1)
            {
                musicContextMenu.getItems().add(removeFromPlaylist);
            }

            if(!playlistSongs.isEmpty())
            {
                musicTable.setItems(playlistSongs);
                setMusicPlayer();
            }
            else
            {
                resetPlayer();
                musicTable.setPlaceholder(new Label("No music has been entered in this playlist \nRight click on a song in library mode to add"));
            }

        }
    }

    @FXML
    void changeOrder()
    {
        String order = orderText.getText();
        if(!order.isBlank())
        {
            try
            {
                int ordernum = Integer.parseInt(order);
                int id = playlistBox.getSelectionModel().getSelectedItem().getPlaylistID();
                int selindex = musicTable.getSelectionModel().getSelectedIndex();
                obj.changeOrder(id,musicTable.getSelectionModel().getSelectedItem().getFilepath(),ordernum);

                musicTable.setItems(obj.getMusicList(id));
                setMusicPlayer();
            }
            catch(NumberFormatException e)
            {
                Alert al = new Alert(Alert.AlertType.WARNING);
                al.setHeaderText("Cannot change order");
                al.setContentText("Enter only numbers");
                al.show();
                orderText.clear();
            }

        }
    }

    void setMusicPlayer()
    {
        if(mediaPlayer != null)
        {
            stopMusic();
        }

        musicTable.getSelectionModel().select(0);
        lastIndex = musicTable.getItems().size()-1;
        loadMedia();

    }

    @FXML
    void resetPlayer()
    {
        stopMusic();
        mediaPlayer = null;
        media = null;
        musicTable.getItems().clear();
        musicTable.setPlaceholder(new Label("Select a library or playlist"));
        libraryBox.getSelectionModel().clearSelection();
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                playlistBox.getSelectionModel().clearSelection();
            }
        });
        tm.stop();
        progressbar.setDisable(true);
        volumeSlider.setDisable(true);

        titleLabel.setText("Title: ");
        artistLabel.setText("Artist: ");
        albumLabel.setText("Album: ");
        genreLabel.setText("Genre: ");
        yearLabel.setText("Year: ");
        lyricsBox.setText("");
        customTagLabel.setText("Custom Tags: ");
        musicTable.setPrefWidth(500.00);
        orderText.clear();
        orderText.setVisible(false);
        orderButton.setVisible(false);
    }

    void stopMusic()
    {
        if(mediaPlayer!=null)
        {
            mediaPlayer.stop();
            PlayButton.setText("Play");
            tm.stop();
        }
    }

    void playMusic()
    {
        if(mediaPlayer != null)
        {
            mediaPlayer.setVolume(volumeSlider.getValue()/100);
        }
        mediaPlayer.play();
        PlayButton.setText("Pause");

        progressbar.setDisable(false);
        volumeSlider.setDisable(false);

        tm.play();

    }

    void loadMedia()
    {
        String filepath = musicTable.getSelectionModel().getSelectedItem().getFilepath();
        media = new Media(new File(filepath).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(new Runnable()
        {
            @Override
            public void run()
            {
                setProgressbar();

                Song metadata = obj.getSongMetadata(filepath);
                titleLabel.setText("Title: "+metadata.getTitle());
                artistLabel.setText("Artist: "+metadata.getArtist());
                albumLabel.setText("Album: "+metadata.getAlbum());
                genreLabel.setText("Genre: "+metadata.getGenre());
                yearLabel.setText("Year: "+metadata.getYear());
                lyricsBox.setText(metadata.getLyrics());

                String tags = obj.getCustomTags(filepath);
                customTagLabel.setText("Custom Tags: "+tags);
            }
        });

        mediaPlayer.setOnEndOfMedia(new Runnable()
        {
            @Override
            public void run()
            {
                if(autoplayCheckBox.isSelected())
                {
                    if (shuffle.isSelected())
                    {
                        selectRandomSong();
                    }
                    else
                    {
                        nextButtonPress();
                    }
                }
                else
                {
                    stopMusic();
                    loadMedia();
                }
            }
        });
        orderText.clear();

    }

    void selectRandomSong()
    {
        Random rand = new Random();
        int x = rand.nextInt(lastIndex+1);
        musicTable.getSelectionModel().select(x);
        musicTable.scrollTo(x-2);
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
            libraryBox.setItems(obj.loadLibraries());
        }
    }

    @FXML
    void volumeChanged()
    {
        if(mediaPlayer != null)
        {
            mediaPlayer.setVolume(volumeSlider.getValue()/100);
        }
    }

    @FXML
    void createCustomTag()
    {
        if(mediaPlayer != null)
        {
            TextInputDialog ti = new TextInputDialog();
            ti.setContentText("Enter the tag name for "+musicTable.getSelectionModel().getSelectedItem().getTitle());
            ti.setHeaderText("Create custom tag");
            ti.show();

            ti.setOnCloseRequest(e -> {
                String s = ti.getResult();

                if(s!=null && !s.isBlank())
                {
                    String filep = musicTable.getSelectionModel().getSelectedItem().getFilepath();
                    obj.enterCustomTag(s, filep);
                    String tags = obj.getCustomTags(filep);
                    customTagLabel.setText("Custom Tags: "+tags);
                }
            });

        }
    }

    @FXML
    void createPlaylist()
    {
        TextInputDialog ti = new TextInputDialog();
        ti.setHeaderText("Create a new Playlist");
        ti.setContentText("Enter a name");
        ti.show();

        ti.setOnCloseRequest(e -> {
            String plname= ti.getResult();

            if(plname!=null && !plname.isBlank())
            {
                try
                {
                    obj.createPlaylist(plname);

                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setHeaderText("Playlist created");
                    a.setContentText("'"+plname+"' has been created");
                    a.show();
                    refreshPlaylists();
                }
                catch (SQLException ex)
                {
                    int code = ex.getErrorCode();
                    if(code == 19)
                    {
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setHeaderText("Could not create playlist");
                        a.setContentText("A playlist with the same name already exists");
                        a.show();
                    }
                }

            }
        });
    }

    void refreshPlaylists()
    {
        if(obj.getPlaylists() != null)
        {
            playlistBox.setItems(obj.getPlaylists());
        }
    }

    @FXML
    void removeFromPlaylist()
    {
        if(musicTable.getSelectionModel().getSelectedItem() != null)
        {
            String filep = musicTable.getSelectionModel().getSelectedItem().getFilepath();
            int id = playlistBox.getSelectionModel().getSelectedItem().getPlaylistID();

            obj.removeFromPlaylist(id, filep);

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText("Removed from playlist");
            a.setContentText("'" + musicTable.getSelectionModel().getSelectedItem().getTitle() + "' has been removed from playlist '" + playlistBox.getSelectionModel().getSelectedItem().getPlaylistName() + "'");
            a.show();

            //removing from observable list
            playlistSongs.remove(musicTable.getSelectionModel().getFocusedIndex());
            musicTable.setItems(playlistSongs);
            setMusicPlayer();
        }
    }

    @FXML
    void addtoPlaylist()
    {
        if(musicTable.getSelectionModel().getSelectedItem() != null)
        {
            String filep = musicTable.getSelectionModel().getSelectedItem().getFilepath();
            String name = musicTable.getSelectionModel().getSelectedItem().getTitle();
            ChoiceDialog<Playlist> cd = new ChoiceDialog<Playlist>();
            cd.getItems().addAll( obj.getPlaylists());
            cd.setHeaderText("Adding to playlist");
            cd.setContentText("Select the playlist to add '"+name+"' to");
            cd.show();

            cd.setOnCloseRequest(e -> {
                Playlist p = cd.getResult();
                if(p !=null)
                {
                    int id = p.getPlaylistID();
                    Boolean bool = obj.addtoPlaylist(filep,id);

                    if(bool == true)
                    {
                        Alert a = new Alert(Alert.AlertType.INFORMATION);
                        a.setHeaderText("Added to playlist");
                        a.setContentText("added '"+musicTable.getSelectionModel().getSelectedItem().getTitle()+"' to the playlist '"+p.getPlaylistName()+"'");
                        a.show();
                    }
                    else
                    {
                        Alert a = new Alert(Alert.AlertType.INFORMATION);
                        a.setHeaderText("Could not add to playlist");
                        a.setContentText("'"+musicTable.getSelectionModel().getSelectedItem().getTitle()+"' is already in the playlist '"+p.getPlaylistName()+"'");
                        a.show();
                    }

                }
            });

        }

    }

    @FXML
    void deletePlaylist()
    {
        ChoiceDialog<Playlist> cd = new ChoiceDialog<>();
        cd.setHeaderText("Delete Playlist");
        cd.setContentText("Select a playlist to delete");
        cd.getItems().addAll(obj.getPlaylists());
        cd.show();

        cd.setOnCloseRequest(e -> {
            Playlist p = cd.getResult();
            if (p != null)
            {
                int id = p.getPlaylistID();
                obj.deletePlaylist(id);

                Playlist sel = playlistBox.getSelectionModel().getSelectedItem();
                if (sel !=null &&  sel.getPlaylistID() == id)
                {
                    resetPlayer();
                }
                Alert al = new Alert(Alert.AlertType.INFORMATION);
                al.setHeaderText("Playlist deleted");
                al.setContentText("Playlist '" + p.getPlaylistName() + "' has been deleted");
                playlistBox.setItems(obj.getPlaylists());
                al.show();
            }
        });
    }

    @FXML
    void searchMusic()
    {
        if(!searchBox.getSelectionModel().isEmpty() && !searchText.getText().isBlank())
        {
            String category = searchBox.getSelectionModel().getSelectedItem().getCategory();
            String searchterm = searchText.getText();

            ObservableList<Song> searchList = obj.getMusicList(category,searchterm);
            if(!searchList.isEmpty())
            {
                resetPlayer();
                musicTable.setItems(searchList);
                setMusicPlayer();
            }
            else
            {
                Alert al = new Alert(Alert.AlertType.INFORMATION);
                al.setHeaderText("No results");
                al.setContentText("No music found with "+category+": '"+searchText.getText()+"' ");
                al.show();
            }
        }
    }

}
