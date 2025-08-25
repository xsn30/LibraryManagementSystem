package com.example.qflslibrarymanagement;

import com.example.qflslibrarymanagement.LibraryApp;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class HomeScene extends Scene {
    public HomeScene(LibraryApp application) {
        super(new VBox(), 640, 500);

        var title = new Label("Library Management System");
        title.setFont(new Font(24));

        var bookButton = new Button("Manage Books");
        bookButton.setOnAction(event -> application.showBookScene());
        bookButton.setPrefSize(150, 30);

        var vBox = new VBox(20, title, bookButton);
        vBox.setAlignment(Pos.CENTER);
        setRoot(vBox);
    }
}