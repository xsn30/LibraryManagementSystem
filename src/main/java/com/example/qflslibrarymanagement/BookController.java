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

            // 检查新书是否在结果中
            boolean found = allBooks.stream().anyMatch(b -> b.getIsbn().equals(book.getIsbn()));

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
        boolean success = bookService.borrowBook("defaultUser", book.getId());
        if (success) book.setStatus(Book.Status.CHECKED_OUT);
        getAllBooks(null, null, callback);
    }

    public void handleCardScan(String cardId, Consumer<Student> callback) {
        studentService.getStudentByCardId(cardId).ifPresentOrElse(
                student -> {
                    this.currentStudent = student;
                    callback.accept(student);
                },
                () -> callback.accept(null)
        );
    }

    public void returnByBarcode(String barcode, Consumer<List<Book>> callback) {
        if (currentStudent == null) {
            getAllBooks(null, null, callback);
            return;
        }

        List<Book> allBooks = bookService.getAllBooks();
        for (Book book : allBooks) {
            if (book.getIsbn().equals(barcode)) {
                boolean success = bookService.returnBook(currentStudent.getId(), book.getId());
                getAllBooks(null, null, callback);
                return;
            }
        }
        getAllBooks(null, null, callback);
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
        List<Book> allBooks =bookService.getAllBooks();
        for (Book b : allBooks) {
            if (b.getIsbn().equals(barcode)) {
                checkoutBook(b, books -> callback.accept(b));
                return;
            }
        }
        callback.accept(null);
    }

    public void getBookByBarcode(String barcode, Consumer<Book> callback) {
        List<Book> allBooks = bookService.getAllBooks();
        for (Book b : allBooks) {
            if (b.getIsbn().equals(barcode)) {
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
    public void validateISBN(String isbn, Consumer<Boolean> callback) {
        // 简单的ISBN格式验证（13位数字）
        boolean isValid = isbn != null && isbn.matches("\\d{13}");
        callback.accept(isValid);
    }
    public String formatISBN(String rawBarcode) {
        if (rawBarcode == null) return null;

        String cleanISBN = rawBarcode.replaceAll("[^\\d]", "");

        // 尝试识别并格式化
        if (cleanISBN.length() == 10) {
            // ISBN-10格式
            return formatISBN10(cleanISBN);
        } else if (cleanISBN.length() == 13 && (cleanISBN.startsWith("978") || cleanISBN.startsWith("979"))) {
            // ISBN-13格式
            return formatISBN13(cleanISBN);
        } else {
            // 无法识别，返回原始数字
            return cleanISBN;
        }
    }

    private String formatISBN10(String isbn) {
        return isbn.substring(0, 1) + "-" +
                isbn.substring(1, 4) + "-" +
                isbn.substring(4, 9) + "-" +
                isbn.substring(9);
    }

    private String formatISBN13(String isbn) {
        return isbn.substring(0, 3) + "-" +
                isbn.substring(3, 4) + "-" +
                isbn.substring(4, 7) + "-" +
                isbn.substring(7, 12) + "-" +
                isbn.substring(12);
    }
}