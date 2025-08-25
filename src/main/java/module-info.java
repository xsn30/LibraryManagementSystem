module com.example.qflslibrarymanagement {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.qflslibrarymanagement to javafx.fxml;
    exports com.example.qflslibrarymanagement;
}