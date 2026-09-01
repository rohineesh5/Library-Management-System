import dao.BookDAO;
import dao.BookDAOImpl;
import dao.IssueDAO;
import dao.IssueDAOImpl;
import dao.MemberDAO;
import dao.MemberDAOImpl;
import model.Book;
import model.IssueRecord;
import model.Member;
import util.FineCalculator;
import util.Validator;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Main — Entry point and console UI for the Library Management System.
 *
 * Architecture overview:
 *   Main.java  ──calls──▶  DAO layer  ──calls──▶  DBConnection  ──▶  MySQL
 *                             │
 *                             └──uses──▶  model objects (Book, Member, IssueRecord)
 *                                         util objects  (FineCalculator, Validator)
 *
 * The menu runs in an infinite while loop.
 * Typing '11' breaks the loop and exits the program.
 */
public class Main {

    // ─── DAO instances ────────────────────────────────────────────────────────

    /*
     * We create ONE instance of each DAO and reuse it throughout the session.
     * Each DAO opens its own Connection per operation — no shared state issues.
     */
    private static final BookDAO   bookDAO   = new BookDAOImpl();
    private static final MemberDAO memberDAO = new MemberDAOImpl();
    private static final IssueDAO  issueDAO  = new IssueDAOImpl();

    // ─── Scanner ──────────────────────────────────────────────────────────────

    /*
     * One Scanner for the entire program.
     * System.in is opened once; closing it mid-program would prevent
     * further input reads — so we keep it open until the program exits.
     */
    private static final Scanner sc = new Scanner(System.in);


    // ─── main() ───────────────────────────────────────────────────────────────

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║    LIBRARY MANAGEMENT SYSTEM         ║");
        System.out.println("╚══════════════════════════════════════╝");

