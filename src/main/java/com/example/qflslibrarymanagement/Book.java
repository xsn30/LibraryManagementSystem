package com.example.qflslibrarymanagement;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity                       // 告诉 JPA 这是一个实体类
@Table(name = "books")        // 指定数据库表名为 books
public class Book {
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(name = "published_year")
    private int publishedYear;

    @Column(unique = true)
    private String isbn;

    @Enumerated(EnumType.STRING)   // 枚举存字符串
    private Genre genre;

    @Column(name = "borrow_time")
    private LocalDateTime borrowTime;

    @Column(name = "return_time")
    private LocalDateTime returnTime;

    @Column(name = "borrower_id")
    private String borrowerId; // 借书人ID

    @Enumerated(EnumType.STRING)
    private Status status = Status.AVAILABLE;
    public enum Genre {
        FICTION, NON_FICTION, SCIENCE, FANTASY, MYSTERY, BIOGRAPHY
    }

    @Column(name = "borrower_name")
    private String borrowerName;

    public enum Status {
        AVAILABLE, CHECKED_OUT
    }
    public Book() {
    }

    public Book(String id, String title, String author, int publishedYear,String isbn,Genre genre, Status status) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.publishedYear = publishedYear;
        this.isbn = isbn;
        this.genre = genre;
        this.status = status;
        this.borrowTime = null;
        this.returnTime = null;
        this.borrowerId = null;
        this.borrowerName = null;
    }
    public Book(String id, String title, String author, int publishedYear, String isbn,
                Genre genre, Status status, LocalDateTime borrowTime, LocalDateTime returnTime,
                String borrowerId, String borrowerName) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.publishedYear = publishedYear;
        this.isbn = isbn;
        this.genre = genre;
        this.status = status;
        this.borrowTime = borrowTime;
        this.returnTime = returnTime;
        this.borrowerId = borrowerId;
        this.borrowerName = borrowerName;
    }

    // Getter 和 Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getPublishedYear() { return publishedYear; }
    public void setPublishedYear(int publishedYear) { this.publishedYear = publishedYear; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getBorrowTime() { return borrowTime; }
    public void setBorrowTime(LocalDateTime borrowTime) { this.borrowTime = borrowTime; }

    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }

    public String getBorrowerId() { return borrowerId; }
    public void setBorrowerId(String borrowerId) { this.borrowerId = borrowerId; }

    public boolean isOverdue() {
        if (borrowTime == null || status == Status.AVAILABLE) {
            return false;
        }
        return LocalDateTime.now().isAfter(borrowTime.plusWeeks(2));
    }

    // 计算剩余天数的方法
    public long getRemainingDays() {
        if (borrowTime == null || status == Status.AVAILABLE) {
            return 0;
        }
        LocalDateTime dueDate = borrowTime.plusWeeks(2);
        return ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
    }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
}
