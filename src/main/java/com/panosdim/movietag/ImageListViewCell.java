package com.panosdim.movietag;

import info.movito.themoviedbapi.model.core.Movie;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Map;

public final class ImageListViewCell extends ListCell<Movie> {
    private final String baseUrl;
    private final String posterSize;
    private final Map<String, Image> imageCache;

    public ImageListViewCell(String baseUrl, String posterSize, Map<String, Image> imageCache) {
        this.baseUrl = baseUrl;
        this.posterSize = posterSize;
        this.imageCache = imageCache;
    }

    @Override
    protected void updateItem(Movie item, boolean empty) {
        String releaseYear = "";
        if (item != null && item.getReleaseDate() != null && !item.getReleaseDate().isEmpty()) {
            releaseYear = item.getReleaseDate().substring(0, 4);
        }
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            // Create the HBox to hold our components
            HBox hBox = new HBox(10);
            hBox.setAlignment(Pos.CENTER_LEFT); // Align items to the left within HBox

            // Create the ImageView for the movie poster
            ImageView imageView = new ImageView();
            imageView.setFitHeight(100);
            imageView.setPreserveRatio(true);

            String imageUrl = baseUrl + posterSize + item.getPosterPath();

            // Check if the image is already in the cache
            Image posterImage = imageCache.get(imageUrl);
            if (posterImage == null) {
                // If not, load it and put it in the cache
                posterImage = new Image(imageUrl, true);
                imageCache.put(imageUrl, posterImage);
            }
            imageView.setImage(posterImage);

            // Create a Label for the movie title
            Label name = new Label(item.getOriginalTitle());
            name.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            name.setAlignment(Pos.CENTER_LEFT);

            // Create a Label for the release date
            Label releaseDate = new Label(releaseYear);
            releaseDate.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            releaseDate.setAlignment(Pos.CENTER_LEFT);

            // Create a Label for the movie description
            Label description = new Label(item.getOverview());
            description.setAlignment(Pos.CENTER_LEFT);
            description.setWrapText(true);
            description.setMaxWidth(540);
            description.setPrefWidth(540);

            // Create a VBox to hold name, description, and release date
            VBox vBox = new VBox(5, name, description, releaseDate);

            // Create a spacer Region for nice formatting
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Add components to the HBox
            hBox.getChildren().addAll(imageView, vBox, spacer);
            // Set the graphic for the ListCell
            setGraphic(hBox);
        }
    }
}