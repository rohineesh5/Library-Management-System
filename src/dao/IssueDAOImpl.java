package dao;

import database.DBConnection;
import model.IssueRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * IssueDAOImpl — Concrete implementation of the IssueDAO interface.
 *
 * ═══════════════════════════════════════════════════════════════
 * TRANSACTION MANAGEMENT — CORE CONCEPT
 * ═══════════════════════════════════════════════════════════════
 *
 * By default, JDBC operates in "auto-commit" mode:
 *   every single SQL statement is committed to the database immediately
 *   the moment it executes — there is no way to undo it.
 *
 * For issueBook(), we need TWO queries to ALWAYS succeed or ALWAYS fail together:
 *   Query 1: UPDATE books  → reduce available count by 1
 *   Query 2: INSERT issued_books → record the issue
 *
 * Problem without a transaction:
 *   If Query 1 succeeds but Query 2 fails (e.g., network error, invalid member_id),
 *   the book's available count is already decremented — but no issue record exists.
 *   The database is now in an INCONSISTENT STATE. This is a data integrity bug.
 *
 * Solution — wrap both queries in a transaction:
 *   conn.setAutoCommit(false)  → disable auto-commit; changes are held in memory
 *   ... run Query 1 ...
 *   ... run Query 2 ...
 *   conn.commit()              → BOTH changes are written to disk permanently
 *
 *   If ANYTHING fails between setAutoCommit and commit:
 *   conn.rollback()            → BOTH changes are completely undone (as if neither ran)
 *
 * This guarantees ATOMICITY — a fundamental property of reliable databases (ACID).
 * ═══════════════════════════════════════════════════════════════
 */
public class IssueDAOImpl implements IssueDAO {

    // ─── SQL Constants ────────────────────────────────────────────────────────

    /**
     * Check how many copies of the book are currently available.
     *
     * SELECT available FROM books WHERE book_id = ?
     *
     * We fetch the 'available' column for the given book.
     * If available > 0, we can proceed to issue.
     * If available == 0, we reject the request immediately.
     */
    private static final String CHECK_AVAILABILITY_SQL =
            "SELECT available FROM books WHERE book_id = ?";

    /**
     * Reduce the available count by exactly 1 for the given book.
     *
     * UPDATE books SET available = available - 1 WHERE book_id = ?
     *
     * "available - 1" is done inside the DB (not in Java) to prevent
     * race conditions — if two users issue the same book simultaneously,
     * both would read available=1 in Java and both would try to decrement.
     * Letting the DB handle the math inside an atomic SQL statement
     * (with a transaction) avoids this concurrency bug.
     *
     * An additional guard WHERE available > 0 is omitted here because
     * we already checked availability in a prior query within the SAME
     * transaction, making it safe at this isolation level.
     */
    private static final String DECREMENT_AVAILABLE_SQL =
            "UPDATE books SET available = available - 1 WHERE book_id = ?";

    /**
     * Insert a new row into the 'issued_books' table.
     *
     * issue_id  → AUTO_INCREMENT, not included.
     * return_date → NULL until the book is returned, not included.
     *
     * ? placeholders (in order):
     *   1 → book_id
     *   2 → member_id
     *   3 → issue_date
     *   4 → due_date
     *   5 → status  ("ISSUED")
     */
    private static final String INSERT_ISSUE_SQL =
            "INSERT INTO issued_books (book_id, member_id, issue_date, due_date, status) " +
            "VALUES (?, ?, ?, ?, ?)";

    /**
     * Fetch the current status AND book_id for a given issue record.
     *
     * SELECT book_id, status FROM issued_books WHERE issue_id = ?
     *
     * We need both columns:
     *   status  → to detect an already-returned book (prevent double-return)
     *   book_id → to know which book's available count to increment on return
     */
    private static final String CHECK_ISSUE_STATUS_SQL =
            "SELECT book_id, status FROM issued_books WHERE issue_id = ?";

    /**
     * Mark the issue record as returned.
     *
     * SET return_date = ?  → records the actual return date (today)
     * SET status = 'RETURNED'  → closes the issue record
     * WHERE issue_id = ?  → targets exactly one row by primary key
     *
     * ? placeholders (in order):
     *   1 → return_date  (java.sql.Date — today's date)
     *   2 → issue_id     (WHERE clause)
     */
    private static final String UPDATE_ISSUE_RETURN_SQL =
            "UPDATE issued_books SET return_date = ?, status = 'RETURNED' WHERE issue_id = ?";

