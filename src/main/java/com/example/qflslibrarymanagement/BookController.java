package com.example.qflslibrarymanagement;

import java.util.List;
import java.util.function.Consumer;

public class BookController {
    private final LibraryRepository repository = new LibraryRepository();

    public void getAllBooks(String author, Book.Genre genre, Consumer<List<Book>> callback) {
        List<Book> allBooks = repository.getAllBooks();
        List<Book> filtered = allBooks.stream()
                .filter(b -> (author == null || author.isEmpty() || b.getAuthor().contains(author)) &&
                        (genre == null || b.getGenre() == genre))
                .toList();
        callback.accept(filtered);
    }

    public void addBook(Book book, Consumer<List<Book>> callback) {
        repository.addBook(book);
        getAllBooks(null, null, callback);
    }

    public void updateBook(Book book, Consumer<List<Book>> callback) {
        repository.updateBook(book);
        getAllBooks(null, null, callback);
    }

    public void deleteBook(Book book, Consumer<List<Book>> callback) {
        repository.deleteBook(book);
        getAllBooks(null, null, callback);
    }

    public void checkoutBook(Book book, Consumer<List<Book>> callback) {
        boolean success = repository.borrowBook("defaultUser", book.getId());
        if (success) book.setStatus(Book.Status.CHECKED_OUT);
        getAllBooks(null, null, callback);
    }

    public void returnBook(Book book, Consumer<List<Book>> callback) {
        boolean success = repository.returnBook("defaultUser", book.getId());
        if (success) book.setStatus(Book.Status.AVAILABLE);
        getAllBooks(null, null, callback);
    }

    public void checkoutByBarcode(String barcode, Consumer<Book> callback) {
        List<Book> allBooks = repository.getAllBooks();
        for (Book b : allBooks) {
            if (b.getIsbn().equals(barcode)) {
                checkoutBook(b, books -> callback.accept(b));
                return;
            }
        }
        callback.accept(null);
    }

    public void getBookByBarcode(String barcode, Consumer<Book> callback) {
        List<Book> allBooks = repository.getAllBooks();
        for (Book b : allBooks) {
            if (b.getIsbn().equals(barcode)) {
                callback.accept(b);
                return;
            }
        }
        callback.accept(null);
    }
}