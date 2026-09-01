package dao;

import database.DBConnection;
import model.Book;
import util.Validator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * BookDAOImpl — Concrete implementation of the BookDAO interface.
 *
 * Phase 1: addBook()
 * Phase 2: viewAllBooks(), searchBookById(), searchBookByTitle()
 * Phase 3: updateBook(), deleteBook()
 */
public class BookDAOImpl implements BookDAO {

    // ─── SQL Constants ────────────────────────────────────────────────────────

    /** Phase 1 — INSERT a new book row into the books table. */
    private static final String INSERT_BOOK_SQL =
            "INSERT INTO books (title, author, category, quantity, available) VALUES (?, ?, ?, ?, ?)";

    /**
     * Phase 2 — SELECT all rows from the books table.
     *
     * SELECT *  → retrieves every column: book_id, title, author, category, quantity, available
     * FROM books → the target table
     *
     * No WHERE clause → returns every single book in the library.
     * No PreparedStatement needed here because there are no user inputs —
     * using a plain Statement would also work, but PreparedStatement is used
     * for consistency and to keep the door open for future filtering.
     */
    private static final String SELECT_ALL_BOOKS_SQL =
            "SELECT * FROM books";

    /**
     * Phase 2 — SELECT a single book by its primary key (book_id).
     *
     * WHERE book_id = ?  → filters to exactly one row (book_id is the PRIMARY KEY,
     *                       so at most one row will ever match)
     *
     * PreparedStatement is required here because book_id comes from user input.
     */
    private static final String SELECT_BOOK_BY_ID_SQL =
            "SELECT * FROM books WHERE book_id = ?";

    /**
     * Phase 2 — SELECT all books whose title contains the search keyword.
     *
     * LIKE '%?%' pattern:
     *   %  → wildcard that matches any sequence of characters
     *   So  LIKE '%java%'  matches "Java Programming", "Core Java", "Java 17"
     *
     * IMPORTANT: You cannot put '%' directly around '?' in a PreparedStatement.
     * Instead, we wrap the user's input in Java before binding:
     *   pstmt.setString(1, "%" + keyword + "%")
     *
     * This keeps SQL Injection protection intact while enabling partial matching.
     */
    private static final String SELECT_BOOKS_BY_TITLE_SQL =
            "SELECT * FROM books WHERE title LIKE ?";

    /**
     * Phase 3 — UPDATE all editable columns for the book matching book_id.
     *
     * SET clause lists every column that can be changed by a librarian:
     *   title, author, category, quantity, available
     *
     * WHERE book_id = ?  → restricts the update to exactly one row.
     *   Without WHERE, EVERY row in the table would be overwritten!
     *
     * Parameter order (matches the ? placeholders left-to-right):
     *   1 → title
     *   2 → author
     *   3 → category
     *   4 → quantity
     *   5 → available
     *   6 → book_id  (in the WHERE clause — always last)
     */
    private static final String UPDATE_BOOK_SQL =
            "UPDATE books SET title = ?, author = ?, category = ?, quantity = ?, available = ? WHERE book_id = ?";

    /**
     * Phase 3 — DELETE the row whose book_id matches the given value.
     *
     * WHERE book_id = ?  → targets only one specific row.
     *   Without WHERE, DELETE FROM books would erase the entire table!
     *
     * executeUpdate() returns the number of rows deleted:
     *   1 → the book was found and deleted successfully
     *   0 → no row matched the ID (invalid / already deleted)
     */
    private static final String DELETE_BOOK_SQL =
            "DELETE FROM books WHERE book_id = ?";


    // ─── Phase 1: addBook() ───────────────────────────────────────────────────

