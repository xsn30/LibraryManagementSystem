package com.example.qflslibrarymanagement;

import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class LibraryRepository {

    private final BookRepository bookRepository;

    public LibraryRepository(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public void addBook(Book book) {
        bookRepository.save(book);
    }

    public void updateBook(Book book) {
        bookRepository.save(book); 
    }

    public void deleteBook(Book book) {
        bookRepository.deleteById(book.getId());
    }

    
    public boolean borrowBook(String userId, String bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null && book.getStatus() == Book.Status.AVAILABLE) {
            book.setStatus(Book.Status.CHECKED_OUT);
            bookRepository.save(book);
            return true;
        }
        return false;
    }

    public boolean returnBook(String userId, String bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null && book.getStatus() == Book.Status.CHECKED_OUT) {
            book.setStatus(Book.Status.AVAILABLE);
            bookRepository.save(book);
            return true;
        }
        return false;
    }
}