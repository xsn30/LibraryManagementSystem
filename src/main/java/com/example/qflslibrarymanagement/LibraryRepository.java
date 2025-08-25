package com.example.qflslibrarymanagement;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LibraryRepository {
    private static final String DB_URL = "jdbc:sqlite:library.db";

    // 添加书
    public void addBook(Book book) {
        String sql = "INSERT INTO books (book_id, title, author, published_year, isbn, genre, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getId());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getAuthor());
            stmt.setInt(4, book.getPublishedYear());
            stmt.setString(5, book.getIsbn());
            stmt.setString(6, book.getGenre().name());
            stmt.setString(7, book.getStatus().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 更新书
    public void updateBook(Book book) {
        String sql = "UPDATE books SET title=?, author=?, published_year=?, isbn=?, genre=?, status=? WHERE book_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setInt(3, book.getPublishedYear());
            stmt.setString(4, book.getIsbn());
            stmt.setString(5, book.getGenre().name());
            stmt.setString(6, book.getStatus().name());
            stmt.setString(7, book.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除书
    public void deleteBook(Book book) {
        String sql = "DELETE FROM books WHERE book_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询所有书
    public List<Book> getAllBooks() {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Book.Genre genre = Book.Genre.valueOf(rs.getString("genre"));
                Book.Status status = Book.Status.valueOf(rs.getString("status"));

                Book book = new Book(
                        rs.getString("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("published_year"),
                        rs.getString("isbn"),
                        genre,
                        status
                );
                list.add(book);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 借书（默认用户）
    public boolean borrowBook(String userId, String bookId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            // 检查是否可借
            String checkSql = "SELECT status FROM books WHERE book_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, bookId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next() || Book.Status.valueOf(rs.getString("status")) != Book.Status.AVAILABLE) {
                    return false;
                }
            }

            // 更新图书状态
            String updateSql = "UPDATE books SET status=? WHERE book_id=?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, Book.Status.CHECKED_OUT.name());
                updateStmt.setString(2, bookId);
                updateStmt.executeUpdate();
            }

            // 插入借阅记录
            String insertLoan = "INSERT INTO loans (user_id, book_id, borrow_date) VALUES (?, ?, ?)";
            try (PreparedStatement loanStmt = conn.prepareStatement(insertLoan)) {
                loanStmt.setString(1, userId);
                loanStmt.setString(2, bookId);
                loanStmt.setString(3, LocalDate.now().toString());
                loanStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 还书（默认用户）
    public boolean returnBook(String userId, String bookId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            // 检查借阅记录
            String checkLoan = "SELECT loan_id FROM loans WHERE user_id = ? AND book_id = ? AND return_date IS NULL";
            int loanId;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkLoan)) {
                checkStmt.setString(1, userId);
                checkStmt.setString(2, bookId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) return false;
                loanId = rs.getInt("loan_id");
            }

            // 更新借阅记录
            String updateLoan = "UPDATE loans SET return_date=? WHERE loan_id=?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateLoan)) {
                updateStmt.setString(1, LocalDate.now().toString());
                updateStmt.setInt(2, loanId);
                updateStmt.executeUpdate();
            }

            // 更新图书状态
            String updateBook = "UPDATE books SET status=? WHERE book_id=?";
            try (PreparedStatement bookStmt = conn.prepareStatement(updateBook)) {
                bookStmt.setString(1, Book.Status.AVAILABLE.name());
                bookStmt.setString(2, bookId);
                bookStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}