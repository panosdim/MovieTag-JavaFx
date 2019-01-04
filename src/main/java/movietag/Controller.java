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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jcodec.containers.mp4.boxes.MetaValue;
import org.jcodec.movtool.MetadataEditor;
import org.jcodec.platform.Platform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

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

    private String fourccToString(int key) {
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).putInt(key);
        return Platform.stringFromCharset(bytes, Charset.defaultCharset());
    }

    private int stringToFourcc(String fourcc) {
        if (fourcc.length() != 4)
            return 0;
        byte[] bytes = Platform.getBytesForCharset(fourcc, Charset.defaultCharset());
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private byte[] downloadUrl(URL toDownload) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            byte[] chunk = new byte[4096];
            int bytesRead;
            InputStream stream = toDownload.openStream();

            while ((bytesRead = stream.read(chunk)) > 0) {
                outputStream.write(chunk, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return outputStream.toByteArray();
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
        MovieDb selectedMovie = lstResults.getSelectionModel().getSelectedItem();

        String ext = FilenameUtils.getExtension(movie.getName());
        if (ext.equals("mp4")) {
            try {
                mediaMeta = MetadataEditor.createFrom(movie);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Map<Integer, MetaValue> iTunesMeta = mediaMeta.getItunesMeta();
            if (iTunesMeta != null) {
                URL imgPath;
                try {
                    imgPath = new URL(baseUrl + posterSize + selectedMovie.getPosterPath());
                    Task<byte[]> task = new Task<byte[]>() {
                        @Override
                        public byte[] call() {
                            return downloadUrl(imgPath);
                        }

                        @Override
                        protected void succeeded() {
                            super.succeeded();
                            iTunesMeta.put(stringToFourcc("covr"), MetaValue.createOther(MetaValue.TYPE_JPEG, this.getValue()));
                            try {
                                mediaMeta.save(true); // fast mode is on
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            btnSave.setDisable(false);
                        }
                    };

                    new Thread(task).start();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                Process p;
                File image = File.createTempFile("cover", ".jpg");
                image.deleteOnExit();
                FileUtils.copyURLToFile(new URL(baseUrl + posterSize + selectedMovie.getPosterPath()), image);

                p = new ProcessBuilder(mkvPropEdit.getAbsolutePath(), movie.getAbsolutePath(), "--delete-attachment", "mime-type:image/jpeg").start();
                p.waitFor();
                if (p.exitValue() == 2) {
                    System.out.println("Error occurred when try to delete attachments.");
                }

                p = new ProcessBuilder(mkvPropEdit.getAbsolutePath(), movie.getAbsolutePath(),
                        "--attachment-name", "cover.jpg",
                        "--attachment-mime-type", "image/jpeg",
                        "--add-attachment", image.getAbsolutePath()).start();
                p.waitFor();
                if (p.exitValue() == 2) {
                    System.out.println("Error occurred when try to add attachments.");
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
