package com.example.qflslibrarymanagement;
import com.example.qflslibrarymanagement.BookController;
import com.example.qflslibrarymanagement.BookScene;
import com.example.qflslibrarymanagement.HomeScene;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class LibraryApp extends Application {

    private static String[] savedArgs;
    private BookController bookController;
    private static ConfigurableApplicationContext springContext;
    private Stage stage;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 700;

    public static void main(String[] args) {
        savedArgs = args;
        System.setProperty("spring.datasource.url", "jdbc:h2:file:./data/librarydb;DB_CLOSE_ON_EXIT=FALSE");
        System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "update");
        System.setProperty("spring.h2.console.enabled", "true");
        System.setProperty("spring.h2.console.path", "/h2-console");
        System.setProperty("spring.jpa.show-sql", "true");
        System.setProperty("spring.jpa.properties.hibernate.format_sql", "true");

        // 使用 SpringApplicationBuilder 并关闭 headless
        springContext = new SpringApplicationBuilder(LibraryApp.class)
                .headless(false)   // 保证 JavaFX 可以使用
                .run(args);

        // 启动 JavaFX
        Application.launch(args);
    }

    @Override
    public void init() {
        bookController = springContext.getBean(BookController.class);
        StudentService studentService = springContext.getBean(StudentService.class);
        studentService.initTestData(); // 初始化测试学生数据
    }


    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        initializeMainWindow();
        showHomeScene(); // 直接显示你的主界面
    }
    private void initializeMainWindow() {
        stage.setTitle("图书管理系统");
        stage.setWidth(WINDOW_WIDTH);
        stage.setHeight(WINDOW_HEIGHT);
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
        bookScene.refreshTable(); // <-- 一定要刷新一次，从数据库加载
    }

    public Stage getStage() {
        return stage;
    }
    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close(); // 关闭 Spring Context
        }
    }
}
