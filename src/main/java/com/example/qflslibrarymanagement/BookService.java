package com.example.qflslibrarymanagement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookService {
    private final BookRepository bookRepository;

    private final StudentRepository studentRepository;


    // 构造方法注入 Repository
    public BookService(BookRepository bookRepository,StudentRepository studentRepository) {
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
    }

    // 添加一本书
    public Book addBook(Book book) {
        try {
            Book savedBook = bookRepository.save(book);
            return savedBook;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Book updateBook(Book book) {
        return bookRepository.save(book);
    }

    // 获取所有书
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // 根据 ID 查书
    public Book getBookById(String id) {
        return bookRepository.findById(id).orElse(null);
    }

    // 删除书
    public void deleteBook(String id) {
        bookRepository.deleteById(id);
    }

    // 借书
    public boolean borrowBook(String studentId, String bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null && book.getStatus() == Book.Status.AVAILABLE) {
            book.setStatus(Book.Status.CHECKED_OUT);
            book.setBorrowTime(LocalDateTime.now());
            book.setBorrowerId(studentId);
            book.setReturnTime(null);
            Optional<Student> studentOptional = studentRepository.findById(studentId);
            studentOptional.ifPresent(student ->
                    book.setBorrowerName(student.getName()));
            bookRepository.save(book);
            return true;
        }
        return false;
    }

    // 还书
    public boolean returnBook(String studentId, String bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null && book.getStatus() == Book.Status.CHECKED_OUT) {
            book.setStatus(Book.Status.AVAILABLE);
            book.setReturnTime(LocalDateTime.now());
            bookRepository.save(book);

            // 检查是否超时
            if (book.getBorrowTime() != null) {
                LocalDateTime dueDate = book.getBorrowTime().plusWeeks(2);
                if (LocalDateTime.now().isAfter(dueDate)) {
                    System.out.println("书籍超时归还: " + book.getTitle());
                }
            }
            return true;
        }
        return false;
    }
    // 在BookService中添加
    public List<Book> getAllBooksWithRefresh() {
        // 强制Hibernate刷新缓存
        bookRepository.flush();
        return bookRepository.findAll();
    }

}