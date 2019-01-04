package movietag;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        // Set proxy settings
        System.setProperty("http.proxyHost", "iproxy.intracomtel.com");
        System.setProperty("http.proxyPort", "80");
        System.setProperty("https.proxyHost", "iproxy.intracomtel.com");
        System.setProperty("https.proxyPort", "80");

        Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setTitle("Movie Tag");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
