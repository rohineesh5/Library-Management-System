package model;

public class Book {

    // ─── Private Fields ───────────────────────────────────────────────────────
    private int bookId;
    private String title;
    private String author;
    private String category;
    private int quantity;
    private int available;

    // ─── Default Constructor ──────────────────────────────────────────────────
    public Book() {
    }

    // ─── Parameterized Constructor ────────────────────────────────────────────
    public Book(int bookId, String title, String author, String category, int quantity, int available) {
        this.bookId    = bookId;
        this.title     = title;
        this.author    = author;
        this.category  = category;
        this.quantity  = quantity;
        this.available = available;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────
    public int getBookId()       { return bookId; }
    public String getTitle()     { return title; }
    public String getAuthor()    { return author; }
    public String getCategory()  { return category; }
    public int getQuantity()     { return quantity; }
    public int getAvailable()    { return available; }

    // ─── Setters ──────────────────────────────────────────────────────────────
    public void setBookId(int bookId)          { this.bookId    = bookId; }
    public void setTitle(String title)         { this.title     = title; }
    public void setAuthor(String author)       { this.author    = author; }
    public void setCategory(String category)   { this.category  = category; }
    public void setQuantity(int quantity)      { this.quantity  = quantity; }
    public void setAvailable(int available)    { this.available = available; }

    // ─── toString() ───────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "Book [bookId=" + bookId + ", title=" + title + ", author=" + author
                + ", category=" + category + ", quantity=" + quantity + ", available=" + available + "]";
    }
}