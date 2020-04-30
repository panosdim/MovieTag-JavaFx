package movietag;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbSearch;
import info.movito.themoviedbapi.model.MovieDb;
import info.movito.themoviedbapi.model.config.TmdbConfiguration;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jcodec.containers.mp4.boxes.MetaValue;
import org.jcodec.movtool.MetadataEditor;
import org.jcodec.platform.Platform;
import tray.animations.AnimationType;
import tray.notification.NotificationType;
import tray.notification.TrayNotification;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Controller {
    public Button btnOpenFile;
    public TextField txtSearch;
    public Button btnSearch;
    public ListView<MovieDb> lstResults;
    public Button btnSave;
    public ImageView imgPoster;
    private TmdbApi tmdb;
    private MetadataEditor mediaMeta;
    private String baseUrl;
    private String posterSize;
    private File mkvPropEdit;
    private File movie;


    public void initialize() {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("/tmdb.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        tmdb = new TmdbApi(props.getProperty("KEY"));
        System.setProperty("http.proxyHost", props.getProperty("PROXYHOST"));
        System.setProperty("http.proxyPort", props.getProperty("PROXYPORT"));
        System.setProperty("https.proxyHost", props.getProperty("PROXYHOST"));
        System.setProperty("https.proxyPort", props.getProperty("PROXYPORT"));

        InputStream mkv = getClass().getResourceAsStream("/mkvpropedit.exe");
        try {
            mkvPropEdit = File.createTempFile("mkvpropedit", ".exe");
            mkvPropEdit.deleteOnExit();
            Files.copy(mkv, mkvPropEdit.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TmdbConfiguration config = tmdb.getConfiguration();
        List<String> posterSizes = config.getPosterSizes();
        posterSize = "w500";
        if (!posterSizes.contains("w500")) {
            posterSize = posterSizes.get(0);
        }
        baseUrl = config.getSecureBaseUrl();

        lstResults.setCellFactory(alignedListView -> new AlignedListViewCell());
    }

    public void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Movie File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Movie Files", "*.mp4", "*.mkv"));
        movie = fileChooser.showOpenDialog(btnOpenFile.getScene().getWindow());

        if (movie != null) {
            lstResults.getSelectionModel().clearSelection();
            lstResults.getItems().clear();
            txtSearch.setDisable(false);
            btnSearch.setDisable(false);
            btnSave.setDisable(true);
            String searchText = FilenameUtils.removeExtension(movie.getName());
            String movieRegex = "([ .\\w']+?)(\\W\\d{4}\\W?.*)";
            Pattern pattern = Pattern.compile(movieRegex);
            Matcher matcher = pattern.matcher(movie.getName());
            if (matcher.matches()) {
                String movieName = matcher.group(1).replaceAll("\\.", " ");
                movieName = Arrays.stream(movieName.split("\\s+"))
                        .map(t -> t.substring(0, 1).toUpperCase() + t.substring(1))
                        .collect(Collectors.joining(" "));
                searchText = movieName;
            }
            txtSearch.setText(searchText);
        }
    }

    private int stringToFourcc(String fourcc) {
        if (fourcc.length() != 4) {
            return 0;
        }
        byte[] bytes = Platform.getBytesForCharset(fourcc, Charset.defaultCharset());
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private byte[] downloadUrl(URL toDownload) {
        File tmpFile;
        try {
            tmpFile = File.createTempFile("cover", ".jpg");
            tmpFile.deleteOnExit();

            FileUtils.copyURLToFile(toDownload, tmpFile);
            return FileUtils.readFileToByteArray(tmpFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void searchTMDB() {
        lstResults.getSelectionModel().clearSelection();
        btnSave.setDisable(true);
        TmdbSearch search = tmdb.getSearch();

        MovieResultsPage searchResults = search.searchMovie(txtSearch.getText(), 0, null, false, 0);
        lstResults.setItems(FXCollections.observableArrayList(searchResults.getResults()));
        lstResults.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            btnSave.setDisable(false);
            if (newValue != null) {
                imgPoster.setImage(new Image(baseUrl + "w154" + newValue.getPosterPath(), 154, 235, false, false));
            } else {
                imgPoster.setImage(null);
            }
        });
    }

    public void writeTag() {
        btnSave.setDisable(true);

        String ext = FilenameUtils.getExtension(movie.getName());
        if (ext.equals("mp4")) {
            writeMp4Tags();
        } else {
            writeMkvTags();
        }
    }

    private void writeMp4Tags() {
        MovieDb selectedMovie = lstResults.getSelectionModel().getSelectedItem();

        try {
            mediaMeta = MetadataEditor.createFrom(movie);
            Map<Integer, MetaValue> iTunesMeta = mediaMeta.getItunesMeta();
            if (iTunesMeta != null) {
                URL imgPath;
                imgPath = new URL(baseUrl + posterSize + selectedMovie.getPosterPath());

                Task<byte[]> task = new Task<byte[]>() {
                    @Override
                    public byte[] call() {
                        return downloadUrl(imgPath);
                    }

                    @Override
                    protected void succeeded() {
                        super.succeeded();
                        iTunesMeta.clear();
                        iTunesMeta.put(stringToFourcc("covr"), MetaValue.createOther(MetaValue.TYPE_JPEG, getValue()));
                        iTunesMeta.put(stringToFourcc("name"), MetaValue.createString(selectedMovie.getOriginalTitle()));
                        iTunesMeta.put(stringToFourcc("ldes"), MetaValue.createString(selectedMovie.getOverview()));
                        try {
                            mediaMeta.save(true); // fast mode is on
                            showNotification("MP4 Movie Tag", "Tags saved successfully", NotificationType.SUCCESS);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        btnSave.setDisable(false);
                    }
                };

                new Thread(task).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMkvTags() {
        MovieDb selectedMovie = lstResults.getSelectionModel().getSelectedItem();
        try {
            Process p;
            File image = File.createTempFile("cover", ".jpg");
            image.deleteOnExit();
            FileUtils.copyURLToFile(new URL(baseUrl + posterSize + selectedMovie.getPosterPath()), image);

            p = new ProcessBuilder(mkvPropEdit.getAbsolutePath(), movie.getAbsolutePath(), "--delete-attachment", "mime-type:image/jpeg").start();
            p.waitFor();
            if (p.exitValue() == 2) {
                showNotification("MKV Movie Tag", "Error occurred when try to delete attachments.", NotificationType.ERROR);
            }

            p = new ProcessBuilder(mkvPropEdit.getAbsolutePath(), movie.getAbsolutePath(),
                    "--attachment-name", "cover.jpg",
                    "--attachment-mime-type", "image/jpeg",
                    "--add-attachment", image.getAbsolutePath()).start();
            p.waitFor();
            if (p.exitValue() == 2) {
                showNotification("MKV Movie Tag", "Error occurred when try to add attachments.", NotificationType.ERROR);
            }

            showNotification("MKV Movie Tag", "Tags saved successfully", NotificationType.SUCCESS);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void showNotification(String title, String message, NotificationType type) {
        TrayNotification tray = new TrayNotification();
        tray.setTitle(title);
        tray.setMessage(message);
        tray.setNotificationType(type);
        tray.setAnimationType(AnimationType.POPUP);
        tray.showAndDismiss(Duration.seconds(0.5));
    }
}

final class AlignedListViewCell extends ListCell<MovieDb> {
    @Override
    protected void updateItem(MovieDb item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            // Create the HBox
            HBox hBox = new HBox(5);

            // Create movie name Label
            Label name = new Label(item.getOriginalTitle());
            name.setAlignment(Pos.BASELINE_LEFT);
            HBox.setHgrow(name, Priority.NEVER);
            hBox.getChildren().add(name);

            Region space = new Region();
            HBox.setHgrow(space, Priority.ALWAYS);
            hBox.getChildren().add(space);

            // Create movie name Label
            Label date = new Label(item.getReleaseDate());
            date.setAlignment(Pos.BASELINE_RIGHT);
            HBox.setHgrow(date, Priority.NEVER);
            hBox.getChildren().add(date);

            setGraphic(hBox);
        }
    }
}
