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
    private enum ScanMode { CHECKOUT, RETURN}
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
            if (!newVal.isEmpty()) {  // 不再依赖换行符
                String cardId = newVal.trim();

                // 调试日志
                System.out.println("原始输入内容: [" + newVal + "]");
                System.out.println("处理后卡号: [" + cardId + "]");
                System.out.println("卡号长度: " + cardId.length());

                bookController.handleCardScan(cardId, student -> {
                    Platform.runLater(() -> {
                        if (student != null) {
                            currentStudentLabel.setText("当前学生：" + student.getName());
                            showAlert("验证成功: " + student.getName());
                            System.out.println("✅ 学生卡验证成功: " + student.getName() +
                                    ", 卡号: " + student.getCardId());
                        } else {
                            currentStudentLabel.setText("当前学生：未选择");
                            showAlert("未找到该学生信息: " + cardId);
                            showAlert("可用测试卡号: CARD001, CARD002, CARD003");
                            System.out.println("❌ 未找到卡号: " + cardId);

                            // 打印数据库中所有学生卡号
                            List<Student> allStudents = bookController.getStudentService().getAllStudents();
                            System.out.println("数据库中现有卡号:");
                            allStudents.forEach(s -> System.out.println(" - " + s.getCardId()));
                        }
                    });
                });

                hiddenCardField.clear(); // 清空输入框
            }
        });
    }

    private void handleScannedBarcode(String barcode) {
        if (currentScanMode == ScanMode.CHECKOUT) {
            // 检查是否已经选择了学生
            if (bookController.getCurrentStudent() == null) {
                showAlert("❌ 借书失败：请先刷学生卡再借书");
                // 自动切换到刷卡模式
                hiddenCardField.requestFocus();
                showAlert("请先刷学生卡（输入卡号后按回车）");
                return;
            }

            // 借书模式
            bookController.checkoutByBarcode(barcode, book -> {
                Platform.runLater(() -> {
                    if (book != null) {
                        refreshTable();
                        showAlert("✅ 借书成功: " + book.getTitle());
                    } else {
                        showAlert("❌ 未找到条形码对应的图书");
                    }
                });
            });
        }  else if (currentScanMode == ScanMode.RETURN) {
            // 还书模式
            bookController.returnByBarcode(barcode, books -> {
                Platform.runLater(() -> {
                    refreshTable();
                    showAlert("✅ 还书成功");
                });
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

        tableView.getColumns().addAll(idColumn, titleColumn, authorColumn,  genreColumn, yearColumn, statusColumn,borrowerColumn,borrowTimeColumn, statusInfoColumn);
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

        // 按钮事件
        scanCheckoutButton.setOnAction(e -> {
            // 检查是否已经选择了学生
            if (bookController.getCurrentStudent() == null) {
                showAlert("请先刷学生卡再借书");
                // 自动聚焦到刷卡字段
                hiddenCardField.requestFocus();
                showAlert("请刷学生卡（输入卡号后按回车）");
                return;
            }

            hiddenBarcodeField.requestFocus();
            currentScanMode = ScanMode.CHECKOUT;
            showAlert("请扫描图书条形码");
        });

        scanReturnButton.setOnAction(e -> {
            hiddenBarcodeField.requestFocus();
            currentScanMode = ScanMode.RETURN;
            showAlert("请扫描要归还的书籍条码");
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
                backButton, addButton, refreshButton,checkOverdueButton, spacer,
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

        var idField = new TextField();
        idField.setPromptText("扫描图书条码 (UUID)");
        var titleField = new TextField();
        titleField.setPromptText("Book Title");
        var authorField = new TextField();
        authorField.setPromptText("Author");
        var yearField = new TextField();
        yearField.setPromptText("Published Year");
        var genreBox = new ChoiceBox<Book.Genre>();
        genreBox.getItems().addAll(Book.Genre.values());

        if (book != null) {
            // 编辑现有书籍：显示ID但不能修改
            idField.setText(book.getId());
            idField.setDisable(true); // 禁止修改已有书籍的ID
            titleField.setText(book.getTitle());
            authorField.setText(book.getAuthor());
            yearField.setText(String.valueOf(book.getPublishedYear()));
            genreBox.setValue(book.getGenre());
        } else {
            idField.setDisable(false);
            genreBox.setValue(Book.Genre.FICTION);
        }
        idField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 10 && !newVal.equals(oldVal)) {
                // 扫码枪通常以回车结束，或者我们可以检测到足够长的输入
                Platform.runLater(() -> {
                    // 自动聚焦到下一个字段，提升用户体验
                    titleField.requestFocus();
                });
            }
        });

        var saveButton = new Button("Save");
        var cancelButton = new Button("Cancel");
        var deleteButton = new Button("Delete");
        deleteButton.setTextFill(Color.RED);

        saveButton.setOnAction(event -> {
            try {
                String bookId = idField.getText().trim();
                if (bookId.isEmpty()) {
                    showAlert("请先扫描图书条码（UUID）");
                    return;
                }

                // 检查ID是否已存在（只在添加新书时检查）
                if (book == null) {
                    Book existingBook = bookController.getBookById(bookId);
                    if (existingBook != null) {
                        showAlert("该条码已被使用，请扫描其他条码");
                        return;
                    }
                }
                var newBook = book != null ? book : new Book(
                        bookId, // 使用扫描的UUID
                        titleField.getText(),
                        authorField.getText(),
                        Integer.parseInt(yearField.getText()),
                        genreBox.getValue(),
                        Book.Status.AVAILABLE
                );
                if (book != null) {
                    newBook.setTitle(titleField.getText());
                    newBook.setAuthor(authorField.getText());
                    newBook.setPublishedYear(Integer.parseInt(yearField.getText()));
                    newBook.setGenre(genreBox.getValue());
                    newBook.setStatus(book.getStatus());
                    newBook.setBorrowTime(book.getBorrowTime());
                    newBook.setReturnTime(book.getReturnTime());
                    newBook.setBorrowerId(book.getBorrowerId());
                    newBook.setBorrowerName(book.getBorrowerName());
                }
                if (book == null) {
                    bookController.addBook(newBook, this::setBooks);
                } else {
                    bookController.updateBook(newBook, this::setBooks);
                }
                popup.hide();
            } catch (Exception e) {
                showAlert("输入格式错误: " + e.getMessage());
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
                new Label(book == null ? "添加新书 - 请先扫描条码" : "编辑书籍"),
                new Label("图书条码 (UUID):"), idField,
                new Label("书名:"), titleField,
                new Label("作者:"), authorField,
                new Label("出版年份:"), yearField,
                new Label("类型:"), genreBox,
                buttonBar
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
        if (book == null) {
            Platform.runLater(() -> idField.requestFocus());
        }
    }
    private void setBooks(List<Book> books) {
        System.out.println("🎯 setBooks 被调用，收到 " + books.size() + " 本书");

        Platform.runLater(() -> {
            bookList.setAll(books);
            table.refresh();
        });
    }
}