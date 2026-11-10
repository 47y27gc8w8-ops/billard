module com.example.billard {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.billard to javafx.fxml;
    exports com.example.billard;
}