    /**
     * Increment the available count by 1 for the returned book.
     *
     * available + 1 is computed inside the DB (not in Java) —
     * same reason as decrement: avoids race conditions.
     *
     * WHERE book_id = ?  → updates only the specific book being returned.
     */
    private static final String INCREMENT_AVAILABLE_SQL =
            "UPDATE books SET available = available + 1 WHERE book_id = ?";


    // ─── issueBook() ─────────────────────────────────────────────────────────

    /**
     * Issues a book to a library member using a database transaction.
     *
     * FLOW:
     *   Step 1 — Open a single Connection (shared across all queries in this transaction).
     *   Step 2 — Disable auto-commit so no query is permanently saved yet.
     *   Step 3 — Check if the book has available copies.
     *            → If not, report and return false immediately (no DB changes made).
     *   Step 4 — UPDATE books: decrement available by 1.
     *   Step 5 — INSERT into issued_books: record the issue.
     *   Step 6 — COMMIT: both Step 4 and Step 5 are permanently saved together.
     *
     *   If ANY exception is thrown between Step 2 and Step 6:
     *     → ROLLBACK undoes Steps 4 and 5 completely.
     *     → Database returns to the state it was in before Step 2.
     *
     * @param record IssueRecord with bookId, memberId, issueDate, dueDate, status.
     * @return true if issue was successful; false otherwise.
     */
    @Override
    public boolean issueBook(IssueRecord record) {

        /*
         * ── IMPORTANT: Connection is NOT in try-with-resources here. ──
         *
         * We need to call conn.rollback() inside the catch block BEFORE the
         * connection closes. try-with-resources closes the connection the
         * moment the try block exits (including on exception), which would
         * happen before our catch block could call rollback().
         *
         * So we manage the Connection manually:
         *   open → setAutoCommit(false) → queries → commit or rollback → close
         */
        Connection conn = null;

        try {
            // Step 1 — Get a connection to the database
            conn = DBConnection.getConnection();

            /*
             * Step 2 — Disable auto-commit.
             *
             * From this point on, NO SQL statement will be permanently written
             * to disk until we explicitly call conn.commit().
             * All changes exist only in a temporary "transaction buffer" inside the DB.
             */
            conn.setAutoCommit(false);


            // ── Step 3: Check book availability ──────────────────────────────

            /*
             * We use try-with-resources for PreparedStatement and ResultSet
             * independently so they are still closed promptly.
             * The Connection itself remains open throughout the transaction.
             */
            try (PreparedStatement checkStmt = conn.prepareStatement(CHECK_AVAILABILITY_SQL)) {

                checkStmt.setInt(1, record.getBookId());

                try (ResultSet rs = checkStmt.executeQuery()) {

                    if (rs.next()) {
                        int available = rs.getInt("available");

                        /*
                         * If no copies are available, we reject the request HERE —
                         * before any modification is made to the database.
                         *
                         * We must restore auto-commit before returning, because this
                         * Connection object may be reused (connection pooling scenarios).
                         */
                        if (available <= 0) {
                            System.out.println("⚠️  Book not available. No copies left for Book ID: "
                                    + record.getBookId());
                            conn.setAutoCommit(true);  // restore default before returning
                            return false;
                        }

                    } else {
                        // book_id doesn't exist in the books table at all
                        System.out.println("⚠️  Book ID not found: " + record.getBookId());
                        conn.setAutoCommit(true);
                        return false;
                    }
                }
            }


            // ── Step 4: Decrement available count ────────────────────────────

            try (PreparedStatement decrementStmt = conn.prepareStatement(DECREMENT_AVAILABLE_SQL)) {

                decrementStmt.setInt(1, record.getBookId());

                /*
                 * executeUpdate() submits the UPDATE to the transaction buffer.
                 * It is NOT yet written to disk — it will only be committed in Step 6.
                 */
                decrementStmt.executeUpdate();
            }


            // ── Step 5: Insert the issue record ──────────────────────────────

            try (PreparedStatement insertStmt = conn.prepareStatement(INSERT_ISSUE_SQL)) {

                /*
                 * Bind all five parameters in the order they appear in INSERT_ISSUE_SQL:
                 *   1 → book_id    (INT)
                 *   2 → member_id  (INT)
                 *   3 → issue_date (java.sql.Date)
                 *   4 → due_date   (java.sql.Date)
                 *   5 → status     (VARCHAR — "ISSUED")
                 *
                 * setDate() accepts java.sql.Date directly — no conversion needed.
                 * This is why IssueRecord uses java.sql.Date instead of java.util.Date.
                 */
                insertStmt.setInt(1, record.getBookId());
                insertStmt.setInt(2, record.getMemberId());
                insertStmt.setDate(3, record.getIssueDate());
                insertStmt.setDate(4, record.getDueDate());
                insertStmt.setString(5, record.getStatus());

                insertStmt.executeUpdate();
            }


            // ── Step 6: COMMIT ────────────────────────────────────────────────

            /*
             * commit() tells the database engine:
             *   "Write BOTH the UPDATE (Step 4) and the INSERT (Step 5)
             *    permanently to disk right now."
             *
             * Only when commit() completes successfully are the changes visible
             * to other database connections and guaranteed to survive a crash.
             */
            conn.commit();

            System.out.println("✅ Book issued successfully!"
                    + " Book ID: " + record.getBookId()
                    + " | Member ID: " + record.getMemberId()
                    + " | Due: " + record.getDueDate());

            return true;

        } catch (SQLException e) {

            /*
             * ── ROLLBACK ─────────────────────────────────────────────────────
             *
             * Any SQLException between setAutoCommit(false) and commit()
             * lands here. We MUST rollback to undo any partial changes.
             *
             * Without rollback():
             *   - If Step 4 ran but Step 5 threw an exception, the 'available'
             *     count would be decremented with no corresponding issue record.
             *   - This is a silent data corruption bug that is very hard to detect.
             *
             * With rollback():
             *   - The DB is restored to the exact state it was in before Step 2.
             *   - Both queries are undone — as if neither ever ran.
             *   - Data integrity is preserved.
             */
            System.out.println("❌ Transaction failed. Rolling back... Error: " + e.getMessage());

            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("↩️  Rollback successful. No changes were made.");
                } catch (SQLException rollbackEx) {
                    // Rollback itself failed — this is a critical failure
                    System.out.println("🚨 Rollback also failed: " + rollbackEx.getMessage());
                    rollbackEx.printStackTrace();
                }
            }