    /**
     * Inserts a new book record into the 'books' table.
     * book_id is AUTO_INCREMENT — MySQL generates it automatically.
     *
     * @param b Book object to insert (bookId field is ignored)
     */
    @Override
    public void addBook(Book b) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_BOOK_SQL)) {

            // Bind each field to its corresponding '?' placeholder (1-indexed)
            pstmt.setString(1, b.getTitle());
            pstmt.setString(2, b.getAuthor());
            pstmt.setString(3, b.getCategory());
            pstmt.setInt(4, b.getQuantity());
            pstmt.setInt(5, b.getAvailable());

            // executeUpdate() runs INSERT/UPDATE/DELETE and returns affected row count
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Book added successfully: " + b.getTitle());
            } else {
                System.out.println("⚠️  Book was not added. No rows affected.");
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "adding book");
            e.printStackTrace();
        }
    }


    // ─── Phase 2: viewAllBooks() ──────────────────────────────────────────────

    /**
     * Retrieves every book stored in the 'books' table.
     *
     * ResultSet — a cursor that points to one row at a time from the query result.
     * rs.next() advances the cursor to the next row and returns:
     *   → true  if a row exists at the new position
     *   → false if there are no more rows (end of result)
     *
     * We include ResultSet in try-with-resources so it is also closed
     * automatically, freeing database resources promptly.
     *
     * @return ArrayList<Book> — all books; empty list if the table is empty
     */
    @Override
    public ArrayList<Book> viewAllBooks() {

        // Start with an empty list; we'll fill it row by row from the ResultSet
        ArrayList<Book> books = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_BOOKS_SQL);

             /*
              * executeQuery() is used for SELECT statements.
              * It returns a ResultSet — unlike executeUpdate() which returns an int.
              * The ResultSet is also declared here so try-with-resources closes it.
              */
             ResultSet rs = pstmt.executeQuery()) {

            /*
             * rs.next() moves the cursor forward one row.
             * The loop continues until there are no more rows in the result.
             */
            while (rs.next()) {

                /*
                 * Extract each column value from the current row.
                 *
                 * rs.getInt("book_id")     → reads the 'book_id' column as int
                 * rs.getString("title")    → reads the 'title'   column as String
                 *
                 * Using column NAMES (not indexes) is safer — if column order
                 * changes in the DB, the code still works correctly.
                 */
                int    bookId    = rs.getInt("book_id");
                String title     = rs.getString("title");
                String author    = rs.getString("author");
                String category  = rs.getString("category");
                int    quantity  = rs.getInt("quantity");
                int    available = rs.getInt("available");

                // Construct a Book object from the row data and add to the list
                books.add(new Book(bookId, title, author, category, quantity, available));
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "fetching books");
            e.printStackTrace();
        }

        // Return the populated list (or empty list if no books exist or error occurred)
        return books;
    }


    // ─── Phase 2: searchBookById() ────────────────────────────────────────────

    /**
     * Finds and returns a single book by its primary key (book_id).
     *
     * Since book_id is a PRIMARY KEY, at most ONE row can match.
     * We use rs.next() once — if it returns true, a book was found.
     * If false, no book with that ID exists and we return null.
     *
     * @param bookId The primary key of the book to find
     * @return The matching Book object, or null if not found
     */
    @Override
    public Book searchBookById(int bookId) {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BOOK_BY_ID_SQL)) {

            /*
             * Bind the user-provided bookId to the '?' placeholder.
             * This is critical — never concatenate user input directly into SQL.
             */
            pstmt.setInt(1, bookId);

            /*
             * executeQuery() returns a ResultSet.
             * We open it inside a nested try-with-resources so it is
             * closed as soon as this inner block exits.
             */
            try (ResultSet rs = pstmt.executeQuery()) {

                /*
                 * rs.next() is called once.
                 * Because book_id is a PRIMARY KEY, we either get exactly 1 row
                 * (true) or 0 rows (false — ID doesn't exist in the table).
                 */
                if (rs.next()) {
                    // Row found — extract all columns and build the Book object
                    return new Book(
                            rs.getInt("book_id"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getString("category"),
                            rs.getInt("quantity"),
                            rs.getInt("available")
                    );
                }
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "searching book by ID");
            e.printStackTrace();
        }

        /*
         * Reaching here means either:
         *   a) No book found with the given ID
         *   b) A SQL exception occurred
         * In both cases, return null — the caller should check for null.
         */
        return null;
    }


    // ─── Phase 2: searchBookByTitle() ────────────────────────────────────────

    /**
     * Searches for all books whose title contains the given keyword.
     * Case-insensitive partial match (e.g., "java" matches "Core Java Handbook").
     *
     * Multiple books can share a similar title, so we return an ArrayList,
     * not a single Book object.
     *
     * @param title The keyword to search for (partial match supported)
     * @return ArrayList<Book> of all matching books; empty if none found
     */
    @Override
    public ArrayList<Book> searchBookByTitle(String title) {

        ArrayList<Book> books = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BOOKS_BY_TITLE_SQL)) {

            /*
             * Wrap the keyword with '%' wildcards BEFORE binding it.
             *
             * We cannot write:  pstmt.setString(1, "%" + ? + "%")  — that's invalid.
             * Instead, we build the pattern string in Java first:
             *   "%" + title + "%" → "%java%"
             * Then bind that complete pattern string to the placeholder.
             *
             * MySQL's LIKE is case-insensitive for VARCHAR columns by default
             * (depends on the column collation — utf8mb4_general_ci is insensitive).
             */
            pstmt.setString(1, "%" + title + "%");

            try (ResultSet rs = pstmt.executeQuery()) {

                // Iterate through all matching rows
                while (rs.next()) {
                    books.add(new Book(
                            rs.getInt("book_id"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getString("category"),
                            rs.getInt("quantity"),
                            rs.getInt("available")
                    ));
                }
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "searching book by title");
            e.printStackTrace();
        }

        return books;
    }


    // ─── Phase 3: updateBook() ───────────────────────────────────────────────

    /**
     * Updates all editable fields of an existing book in the 'books' table.
     *
     * The book to update is identified by its book_id (primary key).
     * If no row matches the given book_id, the update silently affects 0 rows
     * and we report it as an invalid/not-found ID.
     *
     * @param b Book object containing the updated values AND the book_id to target.
     *          All fields (title, author, category, quantity, available) are updated.
     */
    @Override
    public void updateBook(Book b) {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_BOOK_SQL)) {

            /*
             * Bind values in the exact order they appear in the SQL SET clause.
             *
             * Positions 1–5 fill the SET columns:
             *   1 → title     (VARCHAR)
             *   2 → author    (VARCHAR)
             *   3 → category  (VARCHAR)
             *   4 → quantity  (INT)
             *   5 → available (INT)
             *
             * Position 6 fills the WHERE clause:
             *   6 → book_id   (INT) — must come LAST, matching the final '?'
             *
             * Getting the order wrong here would update the wrong columns
             * or cause a type mismatch error — always cross-check with the SQL.
             */
            pstmt.setString(1, b.getTitle());
            pstmt.setString(2, b.getAuthor());
            pstmt.setString(3, b.getCategory());
            pstmt.setInt(4, b.getQuantity());
            pstmt.setInt(5, b.getAvailable());
            pstmt.setInt(6, b.getBookId());   // WHERE book_id = ?

            /*
             * executeUpdate() executes the UPDATE statement.
             * Returns the number of rows that were actually changed.
             *
             *   rowsAffected == 1  → the book_id existed and was updated ✅
             *   rowsAffected == 0  → no row matched — invalid or non-existent ID ⚠️
             */
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Book updated successfully. ID: " + b.getBookId());
            } else {
                // Graceful handling — inform the caller without throwing an exception
                System.out.println("⚠️  No book found with ID: " + b.getBookId() + ". Nothing was updated.");
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "updating book");
            e.printStackTrace();
        }
    }


    // ─── Phase 3: deleteBook() ───────────────────────────────────────────────

    /**
     * Deletes a book record from the 'books' table using its primary key.
     *
     * If the book_id does not exist in the table (invalid ID, already deleted,
     * or wrong input), executeUpdate() returns 0 and we report gracefully
     * instead of throwing an exception.
     *
     * @param bookId The primary key (book_id) of the book to remove.
     */
    @Override
    public void deleteBook(int bookId) {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_BOOK_SQL)) {

            /*
             * Bind the user-provided bookId to the '?' in WHERE book_id = ?
             *
             * This is the ONLY parameter — position 1.
             * Using PreparedStatement prevents a user from injecting SQL like:
             *   bookId = "1 OR 1=1"  which would delete every row.
             */
            pstmt.setInt(1, bookId);

            /*
             * executeUpdate() runs the DELETE statement.
             *
             *   rowsAffected == 1  → book was found and deleted ✅
             *   rowsAffected == 0  → no book matched the ID ⚠️
             *                        (invalid ID, or book already deleted)
             */
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Book deleted successfully. ID: " + bookId);
            } else {
                // Graceful handling — no crash, clear feedback to the user
                System.out.println("⚠️  No book found with ID: " + bookId + ". Nothing was deleted.");
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "deleting book");
            e.printStackTrace();
        }
    }
}
