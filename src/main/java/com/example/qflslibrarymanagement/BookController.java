package com.example.qflslibrarymanagement;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.layout.Region;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class BookController {
    private final BookService bookService;
    private Student currentStudent;
    private final StudentService studentService;

    public BookController(BookService bookService,StudentService studentService) {
        this.bookService = bookService;
        this.studentService = studentService;
    }

    public void getAllBooks(String author, Book.Genre genre, Consumer<List<Book>> callback) {
        List<Book> allBooks = bookService.getAllBooks();
        List<Book> filtered = allBooks.stream()
                .filter(b -> (author == null || author.isEmpty() || b.getAuthor().contains(author)) &&
                        (genre == null || b.getGenre() == genre))
                .toList();
        callback.accept(filtered);
    }

    public void addBook(Book book, Consumer<List<Book>> callback) {
        try {
            // 保存书籍
            Book savedBook = bookService.addBook(book);
            // 使用强制刷新的查询
            List<Book> allBooks = bookService.getAllBooksWithRefresh();

            callback.accept(allBooks);

        } catch (Exception e) {

            getAllBooks(null, null, callback);
        }
    }

    public void updateBook(Book book, Consumer<List<Book>> callback) {
        bookService.updateBook(book); // save 可以更新或新增
        getAllBooks(null, null, callback);
    }

    public void deleteBook(Book book, Consumer<List<Book>> callback) {
        bookService.deleteBook(book.getId());
        getAllBooks(null, null, callback);
    }
    public void checkoutBook(Book book, Consumer<List<Book>> callback) {
        if (currentStudent == null) {
            showAlert("请先选择学生");
            getAllBooks(null, null, callback);
            return;
        }

        boolean success = bookService.borrowBook(currentStudent.getId(), book.getId());
        if (success) book.setStatus(Book.Status.CHECKED_OUT);
        getAllBooks(null, null, callback);
    }

    public void handleCardScan(String cardId, Consumer<Student> callback) {
        System.out.println("[DEBUG] 正在查询卡号: " + cardId + " (原始值)");

        studentService.getStudentByCardId(cardId).ifPresentOrElse(
                student -> {
                    System.out.println("[DEBUG] 找到学生: " + student.getName());
                    callback.accept(student);
                },
                () -> {
                    System.out.println("[DEBUG] 未找到卡号: " + cardId);
                    // 打印数据库中所有卡号辅助调试
                    studentService.getAllStudents().forEach(s ->
                            System.out.println("现有卡号: " + s.getCardId())
                    );
                    callback.accept(null);
                }
        );
    }

    public void returnByBarcode(String barcode, Consumer<List<Book>> callback) {
        if (currentStudent == null) {
            showAlert("请先刷学生卡");
            getAllBooks(null, null, callback);
            return;
        }

        List<Book> allBooks = bookService.getAllBooks();
        for (Book book : allBooks) {
            if (book.getId().equals(barcode)) {
                boolean success = bookService.returnBook(currentStudent.getId(), book.getId());
                if (success) {
                    // 还书成功后刷新表格
                    getAllBooks(null, null, callback);
                } else {
                    showAlert("还书失败");
                    getAllBooks(null, null, callback);
                }
                return;
            }
        }
        showAlert("未找到对应的图书");
        getAllBooks(null, null, callback);
    }
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("系统提示");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    // 获取当前学生
    public Student getCurrentStudent() {
        return currentStudent;
    }

    // 清空当前学生
    public void clearCurrentStudent() {
        this.currentStudent = null;
    }

    public void returnBook(Book book, Consumer<List<Book>> callback) {
        boolean success = bookService.returnBook("defaultUser", book.getId());
        if (success) book.setStatus(Book.Status.AVAILABLE);
        getAllBooks(null, null, callback);
    }

    public void checkoutByBarcode(String barcode, Consumer<Book> callback) {
        if (currentStudent == null) {
            showAlert("请先刷学生卡");
            callback.accept(null);
            return;
        }
        List<Book> allBooks = bookService.getAllBooks();
        for (Book b : allBooks) {
            if (b.getId().equals(barcode)) {
                // 使用当前学生ID借书
                boolean success = bookService.borrowBook(currentStudent.getId(), b.getId());
                if (success) {
                    // 借书成功后刷新整个列表
                    getAllBooks(null, null, refreshedBooks -> {
                        Book borrowedBook = refreshedBooks.stream()
                                .filter(book -> book.getId().equals(barcode))
                                .findFirst()
                                .orElse(null);
                        callback.accept(borrowedBook);
                    });
                } else {
                    showAlert("借书失败：书籍可能已被借出");
                    callback.accept(null);
                }
                return;
            }
        }
        showAlert("未找到对应的图书");
        callback.accept(null);
    }

    public void getBookByBarcode(String barcode, Consumer<Book> callback) {
        List<Book> allBooks = bookService.getAllBooks();
        for (Book b : allBooks) {
            if (b.getId().equals(barcode)) {
                callback.accept(b);
                return;
            }
        }
        callback.accept(null);
    }
    public void checkOverdueBooks(Consumer<List<Book>> callback) {
        List<Book> allBooks = bookService.getAllBooks();
        List<Book> overdueBooks = allBooks.stream()
                .filter(Book::isOverdue)
                .collect(Collectors.toList());

        if (!overdueBooks.isEmpty()) {
            Platform.runLater(() -> {
                showOverdueAlert(overdueBooks);
            });
        }
        callback.accept(overdueBooks);
    }
    private void showOverdueAlert(List<Book> overdueBooks) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("超时书籍提醒");
        alert.setHeaderText("发现 " + overdueBooks.size() + " 本书籍超时未还");

        StringBuilder content = new StringBuilder("超时书籍列表:\n");
        for (Book book : overdueBooks) {
            content.append("📚 ").append(book.getTitle())
                    .append("\n  借书人: ").append(book.getBorrowerName() != null ? book.getBorrowerName() : "未知")
                    .append("\n  借出时间: ")
                    .append(book.getBorrowTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append("\n  超时天数: ")
                    .append(ChronoUnit.DAYS.between(
                            book.getBorrowTime().plusWeeks(2),
                            LocalDateTime.now()))
                    .append(" 天\n\n");
        }

        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setContentText(content.toString());
        alert.show();
    }
    public void getBooksByBorrower(String borrowerName, Consumer<List<Book>> callback) {
        List<Book> allBooks = bookService.getAllBooks();
        List<Book> filtered = allBooks.stream()
                .filter(b -> b.getBorrowerName() != null &&
                        b.getBorrowerName().contains(borrowerName))
                .collect(Collectors.toList());
        callback.accept(filtered);
    }
    public Book getBookById(String id) {
        List<Book> allBooks = bookService.getAllBooks();
        for (Book book : allBooks) {
            if (book.getId().equals(id)) {
                return book;
            }
        }
        return null;
    }
    public StudentService getStudentService() {
        return this.studentService;
    }
}