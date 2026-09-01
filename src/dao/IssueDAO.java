package dao;

import model.IssueRecord;

/**
 * IssueDAO — Interface defining the contract for book issue operations.
 *
 * issueBook() is the core transactional operation:
 *   it must update TWO tables atomically — 'books' and 'issued_books'.
 *   Either BOTH operations succeed, or NEITHER is committed.
 *
 * returnBook() handles book returns with the same transactional guarantees.
 */
public interface IssueDAO {

    /**
     * Issues a book to a member.
     *
     * Internally performs two database operations inside a single transaction:
     *   1. Checks and reduces 'available' count in the 'books' table.
     *   2. Inserts a new row into the 'issued_books' table.
     *
     * @param record IssueRecord containing bookId, memberId, issueDate,
     *               dueDate, and status ("ISSUED").
     * @return true  if the book was successfully issued.
     *         false if no copies are available OR if any DB error occurred.
     */
    boolean issueBook(IssueRecord record);

    /**
     * Returns a previously issued book back to the library.
     *
     * Internally performs two database operations inside a single transaction:
     *   1. Checks the issued_books row — rejects if already "RETURNED".
     *   2. UPDATE issued_books: sets return_date = today, status = "RETURNED".
     *   3. UPDATE books: increments available count by 1.
     *
     * @param issueId The primary key of the issued_books row to close.
     * @return true  if the book was successfully returned.
     *         false if issueId is invalid, already returned, or a DB error occurred.
     */
    boolean returnBook(int issueId);
}
