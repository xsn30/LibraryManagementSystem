package com.example.qflslibrarymanagement;
import com.example.qflslibrarymanagement.BookController;
import com.example.qflslibrarymanagement.BookScene;
import com.example.qflslibrarymanagement.HomeScene;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class LibraryApp extends Application {

    private final BookController bookController = new BookController();
    private Stage stage;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 700;

    public static void main(String[] args) {
        System.out.println("启动JavaFX应用...");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        initializeMainWindow();
        StackPane root = new StackPane(new Label("JavaFX启动成功！"));
        primaryStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        primaryStage.show();
        showHomeScene();
        System.out.println("JavaFX start()方法被调用");
        primaryStage.setTitle("Library Management");

        primaryStage.toFront();
        primaryStage.requestFocus();

    }
    private void initializeMainWindow() {
        stage.setTitle("图书管理系统");
        stage.setWidth(1200);
        stage.setHeight(700);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
    }


    public void showHomeScene() {
        HomeScene homeScene = new HomeScene(this);
        stage.setScene(homeScene);
        stage.show();
    }

    public void showBookScene() {
        BookScene bookScene = new BookScene(bookController, this);
        stage.setScene(bookScene);
        stage.show();
    }

    public Stage getStage() {
        return stage;
    }
}