            e.printStackTrace();
            return false;

        } finally {

            /*
             * ── FINALLY BLOCK ─────────────────────────────────────────────────
             *
             * finally always runs — whether the try succeeded, threw an exception,
             * or even hit a return statement.
             *
             * We restore auto-commit to true and close the Connection here.
             * This is critical in applications that use connection pools:
             * returning a connection with auto-commit=false to the pool would
             * cause every future user of that connection to also have transactions
             * disabled — a very subtle and dangerous bug.
             */
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);  // always restore before releasing
                    conn.close();
                } catch (SQLException closeEx) {
                    System.out.println("⚠️  Failed to close connection: " + closeEx.getMessage());
                    closeEx.printStackTrace();
                }
            }
        }
    }


    // ─── returnBook() ──────────────────────────────────────────────────────

    /**
     * Returns a previously issued book back to the library.
     *
     * FLOW:
     *   Step 1 — Open a shared Connection; disable auto-commit.
     *   Step 2 — Fetch the issue record: check it exists AND is still "ISSUED".
     *            → If issue_id doesn't exist  → reject (invalid ID).
     *            → If status == "RETURNED"   → reject (prevent double-return).
     *   Step 3 — UPDATE issued_books: set return_date = today, status = "RETURNED".
     *   Step 4 — UPDATE books: increment available by 1 using the book_id from Step 2.
     *   Step 5 — COMMIT: both Step 3 and Step 4 are saved permanently together.
     *
     *   If ANY exception occurs between Step 1 and Step 5:
     *     → ROLLBACK — neither update is saved; DB stays consistent.
     *
     * @param issueId The primary key of the issued_books row to close.
     * @return true if return was successful; false otherwise.
     */
    @Override
    public boolean returnBook(int issueId) {

        Connection conn = null;

        try {
            // Step 1 — Open connection and disable auto-commit
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Will be populated in Step 2; needed later in Step 4
            int bookId;


            // ── Step 2: Fetch issue record and validate status ─────────────────

            try (PreparedStatement checkStmt = conn.prepareStatement(CHECK_ISSUE_STATUS_SQL)) {

                checkStmt.setInt(1, issueId);

                try (ResultSet rs = checkStmt.executeQuery()) {

                    if (!rs.next()) {
                        /*
                         * No row found — the issueId doesn't exist in issued_books.
                         * This happens if the caller passes a wrong or non-existent ID.
                         * No DB changes have been made, so no rollback is needed.
                         */
                        System.out.println("⚠️  No issue record found with ID: " + issueId);
                        conn.setAutoCommit(true);
                        return false;
                    }

                    String currentStatus = rs.getString("status");

                    /*
                     * Prevent double-return:
                     *
                     * If the status is already "RETURNED", it means this issue
                     * record was already closed in a previous transaction.
                     * Returning it again would:
                     *   a) Leave return_date and status unchanged (no harm to issued_books)
                     *   b) BUT increment available in books AGAIN — corrupting the count!
                     *
                     * We catch this early and reject with a clear message.
                     */
                    if ("RETURNED".equalsIgnoreCase(currentStatus)) {
                        System.out.println("⚠️  Book already returned. Issue ID: " + issueId
                                + " has status: " + currentStatus);
                        conn.setAutoCommit(true);
                        return false;
                    }

                    // Extract book_id so we know which book to increment in Step 4
                    bookId = rs.getInt("book_id");
                }
            }


            // ── Step 3: Update the issue record ────────────────────────────────

            try (PreparedStatement updateIssueStmt = conn.prepareStatement(UPDATE_ISSUE_RETURN_SQL)) {

                /*
                 * java.sql.Date.valueOf(LocalDate.now()) gives today's date
                 * as a java.sql.Date object suitable for JDBC setDate().
                 *
                 * ? positions:
                 *   1 → return_date (today)
                 *   2 → issue_id    (which row to update)
                 */
                updateIssueStmt.setDate(1, java.sql.Date.valueOf(java.time.LocalDate.now()));
                updateIssueStmt.setInt(2, issueId);

                /*
                 * This UPDATE is buffered inside the transaction.
                 * It will ONLY be written to disk when commit() is called in Step 5.
                 * If Step 4 fails, rollback() will undo this UPDATE too.
                 */
                updateIssueStmt.executeUpdate();
            }


            // ── Step 4: Increment available count in books ──────────────────────

            try (PreparedStatement incrementStmt = conn.prepareStatement(INCREMENT_AVAILABLE_SQL)) {

                /*
                 * We use the bookId extracted in Step 2 — NOT from user input.
                 * This is critical: the book being returned is the one linked to
                 * the issue record, not whatever book_id a user might type in.
                 * This prevents a user from exploiting the return to inflate
                 * the available count of any arbitrary book.
                 */
                incrementStmt.setInt(1, bookId);

                incrementStmt.executeUpdate();
            }


            // ── Step 5: COMMIT ────────────────────────────────────────────────

            /*
             * Both Step 3 (UPDATE issued_books) and Step 4 (UPDATE books)
             * are written to disk simultaneously right now.
             * Neither was visible to any other DB connection before this line.
             */
            conn.commit();

            System.out.println("✅ Book returned successfully!"
                    + " Issue ID: " + issueId
                    + " | Book ID: " + bookId
                    + " | Return Date: " + java.time.LocalDate.now());

            return true;

        } catch (SQLException e) {

            /*
             * ROLLBACK:
             *   Without it: issued_books may be marked RETURNED but books.available
             *   is NOT incremented (or vice versa) — the library loses track of a copy.
             *
             *   With rollback: both updates are undone — the book stays "ISSUED"
             *   and available count is unchanged. The caller can retry safely.
             */
            System.out.println("❌ Return transaction failed. Rolling back... Error: " + e.getMessage());

            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("↩️  Rollback successful. No changes were made.");
                } catch (SQLException rollbackEx) {
                    System.out.println("🚨 Rollback also failed: " + rollbackEx.getMessage());
                    rollbackEx.printStackTrace();
                }
            }

            e.printStackTrace();
            return false;

        } finally {

            // Always restore auto-commit and close the connection
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    System.out.println("⚠️  Failed to close connection: " + closeEx.getMessage());
                    closeEx.printStackTrace();
                }
            }
        }
    }
}
