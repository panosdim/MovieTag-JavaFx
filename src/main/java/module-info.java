module com.panosdim.movietag {
    requires javafx.controls;
    requires javafx.fxml;
    requires info.movito.themoviedbapi;
    requires org.apache.commons.io;
    requires org.slf4j;
    requires jcodec;


    opens com.panosdim.movietag to javafx.fxml;
    exports com.panosdim.movietag;
}