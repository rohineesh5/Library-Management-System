package dao;

import model.Book;

import java.util.ArrayList;

/**
 * BookDAO — Interface defining the contract for all book-related DB operations.
 *
 * Any class that implements this interface MUST provide concrete implementations
 * for all methods listed here. This enforces consistency and allows easy
 * swapping of implementations (e.g., switching from MySQL to PostgreSQL).
 */
public interface BookDAO {

    // Phase 1
    void addBook(Book b);

    // Phase 2 — implemented now
    ArrayList<Book> viewAllBooks();
    Book searchBookById(int bookId);
    ArrayList<Book> searchBookByTitle(String title);

    // Phase 3 — implemented
    void updateBook(Book b);
    void deleteBook(int bookId);
}