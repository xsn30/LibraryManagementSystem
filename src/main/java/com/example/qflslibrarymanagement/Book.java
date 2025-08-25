package com.example.qflslibrarymanagement;


public class Book {
    private String id;
    private String title;
    private String author;
    private int publishedYear;
    private String isbn;
    private Genre genre;
    private Status status = Status.AVAILABLE;
    public enum Genre {
        FICTION, NON_FICTION, SCIENCE, FANTASY, MYSTERY, BIOGRAPHY
    }

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
}
