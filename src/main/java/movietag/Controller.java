package movietag;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbSearch;
import info.movito.themoviedbapi.model.MovieDb;
import info.movito.themoviedbapi.model.config.TmdbConfiguration;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.util.Callback;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Controller {
    public Button btnOpenFile;
    public TextField txtSearch;
    public Button btnSearch;
    public ListView<MovieDb> lstResults;
    public Button btnSave;
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

        lstResults.setCellFactory(new Callback<ListView<MovieDb>, ListCell<MovieDb>>() {
            @Override
            public ListCell<MovieDb> call(ListView<MovieDb> param) {
                final Tooltip tooltip = new Tooltip();
                tooltip.setFont(new Font(12));
                ListCell<MovieDb> cell = new ListCell<MovieDb>() {
                    @Override
                    protected void updateItem(MovieDb item, boolean bln) {
                        super.updateItem(item, bln);
                        if (item != null) {
                            setText(String.format("%-50.50s %10s", item.getOriginalTitle(), item.getReleaseDate()));
                        } else {
                            setText(null);
                        }
                    }
                };

                cell.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                    if (isNowHovered && !cell.isEmpty()) {
                        MovieDb movie = cell.getItem();
                        StringBuilder sb = new StringBuilder(movie.getOverview());

                        int i = 0;
                        while ((i = sb.indexOf(" ", i + 30)) != -1) {
                            sb.replace(i, i + 1, "\n");
                        }
                        tooltip.setText(sb.toString());
                        tooltip.setGraphic(new ImageView(new Image(baseUrl + "w45" + movie.getPosterPath(), 45, 64, false, false)));
                        cell.setTooltip(tooltip);
                    }
                });

                return cell;
            }
        });
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
            txtSearch.setText(FilenameUtils.removeExtension(movie.getName()));
        }
    }

    private int stringToFourcc(String fourcc) {
        if (fourcc.length() != 4)
            return 0;
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
        lstResults.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> btnSave.setDisable(false));
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
                        iTunesMeta.put(stringToFourcc("covr"), MetaValue.createOther(MetaValue.TYPE_JPEG, this.getValue()));
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