        /*
         * The main loop runs indefinitely.
         * Only option 11 (Exit) calls break to exit the loop.
         * Any invalid input is caught and the menu is shown again.
         */
        while (true) {

            printMenu();

            int choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1  -> addBook();
                case 2  -> viewBooks();
                case 3  -> searchBook();
                case 4  -> updateBook();
                case 5  -> deleteBook();
                case 6  -> addMember();
                case 7  -> viewMembers();
                case 8  -> issueBook();
                case 9  -> returnBook();
                case 10 -> fineDetails();
                case 11 -> {
                    System.out.println("\n👋 Thank you for using the Library System. Goodbye!");
                    sc.close();
                    return;   // exits main() — terminates the program cleanly
                }
                default -> System.out.println("\n⚠️  Invalid choice. Please enter a number between 1 and 11.\n");
            }
        }
    }


    // ─── Menu Printer ─────────────────────────────────────────────────────────

    /** Prints the main menu to the console. Extracted to keep main() clean. */
    private static void printMenu() {
        System.out.println();
        System.out.println("┌──────────────────────────────────────┐");
        System.out.println("│           MAIN MENU                  │");
        System.out.println("├──────────────────────────────────────┤");
        System.out.println("│  BOOK OPERATIONS                     │");
        System.out.println("│   1.  Add Book                       │");
        System.out.println("│   2.  View Books                     │");
        System.out.println("│   3.  Search Book                    │");
        System.out.println("│   4.  Update Book                    │");
        System.out.println("│   5.  Delete Book                    │");
        System.out.println("├──────────────────────────────────────┤");
        System.out.println("│  MEMBER OPERATIONS                   │");
        System.out.println("│   6.  Add Member                     │");
        System.out.println("│   7.  View Members                   │");
        System.out.println("├──────────────────────────────────────┤");
        System.out.println("│  ISSUE / RETURN                      │");
        System.out.println("│   8.  Issue Book                     │");
        System.out.println("│   9.  Return Book                    │");
        System.out.println("│  10.  Fine Details                   │");
        System.out.println("├──────────────────────────────────────┤");
        System.out.println("│  11.  Exit                           │");
        System.out.println("└──────────────────────────────────────┘");
    }


    // ═══════════════════════════════════════════════════════════════════════════
    // BOOK OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Option 1 — Collects book details, validates input, and calls addBook(). */
    private static void addBook() {
        System.out.println("\n── ADD BOOK ──────────────────────────");

        System.out.print("  Title     : ");
        String title = sc.nextLine().trim();

        System.out.print("  Author    : ");
        String author = sc.nextLine().trim();

        System.out.print("  Category  : ");
        String category = sc.nextLine().trim();

        int quantity  = readInt("  Quantity  : ");
        int available = readInt("  Available : ");

        // Validate all inputs using Validator utility before attempting DB insert
        if (!Validator.validateBook(title, author, category, quantity, available)) {
            System.out.println("  ⚠️  Book creation cancelled due to validation errors.");
            return;
        }

        /*
         * bookId = 0 here — MySQL AUTO_INCREMENT ignores it on INSERT.
         * The DB will assign the real ID automatically.
         */
        Book book = new Book(0, title, author, category, quantity, available);
        bookDAO.addBook(book);
    }

    /** Option 2 — Fetches and prints all books in a formatted table. */
    private static void viewBooks() {
        System.out.println("\n── ALL BOOKS ─────────────────────────");

        ArrayList<Book> books = bookDAO.viewAllBooks();

        if (books.isEmpty()) {
            System.out.println("  No books found in the library.");
            return;
        }

        // Header row
        System.out.printf("  %-5s %-25s %-20s %-15s %-8s %-9s%n",
                "ID", "Title", "Author", "Category", "Qty", "Available");
        System.out.println("  " + "─".repeat(87));

        // One row per book
        for (Book b : books) {
            System.out.printf("  %-5d %-25s %-20s %-15s %-8d %-9d%n",
                    b.getBookId(),
                    truncate(b.getTitle(), 24),
                    truncate(b.getAuthor(), 19),
                    truncate(b.getCategory(), 14),
                    b.getQuantity(),
                    b.getAvailable());
        }
    }

    /** Option 3 — Sub-menu: search by ID or by title. */
    private static void searchBook() {
        System.out.println("\n── SEARCH BOOK ───────────────────────");
        System.out.println("  1. Search by ID");
        System.out.println("  2. Search by Title");

        int subChoice = readInt("  Choose: ");

        switch (subChoice) {

            case 1 -> {
                int bookId = readInt("  Enter Book ID: ");
                if (bookId <= 0) {
                    System.out.println("  ⚠️  Invalid Book ID.");
                    return;
                }
                Book b = bookDAO.searchBookById(bookId);

                if (b != null) {
                    System.out.println("\n  Found: " + b);
                } else {
                    System.out.println("\n  ⚠️  No book found with ID: " + bookId);
                }
            }

            case 2 -> {
                System.out.print("  Enter Title keyword: ");
                String keyword = sc.nextLine().trim();

                if (!Validator.isNotEmpty(keyword, "Search Keyword")) {
                    return;
                }

                ArrayList<Book> results = bookDAO.searchBookByTitle(keyword);

                if (results.isEmpty()) {
                    System.out.println("\n  ⚠️  No books found matching: \"" + keyword + "\"");
                } else {
                    System.out.println("\n  Found " + results.size() + " result(s):");
                    for (Book b : results) {
                        System.out.println("   → " + b);
                    }
                }
            }

            default -> System.out.println("  ⚠️  Invalid sub-choice.");
        }
    }

    /** Option 4 — Reads updated fields, validates them, and calls updateBook(). */
    private static void updateBook() {
        System.out.println("\n── UPDATE BOOK ───────────────────────");

        int bookId = readInt("  Enter Book ID to update: ");
        if (bookId <= 0) {
            System.out.println("  ⚠️  Invalid Book ID.");
            return;
        }

        // First verify the book exists before asking for new values
        Book existing = bookDAO.searchBookById(bookId);
        if (existing == null) {
            System.out.println("  ⚠️  No book found with ID: " + bookId + ". Update cancelled.");
            return;
        }

        System.out.println("  Current: " + existing);
        System.out.println("  Enter new values (press Enter to accept current):\n");

        System.out.print("  New Title     [" + existing.getTitle()    + "]: ");
        String title = sc.nextLine().trim();
        if (title.isEmpty()) title = existing.getTitle();

        System.out.print("  New Author    [" + existing.getAuthor()   + "]: ");
        String author = sc.nextLine().trim();
        if (author.isEmpty()) author = existing.getAuthor();

        System.out.print("  New Category  [" + existing.getCategory() + "]: ");
        String category = sc.nextLine().trim();
        if (category.isEmpty()) category = existing.getCategory();

        int quantity  = readInt("  New Quantity  [" + existing.getQuantity()  + "]: ",
                                existing.getQuantity());
        int available = readInt("  New Available [" + existing.getAvailable() + "]: ",
                                existing.getAvailable());

        // Validate updated inputs
        if (!Validator.validateBook(title, author, category, quantity, available)) {
            System.out.println("  ⚠️  Update cancelled due to validation errors.");
            return;
        }

        Book updated = new Book(bookId, title, author, category, quantity, available);
        bookDAO.updateBook(updated);
    }

    /** Option 5 — Reads a book ID and calls deleteBook(). */
    private static void deleteBook() {
        System.out.println("\n── DELETE BOOK ───────────────────────");

        int bookId = readInt("  Enter Book ID to delete: ");
        if (bookId <= 0) {
            System.out.println("  ⚠️  Invalid Book ID.");
            return;
        }

        // Confirm before deleting
        System.out.print("  ⚠️  Are you sure you want to delete Book ID " + bookId + "? (y/n): ");
        String confirm = sc.nextLine().trim();

        if (confirm.equalsIgnoreCase("y")) {
            bookDAO.deleteBook(bookId);
        } else {
            System.out.println("  Deletion cancelled.");
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════
    // MEMBER OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Option 6 — Collects member details, validates input, and calls addMember(). */
    private static void addMember() {
        System.out.println("\n── ADD MEMBER ────────────────────────");

        System.out.print("  Name  : ");
        String name = sc.nextLine().trim();

        System.out.print("  Email : ");
        String email = sc.nextLine().trim();

        System.out.print("  Phone : ");
        String phone = sc.nextLine().trim();

        // Validate member inputs (name, email format, phone format)
        if (!Validator.validateMember(name, email, phone)) {
            System.out.println("  ⚠️  Member registration cancelled due to validation errors.");
            return;
        }

        memberDAO.addMember(new Member(name, email, phone));
    }

    /** Option 7 — Fetches and prints all members in a formatted table. */
    private static void viewMembers() {
        System.out.println("\n── ALL MEMBERS ───────────────────────");

        ArrayList<Member> members = memberDAO.viewMembers();

        if (members.isEmpty()) {
            System.out.println("  No members registered.");
            return;
        }

        System.out.printf("  %-5s %-20s %-28s %-15s%n",
                "ID", "Name", "Email", "Phone");
        System.out.println("  " + "─".repeat(70));

        for (Member m : members) {
            System.out.printf("  %-5d %-20s %-28s %-15s%n",
                    m.getMemberId(),
                    truncate(m.getName(), 19),
                    truncate(m.getEmail(), 27),
                    m.getPhone());
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════
    // ISSUE / RETURN OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Option 8 — Collects issue details and calls issueBook(). */
    private static void issueBook() {
        System.out.println("\n── ISSUE BOOK ────────────────────────");

        int bookId   = readInt("  Enter Book ID   : ");
        int memberId = readInt("  Enter Member ID : ");

        if (bookId <= 0 || memberId <= 0) {
            System.out.println("  ⚠️  Invalid Book ID or Member ID.");
            return;
        }

        /*
         * Issue date = today.
         * Due date   = today + 14 days (DUE_PERIOD_DAYS from FineCalculator).
         *
         * LocalDate.now()  → current date (java.time)
         * Date.valueOf()   → converts LocalDate to java.sql.Date for JDBC
         */
        LocalDate today   = LocalDate.now();
        LocalDate dueDate = FineCalculator.calculateDueDate(today);

        IssueRecord record = new IssueRecord(
                bookId,
                memberId,
                Date.valueOf(today),
                Date.valueOf(dueDate),
                "ISSUED"
        );

        boolean success = issueDAO.issueBook(record);

        if (success) {
            System.out.println("  Issue Date : " + today);
            System.out.println("  Due Date   : " + dueDate);
        }
    }

    /** Option 9 — Reads an issue ID and calls returnBook(). */
    private static void returnBook() {
        System.out.println("\n── RETURN BOOK ───────────────────────");

        int issueId = readInt("  Enter Issue ID: ");
        if (issueId <= 0) {
            System.out.println("  ⚠️  Invalid Issue ID.");
            return;
        }

        issueDAO.returnBook(issueId);
    }

    /** Option 10 — Computes and prints fine for a given issue using FineCalculator. */
    private static void fineDetails() {
        System.out.println("\n── FINE DETAILS ──────────────────────");

        System.out.println("  1. Fine for a returned book  (issue + due + return dates)");
        System.out.println("  2. Current fine for an unreturned book  (issue + due dates)");

        int subChoice = readInt("  Choose: ");

        try {
            switch (subChoice) {

                case 1 -> {
                    System.out.print("  Issue Date  (YYYY-MM-DD): ");
                    LocalDate issueDate  = LocalDate.parse(sc.nextLine().trim());

                    System.out.print("  Due Date    (YYYY-MM-DD): ");
                    LocalDate dueDate    = LocalDate.parse(sc.nextLine().trim());

                    System.out.print("  Return Date (YYYY-MM-DD): ");
                    LocalDate returnDate = LocalDate.parse(sc.nextLine().trim());

                    FineCalculator.printFineDetails(issueDate, dueDate, returnDate);
                }

                case 2 -> {
                    System.out.print("  Issue Date (YYYY-MM-DD): ");
                    LocalDate issueDate = LocalDate.parse(sc.nextLine().trim());

                    System.out.print("  Due Date   (YYYY-MM-DD): ");
                    LocalDate dueDate   = LocalDate.parse(sc.nextLine().trim());

                    FineCalculator.printCurrentOverdueStatus(issueDate, dueDate);
                }

                default -> System.out.println("  ⚠️  Invalid sub-choice.");
            }
        } catch (DateTimeParseException e) {
            System.out.println("  ❌ Validation Error: Invalid date format. Please use YYYY-MM-DD format (e.g., 2026-08-05).");
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER / UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Safely reads an integer from the console.
     *
     * The problem with Scanner.nextInt() used naively:
     *   After reading the int, the '\n' newline character stays in the buffer.
     *   The next sc.nextLine() call reads that leftover '\n' as an empty string.
     *
     * Fix: always call sc.nextLine() immediately after sc.nextInt()
     *      to consume the dangling newline.
     *
     * InputMismatchException is caught when the user types a non-integer
     * (e.g., "abc"). We show an error message and default to 0.
     *
     * @param prompt Message to display before reading input.
     * @return The integer entered by the user, or 0 on invalid input.
     */
    private static int readInt(String prompt) {
        System.out.print(prompt);
        try {
            int value = sc.nextInt();
            sc.nextLine();    // consume the leftover '\n'
            return value;
        } catch (InputMismatchException e) {
            sc.nextLine();    // clear the invalid token from the buffer
            System.out.println("  ⚠️  Invalid input. Expected a number.");
            return 0;
        }
    }

    /**
     * Overloaded readInt — shows a prompt and returns a default value
     * if the user presses Enter without typing a number.
     *
     * Used in updateBook() to let users keep existing values by pressing Enter.
     *
     * @param prompt       Message to display.
     * @param defaultValue Value to return if input is invalid or empty.
     * @return The entered integer, or defaultValue if input was invalid.
     */
    private static int readInt(String prompt, int defaultValue) {
        System.out.print(prompt);
        try {
            int value = sc.nextInt();
            sc.nextLine();
            return value;
        } catch (InputMismatchException e) {
            sc.nextLine();
            return defaultValue;
        }
    }

    /**
     * Truncates a String to a maximum length and appends "…" if trimmed.
     *
     * Used for table formatting — prevents long titles from breaking column alignment.
     *
     * @param text      The string to truncate.
     * @param maxLength Maximum allowed length (including the "…" character).
     * @return The original string if short enough, or a truncated version ending with "…".
     */
    private static String truncate(String text, int maxLength) {
        if (text == null)             return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 1) + "…";
    }
}