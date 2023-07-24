package MusicPlayerFX;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.io.FilenameFilter;
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
    @FXML
    ImageView albumArt;

    int lastIndex;
    Media media;
    MediaPlayer mediaPlayer;
    Connector obj = new Connector();
    Timeline tm;
    MenuItem removeFromPlaylist;
    ObservableList<Song> playlistSongs;

    ChangeListener musicTableSelectedItemListener = new ChangeListener()
    {
        @Override
        public void changed(ObservableValue observableValue, Object oldSel, Object newSel)
        {

            if(newSel != null )
            {
                stopMusic();

                Song selsong = musicTable.getSelectionModel().getSelectedItem();
                int index = musicTable.getSelectionModel().getSelectedIndex();
                String filepath = selsong.getFilepath();
                File song = new File(filepath);
                if(song.exists())
                {
                    loadMedia();

                    if(autoplayCheckBox.isSelected())
                    {
                        playMusic();
                    }
                }
                else
                {
                    stopMusic();
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setHeaderText("Song cannot be loaded");
                    a.setContentText("The song '"+selsong.getTitle()+"' could not be found on the disk \n It may have been moved or deleted \n Do you want to remove it from the database?");
                    a.getButtonTypes().remove(ButtonType.OK);
                    a.getButtonTypes().add(ButtonType.YES);
                    a.getButtonTypes().add(ButtonType.NO);
                    a.show();
                    a.setOnCloseRequest(e -> {
                        if(a.getResult().getButtonData() == ButtonType.YES.getButtonData())
                        {
                            Song unavailableSong = musicTable.getSelectionModel().getSelectedItem();
                            obj.removeFromMusicList(unavailableSong.getFilepath());
                            musicTable.getItems().remove(unavailableSong);


                        }
                        musicTable.getSelectionModel().clearSelection();
                        media = null;
                        mediaPlayer = null;

                    });

                    tm.stop();
                    progressbar.setValue(0.00);
                    progressbar.setMax(0.00);

                }

            }
        }
    };

    @FXML
    void importMusic()
    {
        File directory = new File(System.getProperty("user.home"));

        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(directory);
        directory = dc.showDialog(new Stage());
        if(directory != null)
        {
            File[] fileArray = directory.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.toLowerCase().endsWith(".mp3");
                }
            });

            //mp3 files exist in the selected folder
            if(fileArray!=null && fileArray.length != 0)
            {
                obj.importMedia(directory, fileArray);

                Alert al = new Alert(Alert.AlertType.INFORMATION);
                al.setHeaderText("Library Importing");
                al.setContentText("Your music library is being imported \n if it contains a large number of files you may need to wait for a few minutes \n for all the files to be imported");
                al.show();

                al.setOnCloseRequest(e -> {
                    refreshLibraries();
                });
            }
            else
            {
                System.out.println("No music here");
            }

        }
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

        musicTable.getSelectionModel().selectedItemProperty().addListener(musicTableSelectedItemListener);

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
                System.out.println("here?");
                tm.play();

            }
            else
            {
                if(musicTable.getSelectionModel().getSelectedItem() == null)
                {
                    musicTable.getSelectionModel().select(0);
                }

                File song = new File(musicTable.getSelectionModel().getSelectedItem().getFilepath());
                if(song.exists())
                {
                    loadMedia();
                    playMusic();
                }
            }
        }

    }
    @FXML
    void previousButtonPress()
    {
        if(!musicTable.getSelectionModel().isEmpty())
        {
            if (!musicTable.getSelectionModel().isSelected(0))
            {
                stopMusic();
                musicTable.getSelectionModel().selectPrevious();
            }
        }

    }
    @FXML
    void nextButtonPress()
    {
        if(!musicTable.getSelectionModel().isEmpty())
        {
            if (shuffle.isSelected())
            {
                selectRandomSong();
            }
            else if (!musicTable.getSelectionModel().isSelected(lastIndex))
            {
                stopMusic();
                musicTable.getSelectionModel().selectNext();
            }
        }

    }

    @FXML
    void libraryComboBoxItemClicked() //update music tableview
    {
        clearMetadata();
        stopMusic();
        media = null;
        mediaPlayer = null;
        tm.stop();
        progressbar.setValue(0.00);
        progressbar.setMax(0.00);

        Libraries selLib = libraryBox.getSelectionModel().getSelectedItem();
        if(!libraryBox.getSelectionModel().isEmpty())
        {
            if(new File(selLib.getFolderpath()).exists())
            {
                musicTable.setPrefWidth(500.00);
                orderText.clear();
                orderButton.setVisible(false);
                orderText.setVisible(false);
                orderColumn.setVisible(false);
                if (musicContextMenu.getItems().size() == 2)
                {
                    musicContextMenu.getItems().remove(1);
                }
                playlistBox.getSelectionModel().clearSelection();
                musicTable.setItems(obj.getMusicList(libraryBox.getSelectionModel().getSelectedItem().getFolderpath()));
                lastIndex = musicTable.getItems().size() - 1;
            }
            else
            {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setHeaderText("Library not available");
                a.setContentText("The library '"+selLib.getFolder()+"' could not be found on the disk \n It may have been moved or deleted \n Do you want to remove it from the database?");
                a.getButtonTypes().remove(ButtonType.OK);
                a.getButtonTypes().add(ButtonType.YES);
                a.getButtonTypes().add(ButtonType.NO);
                a.show();
                a.setOnCloseRequest(e -> {
                    if(a.getResult().getButtonData() == ButtonType.YES.getButtonData())
                    {
                        obj.removeFromLibraries(selLib.getFolderpath());
                        libraryBox.getItems().remove(selLib);
                    }
                    resetPlayer();
                });
            }
        }

    }

    @FXML
    void playlistComboBoxItemClicked() //update music tableview
    {
        clearMetadata();
        stopMusic();
        media = null;
        mediaPlayer = null;
        tm.stop();
        progressbar.setValue(0.00);
        progressbar.setMax(0.00);

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
                lastIndex = musicTable.getItems().size()-1;
                //setMusicPlayer();
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
        loadMedia();

    }

    void setMusicPlayer(int index)
    {
        if(mediaPlayer != null)
        {
            stopMusic();
        }

        musicTable.getSelectionModel().select(index);
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
        progressbar.setValue(0.00);
        progressbar.setMax(0.00);
        volumeSlider.setDisable(true);

        clearMetadata();
        musicTable.setPrefWidth(500.00);
        orderText.clear();
        orderText.setVisible(false);
        orderButton.setVisible(false);
    }

    void clearMetadata()
    {
        titleLabel.setText("Title: ");
        artistLabel.setText("Artist: ");
        albumLabel.setText("Album: ");
        genreLabel.setText("Genre: ");
        yearLabel.setText("Year: ");
        lyricsBox.setText("");
        customTagLabel.setText("Custom Tags: ");

        if(albumArt.getImage()!=null)
            albumArt.setImage(null);
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
            mediaPlayer.play();
            PlayButton.setText("Pause");

            progressbar.setDisable(false);
            volumeSlider.setDisable(false);

            tm.play();
        }


    }

    void loadMedia()
    {
        Song selsong = musicTable.getSelectionModel().getSelectedItem();
        String filepath = selsong.getFilepath();
        File song = new File(filepath);

            media = new Media(song.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnReady(new Runnable()
            {
                @Override
                public void run()
                {
                    if(media != null)
                    {
                        setProgressbar();

                        Song metadata = obj.getSongMetadata(filepath);
                        titleLabel.setText("Title: " + metadata.getTitle());
                        artistLabel.setText("Artist: " + metadata.getArtist());
                        albumLabel.setText("Album: " + metadata.getAlbum());
                        genreLabel.setText("Genre: " + metadata.getGenre());
                        yearLabel.setText("Year: " + metadata.getYear());
                        lyricsBox.setText(metadata.getLyrics());
                        Image coverart = (Image) media.getMetadata().get("image");

                        if(coverart!=null)
                            albumArt.setImage(coverart);

                        String tags = obj.getCustomTags(filepath);
                        customTagLabel.setText("Custom Tags: " + tags);
                    }
                }
            });

            mediaPlayer.setOnEndOfMedia(new Runnable()
            {
                @Override
                public void run()
                {
                    if (autoplayCheckBox.isSelected())
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
        clearMetadata();
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
            int index = musicTable.getSelectionModel().getFocusedIndex();
            playlistSongs.remove(index);

            if(!musicTable.getItems().isEmpty())
            {
                musicTable.setItems(playlistSongs);
                stopMusic();
                if(autoplayCheckBox.isSelected())
                {
                    if(index == lastIndex)
                    {
                        setMusicPlayer(index - 1);
                    }
                    else if(index == 0)
                    {
                        setMusicPlayer();
                    }
                    else {
                        setMusicPlayer(index);
                    }
                }
                else
                {
                    musicTable.getSelectionModel().clearSelection();
                    media = null;
                    mediaPlayer = null;
                }


            }
            else
            {
                resetPlayer();
                musicTable.setPlaceholder(new Label("No music has been entered in this playlist \nRight click on a song in library mode to add"));
            }

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
            if(obj.getPlaylists() != null)
            {
                cd.getItems().addAll(obj.getPlaylists());
                cd.setHeaderText("Adding to playlist");
                cd.setContentText("Select the playlist to add '" + name + "' to");
                cd.show();

                cd.setOnCloseRequest(e -> {
                    Playlist p = cd.getResult();
                    if (p != null)
                    {
                        int id = p.getPlaylistID();
                        Boolean bool = obj.addtoPlaylist(filep, id);

                        if (bool == true)
                        {
                            Alert a = new Alert(Alert.AlertType.INFORMATION);
                            a.setHeaderText("Added to playlist");
                            a.setContentText("added '" + musicTable.getSelectionModel().getSelectedItem().getTitle() + "' to the playlist '" + p.getPlaylistName() + "'");
                            a.show();
                        }
                        else
                        {
                            Alert a = new Alert(Alert.AlertType.INFORMATION);
                            a.setHeaderText("Could not add to playlist");
                            a.setContentText("'" + musicTable.getSelectionModel().getSelectedItem().getTitle() + "' is already in the playlist '" + p.getPlaylistName() + "'");
                            a.show();
                        }

                    }
                });
            }

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
