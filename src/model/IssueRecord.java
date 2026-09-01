package model;

import java.sql.Date;

/**
 * Model class representing a Book Issue record.
 *
 * Maps directly to the 'issued_books' table in the database:
 *   issue_id | book_id | member_id | issue_date | due_date | return_date | status
 *
 * 'status' holds one of two values:
 *   "ISSUED"   → book is currently with the member
 *   "RETURNED" → book has been returned
 *
 * java.sql.Date is used (not java.util.Date) because JDBC's
 * PreparedStatement.setDate() and ResultSet.getDate() work
 * directly with java.sql.Date — no conversion needed.
 */
public class IssueRecord {

    // ─── Private Fields ───────────────────────────────────────────────────────

    /**
     * issue_id: Primary key, AUTO_INCREMENT — MySQL generates it.
     * We never set this manually when inserting a new issue.
     */
    private int issueId;

    /**
     * book_id: Foreign key referencing books.book_id.
     * Identifies which book was issued.
     */
    private int bookId;

    /**
     * member_id: Foreign key referencing members.member_id.
     * Identifies which member borrowed the book.
     */
    private int memberId;

    /**
     * issue_date: The calendar date on which the book was issued.
     * Set to the current date (CURDATE()) at the time of issuing.
     * Stored as java.sql.Date — compatible with MySQL DATE column.
     */
    private Date issueDate;

    /**
     * due_date: The deadline by which the book must be returned.
     * Typically calculated as issueDate + 14 days (library policy).
     * Stored as java.sql.Date.
     */
    private Date dueDate;

    /**
     * return_date: The actual date the book was returned.
     * NULL in the database until the member returns the book.
     * java.sql.Date allows null, representing an unreturned book.
     */
    private Date returnDate;

    /**
     * status: Current state of the issue record.
     * "ISSUED"   → book has not been returned yet
     * "RETURNED" → book has been returned
     */
    private String status;


    // ─── Default Constructor ──────────────────────────────────────────────────

    /** No-arg constructor — allows creating an empty IssueRecord and setting fields manually. */
    public IssueRecord() {
    }


    // ─── Parameterized Constructor (for INSERT) ───────────────────────────────

    /**
     * Used when issuing a new book.
     *
     * issueId   is excluded — AUTO_INCREMENT.
     * returnDate is excluded — NULL until the book is returned.
     * status    defaults to "ISSUED" at the time of issue.
     *
     * @param bookId    The ID of the book being issued
     * @param memberId  The ID of the member borrowing the book
     * @param issueDate Today's date (java.sql.Date.valueOf(LocalDate.now()))
     * @param dueDate   Return deadline (issueDate + 14 days typically)
     * @param status    "ISSUED" when creating a new record
     */
    public IssueRecord(int bookId, int memberId, Date issueDate, Date dueDate, String status) {
        this.bookId    = bookId;
        this.memberId  = memberId;
        this.issueDate = issueDate;
        this.dueDate   = dueDate;
        this.status    = status;
    }

    /**
     * Full constructor — used when fetching a complete record from the database.
     * Includes all columns (issue_id, return_date, status).
     */
    public IssueRecord(int issueId, int bookId, int memberId,
                       Date issueDate, Date dueDate, Date returnDate, String status) {
        this.issueId    = issueId;
        this.bookId     = bookId;
        this.memberId   = memberId;
        this.issueDate  = issueDate;
        this.dueDate    = dueDate;
        this.returnDate = returnDate;
        this.status     = status;
    }


    // ─── Getters ──────────────────────────────────────────────────────────────

    public int    getIssueId()    { return issueId; }
    public int    getBookId()     { return bookId; }
    public int    getMemberId()   { return memberId; }
    public Date   getIssueDate()  { return issueDate; }
    public Date   getDueDate()    { return dueDate; }
    public Date   getReturnDate() { return returnDate; }
    public String getStatus()     { return status; }


    // ─── Setters ──────────────────────────────────────────────────────────────

    public void setIssueId(int issueId)        { this.issueId    = issueId; }
    public void setBookId(int bookId)          { this.bookId     = bookId; }
    public void setMemberId(int memberId)      { this.memberId   = memberId; }
    public void setIssueDate(Date issueDate)   { this.issueDate  = issueDate; }
    public void setDueDate(Date dueDate)       { this.dueDate    = dueDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
    public void setStatus(String status)       { this.status     = status; }


    // ─── toString() ───────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "IssueRecord [issueId=" + issueId
                + ", bookId="     + bookId
                + ", memberId="   + memberId
                + ", issueDate="  + issueDate
                + ", dueDate="    + dueDate
                + ", returnDate=" + returnDate
                + ", status="     + status + "]";
    }
}
