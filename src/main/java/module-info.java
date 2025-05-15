module com.example.outforecast {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;


    opens com.example.outforecast to javafx.fxml;
    exports com.example.outforecast;
}