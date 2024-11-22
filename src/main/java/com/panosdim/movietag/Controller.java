package com.panosdim.movietag;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbConfiguration;
import info.movito.themoviedbapi.TmdbSearch;
import info.movito.themoviedbapi.model.core.Movie;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.tools.TmdbException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jcodec.containers.mp4.boxes.MetaValue;
import org.jcodec.movtool.MetadataEditor;
import org.jcodec.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Controller {
    public Button btnOpenFile;
    public TextField txtSearch;
    public Button btnSearch;
    public Button btnWriteTags;
    public ListView<Movie> lstResults;
    public Label lblStatus;
    private TmdbApi tmdb;
    private String baseUrl;
    private String posterSize;
    private File mkvPropEdit;
    private File movie;


    // Map to cache images
    private final Map<String, Image> imageCache = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    public void initialize() {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("tmdb.properties"));
        } catch (IOException e) {
            logger.error("Error loading tmdb.properties", e);
            btnOpenFile.setDisable(true);
            setStatusMessage("Cannot load properties file 'tmdb.properties'", StatusType.ERROR);
            return;
        }

        String mkvPropEditName = System.getProperty("os.name").toLowerCase().contains("win") ? "mkvpropedit.exe" : "mkvpropedit";
        try (InputStream mkv = getClass().getResourceAsStream(mkvPropEditName)) {
            mkvPropEdit = File.createTempFile("mkvpropedit", System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "");
            mkvPropEdit.deleteOnExit();
            Files.copy(Objects.requireNonNull(mkv), mkvPropEdit.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Error initializing mkvPropEdit", e);
            btnOpenFile.setDisable(true);
            setStatusMessage("Error initializing mkvPropEdit", StatusType.ERROR);
            return;
        }

        tmdb = new TmdbApi(props.getProperty("KEY"));

        try {
            TmdbConfiguration config = tmdb.getConfiguration();
            List<String> posterSizes = config.getDetails().getImageConfig().getPosterSizes();
            posterSize = "w500";
            if (!posterSizes.contains("w500")) {
                posterSize = posterSizes.getFirst();
            }
            baseUrl = config.getDetails().getImageConfig().getSecureBaseUrl();
        } catch (TmdbException e) {
            logger.error("Error getting configuration", e);
            btnOpenFile.setDisable(true);
            setStatusMessage("Check your Internet connection and try again", StatusType.ERROR);
            return;
        }

        lstResults.setCellFactory(imageListView -> new ImageListViewCell(baseUrl, posterSize, imageCache));
        setStatusMessage("Please select a movie file by clicking on 'Open Movie'", StatusType.INFO);
    }

    @FXML
    protected void onOpenFileButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Movie File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Movie Files", "*.mp4", "*.mkv"));
        // Set initial directory to Downloads folder
        File downloadsFolder = new File(System.getProperty("user.home"), "Downloads");
        if (downloadsFolder.exists() && downloadsFolder.isDirectory()) {
            fileChooser.setInitialDirectory(downloadsFolder);
        }
        movie = fileChooser.showOpenDialog(btnOpenFile.getScene().getWindow());

        if (movie != null) {
            lstResults.getSelectionModel().clearSelection();
            lstResults.getItems().clear();
            txtSearch.setDisable(false);
            btnSearch.setDisable(false);
            btnWriteTags.setDisable(true);
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
            setStatusMessage("Please press 'Search' button", StatusType.INFO);
        }
    }

    @FXML
    protected void onSearchTMDBButtonClick() {
        setStatusMessage("Please select a movie from the list and press 'Write Tags' button", StatusType.INFO);
        lstResults.getSelectionModel().clearSelection();
        btnWriteTags.setDisable(true);
        TmdbSearch search = tmdb.getSearch();

        MovieResultsPage searchResults;
        try {
            searchResults = search.searchMovie(txtSearch.getText(), false, null, null, 0, null, null);
        } catch (TmdbException e) {
            logger.error("Error searching movie", e);
            setStatusMessage("Error searching movie please try again", StatusType.WARNING);
            return;
        }
        lstResults.setItems(FXCollections.observableArrayList(searchResults.getResults()));
        lstResults.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> btnWriteTags.setDisable(false));
    }

    @FXML
    protected void onWriteTagsButtonClick() {
        btnWriteTags.setDisable(true);

        String ext = FilenameUtils.getExtension(movie.getName());
        if (ext.equals("mp4")) {
            writeMp4Tags();
        } else {
            writeMkvTags();
        }
        btnWriteTags.setDisable(false);
    }

    private void setStatusMessage(String message, StatusType statusType) {
        lblStatus.setStyle(statusType.getStyle());
        lblStatus.setText(message);
    }

    private File downloadImage(String imageUrl) throws IOException, URISyntaxException {
        File imageFile = File.createTempFile("cover", ".jpg");
        imageFile.deleteOnExit();
        URL url = new URI(imageUrl).toURL();
        FileUtils.copyURLToFile(url, imageFile);
        return imageFile;
    }

    private int stringToFourcc(String fourcc) {
        if (fourcc.length() != 4) {
            return 0;
        }
        byte[] bytes = Platform.getBytesForCharset(fourcc, String.valueOf(Charset.defaultCharset()));
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private void writeMp4Tags() {
        Movie selectedMovie = lstResults.getSelectionModel().getSelectedItem();

        try {
            MetadataEditor mediaMeta = MetadataEditor.createFrom(movie);
            Map<Integer, MetaValue> iTunesMeta = mediaMeta.getItunesMeta();
            if (iTunesMeta != null) {
                File imageFile = downloadImage(baseUrl + posterSize + selectedMovie.getPosterPath());

                iTunesMeta.clear();
                iTunesMeta.put(stringToFourcc("covr"), MetaValue.createOther(MetaValue.TYPE_JPEG, FileUtils.readFileToByteArray(imageFile)));
                iTunesMeta.put(stringToFourcc("name"), MetaValue.createString(selectedMovie.getOriginalTitle()));
                iTunesMeta.put(stringToFourcc("ldes"), MetaValue.createString(selectedMovie.getOverview()));
                try {
                    mediaMeta.save(true); // fast mode is on
                    setStatusMessage("Tags saved successfully", StatusType.INFO);
                } catch (IOException e) {
                    setStatusMessage("Error writing MP4 tags", StatusType.ERROR);
                    logger.error("Error writing MP4 tags", e);
                }
            } else {
                logger.error("iTunes meta is null");
                setStatusMessage("Error getting iTunes metadata", StatusType.ERROR);
            }
        } catch (IOException e) {
            setStatusMessage("Error writing MP4 tags", StatusType.ERROR);
            logger.error("Error writing MP4 tags", e);
        } catch (URISyntaxException e) {
            setStatusMessage("Download image URL is not correct", StatusType.ERROR);
            logger.error("Download image URL is not correct", e);
        }
    }

    private void writeMkvTags() {
        Movie selectedMovie = lstResults.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) return;

        try {
            File imageFile = downloadImage(baseUrl + posterSize + selectedMovie.getPosterPath());

            Process process;
            process = new ProcessBuilder(mkvPropEdit.getAbsolutePath(), movie.getAbsolutePath(), "--delete-attachment", "mime-type:image/jpeg").start();
            process.waitFor();
            if (process.exitValue() == 2) {
                setStatusMessage("Error occurred when try to delete attachments.", StatusType.ERROR);
            }

            process = new ProcessBuilder(mkvPropEdit.getAbsolutePath(), movie.getAbsolutePath(),
                    "--attachment-name", "cover.jpg",
                    "--attachment-mime-type", "image/jpeg",
                    "--add-attachment", imageFile.getAbsolutePath()).start();
            process.waitFor();
            if (process.exitValue() == 2) {
                setStatusMessage("Error occurred when try to add attachments.", StatusType.ERROR);
            }

            setStatusMessage("Tags saved successfully", StatusType.INFO);
        } catch (InterruptedException e) {
            logger.error("Error writing MKV tags", e);
            setStatusMessage("Error writing MKV tags", StatusType.ERROR);
        } catch (URISyntaxException e) {
            setStatusMessage("Download image URL is not correct", StatusType.ERROR);
            logger.error("Download image URL is not correct", e);
        } catch (IOException e) {
            setStatusMessage("Error creating image file", StatusType.ERROR);
            logger.error("Error creating image file", e);
        }
    }
}