package com.example.qflslibrarymanagement;
import com.example.qflslibrarymanagement.LibraryApp;
import com.example.qflslibrarymanagement.BookController;
import com.example.qflslibrarymanagement.Book;
import javafx.application.Platform;
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
import java.util.List;
import java.util.UUID;

public class BookScene extends Scene{
    private final BookController bookController;
    private final LibraryApp application;
    private final ObservableList<Book> bookList;
    private final TextField authorSearchField;
    private final ChoiceBox<Book.Genre> genreSearchBox;
    private final TableView<Book> table;
    private final TextField hiddenBarcodeField = new TextField();
    private Button scanCheckoutButton;

    public BookScene(BookController bookController, LibraryApp application) {
        super(new VBox(), 1200, 700);
        this.bookController = bookController;
        this.application = application;
        this.bookList = FXCollections.observableArrayList();
        this.authorSearchField = new TextField();
        authorSearchField.setPromptText("Search by Author");
        this.genreSearchBox = new ChoiceBox<>();

        this.table = createTable();
        hiddenBarcodeField.setVisible(false);
        hiddenBarcodeField.setManaged(false);
        setupBarcodeListener();
        var vBox = new VBox(10, createFilterBox(), table, createButtonBox());
        vBox.setPadding(new Insets(10));
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().add(hiddenBarcodeField);
        setRoot(vBox);

        // Initially load all books
        refreshTable();
    }
    private void setupBarcodeListener() {
        hiddenBarcodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.endsWith("\n")) {
                String barcode = newVal.trim();
                handleScannedBarcode(barcode);
                hiddenBarcodeField.clear();
            }
        });
    }
    private void handleScannedBarcode(String barcode) {
        bookController.getBookByBarcode(barcode, book -> {
            Platform.runLater(() -> {
                if (book != null) {
                    // 在表格中选中该书
                    table.getSelectionModel().select(book);
                    // 自动触发借书
                    bookController.checkoutByBarcode(barcode, updatedBook -> {
                        Platform.runLater(() -> {
                            refreshTable(); // 刷新表格显示最新状态
                            showAlert("借书成功: " + updatedBook.getTitle());
                        });
                    });
                } else {
                    showAlert("未找到条形码对应的图书");
                }
            });
        });
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
        idColumn.setPrefWidth(240);

        var titleColumn = new TableColumn<Book, String>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setPrefWidth(220);

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

        tableView.getColumns().addAll(idColumn, titleColumn, authorColumn, genreColumn, yearColumn, statusColumn);
        return tableView;
    }
    private HBox createFilterBox() {
        genreSearchBox.getItems().add(null); // for "All Genres"
        genreSearchBox.getItems().addAll(Book.Genre.values());

        var searchButton = new Button("Search");
        searchButton.setOnAction(event -> refreshTable());

        var hBox = new HBox(10, new Label("Author:"), authorSearchField, new Label("Genre:"), genreSearchBox, searchButton);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return hBox;
    }

    private HBox createButtonBox() {
        var backButton = new Button("Back");
        backButton.setOnAction(event -> application.showHomeScene());

        var addButton = new Button("Add Book");
        addButton.setOnAction(event -> showPopup(null));

        var refreshButton = new Button("Refresh All");
        refreshButton.setOnAction(event -> {
            authorSearchField.clear();
            genreSearchBox.setValue(null);
            refreshTable();
        });

        var checkoutButton = new Button("Check Out");
        var returnButton = new Button("Return");

        // Enable/disable buttons based on selection
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            checkoutButton.setDisable(newSelection == null || newSelection.getStatus() != Book.Status.AVAILABLE);
            returnButton.setDisable(newSelection == null || newSelection.getStatus() != Book.Status.CHECKED_OUT);
        });
        checkoutButton.setDisable(true);
        returnButton.setDisable(true);

        checkoutButton.setOnAction(e -> bookController.checkoutBook(table.getSelectionModel().getSelectedItem(), this::setBooks));
        returnButton.setOnAction(e -> bookController.returnBook(table.getSelectionModel().getSelectedItem(), this::setBooks));

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        scanCheckoutButton = new Button("扫码借书");
        scanCheckoutButton.setOnAction(e -> {
            hiddenBarcodeField.requestFocus();
            showAlert("请扫描图书条形码");
        });

        var buttonBox = new HBox(10,
                backButton, addButton, refreshButton, spacer,
                scanCheckoutButton, checkoutButton, returnButton);
        buttonBox.setAlignment(Pos.CENTER);
        return buttonBox;
    }
    private void refreshTable() {
        String author = authorSearchField.getText();
        Book.Genre genre = genreSearchBox.getValue();
        bookController.getAllBooks(author, genre, this::setBooks);
    }

    private void showPopup(Book book) {
        var popup = new Popup();
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
            genreBox.setValue(Book.Genre.FICTION); // Default value
        }

        var saveButton = new Button("Save");
        saveButton.setOnAction(event -> {
            if(genreBox.getValue() == null) {
                System.err.println("Genre cannot be empty.");
                return;
            }
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
                    // 新增书
                    bookList.add(newBook);  // <-- 关键：加入 ObservableList
                } else {
                    // 编辑书
                    int index = bookList.indexOf(book);
                    if (index >= 0) {
                        bookList.set(index, newBook); // 更新 ObservableList
                    }
                }

                popup.hide();
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format for year.");
            }
        });

        var cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> popup.hide());

        var deleteButton = new Button("Delete");
        deleteButton.setTextFill(Color.RED);
        deleteButton.setOnAction(event -> {
            if (book != null) {
                bookController.deleteBook(book, this::setBooks);
            }
            popup.hide();
        });

        var buttonBar = new HBox(10, saveButton, cancelButton);
        buttonBar.setAlignment(Pos.CENTER);
        if (book != null) {
            buttonBar.getChildren().add(deleteButton);
        }

        var vBox = new VBox(10, new Label(book == null ? "Add New Book" : "Edit Book"),
                titleField, authorField, isbnField, yearField, genreBox, buttonBar);
        vBox.setAlignment(Pos.CENTER);
        vBox.setBackground(new Background(new BackgroundFill(Color.WHITESMOKE, new CornerRadii(5), null)));
        vBox.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1))));
        vBox.setPrefWidth(300);
        vBox.setPadding(new Insets(15));
        popup.getContent().add(vBox);
        popup.show(application.getStage());
        popup.centerOnScreen();
    }
    private void setBooks(List<Book> books) {
        Platform.runLater(() -> {
            bookList.setAll(books);
            // After setting books, re-evaluate button disable state
            int selectedIndex = table.getSelectionModel().getSelectedIndex();
            table.getSelectionModel().clearSelection();
            table.getSelectionModel().select(selectedIndex);
        });
    }
}
