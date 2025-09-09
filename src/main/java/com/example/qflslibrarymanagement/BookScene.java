package com.example.qflslibrarymanagement;

import com.example.qflslibrarymanagement.LibraryApp;
import com.example.qflslibrarymanagement.BookController;
import com.example.qflslibrarymanagement.Book;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class BookScene extends Scene {
    private final BookController bookController;
    private final LibraryApp application;
    private final ObservableList<Book> bookList;
    private final TextField authorSearchField;
    private final ChoiceBox<Book.Genre> genreSearchBox;
    private final TableView<Book> table;
    private final TextField hiddenBarcodeField = new TextField();
    private final TextField hiddenCardField = new TextField();

    private Label currentStudentLabel = new Label("当前学生：未选择");
    private enum ScanMode { CHECKOUT, RETURN, ADD_BOOK}
    private ScanMode currentScanMode = ScanMode.CHECKOUT;

    public BookScene(BookController bookController, LibraryApp application) {
        super(new VBox(), 1200, 700);
        this.bookController = bookController;
        this.application = application;
        this.bookList = FXCollections.observableArrayList();
        this.authorSearchField = new TextField();
        authorSearchField.setPromptText("Search by Author");
        this.genreSearchBox = new ChoiceBox<>();

        this.table = createTable();
        setupBarcodeListener();
        setupCardReaderListener();

        VBox mainLayout = new VBox(10, createFilterBox(), currentStudentLabel, table, createButtonBox());
        mainLayout.setPadding(new Insets(10));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getChildren().addAll(hiddenBarcodeField, hiddenCardField);
        setRoot(mainLayout);

        refreshTable();
    }

    private void setupBarcodeListener() {
        hiddenBarcodeField.setVisible(false);
        hiddenBarcodeField.setManaged(false);

        hiddenBarcodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.endsWith("\n")) {
                String barcode = newVal.trim();
                handleScannedBarcode(barcode);
                hiddenBarcodeField.clear();
            }
        });
    }

    private void setupCardReaderListener() {
        hiddenCardField.setVisible(false);
        hiddenCardField.setManaged(false);

        hiddenCardField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.endsWith("\n")) {
                String cardId = newVal.trim();
                bookController.handleCardScan(cardId, student -> {
                    Platform.runLater(() -> {
                        if (student != null) {
                            currentStudentLabel.setText("当前学生：" + student.getName());
                            showAlert("学生卡验证成功: " + student.getName());
                        } else {
                            showAlert("未找到该学生信息");
                        }
                    });
                });
                hiddenCardField.clear();
            }
        });
    }

    private void handleScannedBarcode(String barcode) {
        if (currentScanMode == ScanMode.CHECKOUT) {
            // 借书模式
            bookController.checkoutByBarcode(barcode, book -> {
                Platform.runLater(() -> {
                    if (book != null) {
                        refreshTable();
                        showAlert("借书成功: " + book.getTitle());
                    } else {
                        showAlert("未找到条形码对应的图书");
                    }
                });
            });
        } else if (currentScanMode == ScanMode.RETURN) {
            // 还书模式
            bookController.returnByBarcode(barcode, books -> {
                Platform.runLater(() -> {
                    refreshTable();
                    showAlert("还书成功");
                });
            });
        } else if (currentScanMode == ScanMode.ADD_BOOK) {
            // 扫码添加模式 - 自动打开添加窗口并填充ISBN
            Platform.runLater(() -> {
                showPopupWithISBN(barcode); // 自动填充ISBN
            });
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }

    private TableView<Book> createTable() {
        var tableView = new TableView<>(bookList);
        tableView.setRowFactory(tv -> {
            TableRow<Book> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    showPopup(row.getItem());
                }
            });
            return row;
        });

        var idColumn = new TableColumn<Book, String>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(100);

        var titleColumn = new TableColumn<Book, String>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setPrefWidth(200);

        var authorColumn = new TableColumn<Book, String>("Author");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorColumn.setPrefWidth(150);

        var isbnColumn = new TableColumn<Book, String>("ISBN");
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnColumn.setPrefWidth(150);

        var genreColumn = new TableColumn<Book, Book.Genre>("Genre");
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        genreColumn.setPrefWidth(100);

        var yearColumn = new TableColumn<Book, Integer>("Year");
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("publishedYear"));
        yearColumn.setPrefWidth(80);

        var statusColumn = new TableColumn<Book, Book.Status>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(100);

        var borrowerColumn = new TableColumn<Book, String>("借书人");
        borrowerColumn.setCellValueFactory(new PropertyValueFactory<>("borrowerName"));
        borrowerColumn.setPrefWidth(100);

        var borrowTimeColumn = new TableColumn<Book, String>("借出时间");
        borrowTimeColumn.setCellValueFactory(cellData -> {
            Book book = cellData.getValue();
            if (book.getBorrowTime() != null) {
                return new SimpleStringProperty(book.getBorrowTime().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            return new SimpleStringProperty("");
        });
        borrowTimeColumn.setPrefWidth(120);

        var statusInfoColumn = new TableColumn<Book, String>("借阅状态");
        statusInfoColumn.setCellValueFactory(cellData -> {
            Book book = cellData.getValue();
            if (book.getStatus() == Book.Status.CHECKED_OUT) {
                if (book.isOverdue()) {
                    return new SimpleStringProperty("超时未还");
                } else {
                    return new SimpleStringProperty("剩余 " + book.getRemainingDays() + " 天");
                }
            }
            return new SimpleStringProperty("可借阅");
        });
        statusInfoColumn.setPrefWidth(100);

        statusInfoColumn.setCellFactory(column -> new TableCell<Book, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("超时")) {
                        setTextFill(Color.RED);
                        setStyle("-fx-font-weight: bold;");
                    } else if (item.contains("剩余")) {
                        setTextFill(Color.BLUE);
                    } else {
                        setTextFill(Color.GREEN);
                    }
                }
            }
        });

        tableView.getColumns().addAll(idColumn, titleColumn, authorColumn, isbnColumn, genreColumn, yearColumn, statusColumn,borrowerColumn,borrowTimeColumn, statusInfoColumn);
        return tableView;
    }

    private HBox createFilterBox() {
        genreSearchBox.getItems().add(null);
        genreSearchBox.getItems().addAll(Book.Genre.values());

        var searchButton = new Button("Search");
        searchButton.setOnAction(event -> refreshTable());

        TextField borrowerSearchField = new TextField();
        borrowerSearchField.setPromptText("Search by Borrower");

        Button borrowerSearchButton = new Button("Search Borrower");
        borrowerSearchButton.setOnAction(event -> {
            bookController.getBooksByBorrower(borrowerSearchField.getText(), this::setBooks);
        });


        return new HBox(10, new Label("Author:"), authorSearchField, new Label("Genre:"), genreSearchBox,new Label("Borrower:"), borrowerSearchField, borrowerSearchButton, searchButton);
    }

    private HBox createButtonBox() {
        var backButton = new Button("Back");
        backButton.setOnAction(event -> application.showHomeScene());

        var addButton = new Button("Add Book");
        addButton.setOnAction(event -> showPopup(null));

        var refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> {
            authorSearchField.clear();
            genreSearchBox.setValue(null);
            refreshTable();
        });

        var checkoutButton = new Button("Check Out");
        var returnButton = new Button("Return");

        var scanCheckoutButton = new Button("扫码借书");
        var scanReturnButton = new Button("扫码还书");
        var scanCardButton = new Button("刷学生卡");
        var clearStudentButton = new Button("清除学生");
        var scanAddButton = new Button("扫码添加书籍");
        scanAddButton.setOnAction(e -> {
            hiddenBarcodeField.requestFocus();
            currentScanMode = ScanMode.ADD_BOOK;
            showAlert("请扫描新书的ISBN条形码");
        });

        // 按钮事件
        scanCheckoutButton.setOnAction(e -> {
            hiddenBarcodeField.requestFocus();
            currentScanMode = ScanMode.CHECKOUT;
            showAlert("请扫描图书条形码");
        });

        scanReturnButton.setOnAction(e -> {
            hiddenBarcodeField.requestFocus();
            currentScanMode = ScanMode.RETURN;
            showAlert("请扫描要归还的书籍ISBN");
        });

        scanCardButton.setOnAction(e -> {
            hiddenCardField.requestFocus();
            showAlert("请刷学生卡（输入卡号后按回车）");
        });

        clearStudentButton.setOnAction(e -> {
            bookController.clearCurrentStudent();
            currentStudentLabel.setText("当前学生：未选择");
            showAlert("已清除当前学生");
        });

        var checkOverdueButton = new Button("检查超时");
        checkOverdueButton.setOnAction(e -> {
            bookController.checkOverdueBooks(overdueBooks -> {
                Platform.runLater(() -> {
                    if (overdueBooks.isEmpty()) {
                        showAlert("没有超时书籍");
                    }
                });
            });
        });

        // 表格选择监听
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            checkoutButton.setDisable(newSelection == null || newSelection.getStatus() != Book.Status.AVAILABLE);
            returnButton.setDisable(newSelection == null || newSelection.getStatus() != Book.Status.CHECKED_OUT);
        });
        checkoutButton.setDisable(true);
        returnButton.setDisable(true);

        checkoutButton.setOnAction(e ->
                bookController.checkoutBook(table.getSelectionModel().getSelectedItem(), this::setBooks));
        returnButton.setOnAction(e ->
                bookController.returnBook(table.getSelectionModel().getSelectedItem(), this::setBooks));

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(10,
                backButton, addButton, scanAddButton,refreshButton,checkOverdueButton, spacer,
                scanCardButton, clearStudentButton,
                scanCheckoutButton, scanReturnButton,
                checkoutButton, returnButton
        );
    }

    public void refreshTable() {
        String author = authorSearchField.getText();
        Book.Genre genre = genreSearchBox.getValue();
        bookController.getAllBooks(author, genre, this::setBooks);
    }

    private void showPopup(Book book) {
        var popup = new Popup();
        var vBox = new VBox(10);
        vBox.setPadding(new Insets(15));
        vBox.setBackground(new Background(new BackgroundFill(Color.WHITESMOKE, null, null)));
        vBox.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));

        var titleField = new TextField();
        titleField.setPromptText("Book Title");
        var authorField = new TextField();
        authorField.setPromptText("Author");
        var isbnField = new TextField();
        isbnField.setPromptText("ISBN");
        var yearField = new TextField();
        yearField.setPromptText("Published Year");
        var genreBox = new ChoiceBox<Book.Genre>();
        genreBox.getItems().addAll(Book.Genre.values());

        if (book != null) {
            titleField.setText(book.getTitle());
            authorField.setText(book.getAuthor());
            isbnField.setText(book.getIsbn());
            yearField.setText(String.valueOf(book.getPublishedYear()));
            genreBox.setValue(book.getGenre());
        } else {
            genreBox.setValue(Book.Genre.FICTION);
        }

        var saveButton = new Button("Save");
        var cancelButton = new Button("Cancel");
        var deleteButton = new Button("Delete");
        deleteButton.setTextFill(Color.RED);

        saveButton.setOnAction(event -> {
            try {
                var newBook = book != null ? book : new Book(
                        UUID.randomUUID().toString(),
                        titleField.getText(),
                        authorField.getText(),
                        Integer.parseInt(yearField.getText()),
                        isbnField.getText(),
                        genreBox.getValue(),
                        Book.Status.AVAILABLE
                );

                if (book == null) {
                    bookController.addBook(newBook, this::setBooks);
                } else {
                    bookController.updateBook(newBook, this::setBooks);
                }
                popup.hide();
            } catch (Exception e) {
                showAlert("输入格式错误");
            }
        });

        cancelButton.setOnAction(event -> popup.hide());
        deleteButton.setOnAction(event -> {
            if (book != null) bookController.deleteBook(book, this::setBooks);
            popup.hide();
        });

        var buttonBar = new HBox(10, saveButton, cancelButton);
        if (book != null) buttonBar.getChildren().add(deleteButton);

        vBox.getChildren().addAll(
                new Label(book == null ? "Add New Book" : "Edit Book"),
                titleField, authorField, isbnField, yearField, genreBox, buttonBar
        );


        Label borrowInfoLabel = new Label();
        if (book != null && book.getStatus() == Book.Status.CHECKED_OUT) {
            StringBuilder info = new StringBuilder();
            info.append("借书人: ").append(book.getBorrowerName() != null ? book.getBorrowerName() : "未知")
                    .append("\n借出时间: ").append(book.getBorrowTime().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append("\n应还时间: ").append(book.getBorrowTime().plusWeeks(2).format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            if (book.isOverdue()) {
                long overdueDays = ChronoUnit.DAYS.between(
                        book.getBorrowTime().plusWeeks(2),
                        LocalDateTime.now());
                info.append("\n状态: 已超时 ").append(overdueDays).append(" 天!");
            } else {
                info.append("\n剩余天数: ").append(book.getRemainingDays()).append(" 天");
            }

            borrowInfoLabel.setText(info.toString());
            borrowInfoLabel.setTextFill(book.isOverdue() ? Color.RED : Color.BLUE);
        }
        vBox.getChildren().add(borrowInfoLabel);

        popup.getContent().add(vBox);
        popup.show(application.getStage());
        popup.setAutoHide(true);
    }
    private void showPopupWithISBN(String isbn) {var popup = new Popup();
        var titleField = new TextField();
        titleField.setPromptText("Book Title");
        var authorField = new TextField();
        authorField.setPromptText("Author");
        var isbnField = new TextField();
        isbnField.setPromptText("ISBN");
        var yearField = new TextField();
        yearField.setPromptText("Published Year");
        var genreBox = new ChoiceBox<Book.Genre>();
        genreBox.getItems().addAll(Book.Genre.values());
        isbnField.setText(isbn);
        genreBox.setValue(Book.Genre.FICTION); // 默认类型
        bookController.validateISBN(isbn, valid -> {
            Platform.runLater(() -> {
                if (!valid) {
                    showAlert("警告：ISBN格式可能不正确（应为13位数字）");
                }
            });
        });

        // 自动聚焦到书名字段，方便继续输入
        Platform.runLater(() -> titleField.requestFocus());

        var saveButton = new Button("Save");
        var cancelButton = new Button("Cancel");
        saveButton.setOnAction(event -> {
            try {
                var newBook = new Book(
                        UUID.randomUUID().toString(),
                        titleField.getText(),
                        authorField.getText(),
                        Integer.parseInt(yearField.getText()),
                        isbnField.getText(),
                        genreBox.getValue(),
                        Book.Status.AVAILABLE
                );

                // 正确的调用方式：使用bookController而不是bookService
                bookController.addBook(newBook, books -> {
                    System.out.println("回调收到的书籍数量: " + books.size());
                    books.forEach(b -> System.out.println("已有: " + b.getIsbn() + " - " + b.getTitle()));

                    Platform.runLater(() -> {
                        setBooks(books);
                        popup.hide();
                        showAlert("尝试添加: " + newBook.getTitle());
                    });
                });

            } catch (NumberFormatException e) {
                showAlert("请输入正确的出版年份");
            } catch (Exception e) {
                showAlert("保存失败: " + e.getMessage());
            }
        });
        cancelButton.setOnAction(event -> popup.hide());

        var buttonBar = new HBox(10, saveButton, cancelButton);
        buttonBar.setAlignment(Pos.CENTER);

        var vBox = new VBox(10, new Label("扫码添加新书 - 请补充书籍信息"),
                new Label("ISBN: " + isbn), // 显示扫描的ISBN
                new Label("书名:"), titleField,
                new Label("作者:"), authorField,
                new Label("出版年:"), yearField,
                new Label("类型:"), genreBox,
                buttonBar);

        vBox.setAlignment(Pos.CENTER);
        vBox.setBackground(new Background(new BackgroundFill(Color.WHITESMOKE, new CornerRadii(5), null)));
        vBox.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1))));
        vBox.setPrefWidth(350);
        vBox.setPadding(new Insets(15));

        popup.getContent().add(vBox);
        popup.show(application.getStage());
        popup.setAutoHide(true);
    }


    private void setBooks(List<Book> books) {
        System.out.println("🎯 setBooks 被调用，收到 " + books.size() + " 本书");

        Platform.runLater(() -> {
            System.out.println("🖥️ 开始更新UI表格");
            bookList.setAll(books);
            System.out.println("✨ 表格更新完成，当前显示: " + bookList.size() + " 本书");

            // 强制刷新表格显示
            table.refresh();
        });
    }
}