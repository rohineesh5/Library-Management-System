package util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * FineCalculator — A stateless utility class for library fine calculations.
 *
 * Rules:
 *   - Standard loan period : 14 days from the issue date.
 *   - Fine rate            : ₹10 per day for every day past the due date.
 *   - No fine              : if the book is returned on or before the due date.
 *
 * Design notes:
 *   - All methods are static — no object needs to be created to use this class.
 *   - The private constructor prevents accidental instantiation.
 *   - Uses java.time.LocalDate (modern, immutable date API from Java 8+).
 *   - Uses ChronoUnit.DAYS.between() for clean, readable day-count arithmetic.
 */
public class FineCalculator {

    // ─── Constants ────────────────────────────────────────────────────────────

    /**
     * Standard number of days a member is allowed to keep a book.
     * Used to calculate the due date from the issue date.
     *
     * Example: issued on 2024-01-01 → due on 2024-01-15 (14 days later).
     */
    public static final int DUE_PERIOD_DAYS = 14;

    /**
     * Fine charged per day of delay, in Indian Rupees (₹).
     * Applied to every calendar day the book is overdue.
     *
     * Example: 3 days late → ₹10 × 3 = ₹30 fine.
     */
    public static final double FINE_PER_DAY = 10.0;


    // ─── Private Constructor ──────────────────────────────────────────────────

    /**
     * Private constructor — prevents instantiation.
     *
     * A utility class with only static methods should never be instantiated.
     * Making the constructor private enforces this at the language level.
     * Calling 'new FineCalculator()' will result in a compile-time error.
     */
    private FineCalculator() {
        // not instantiable
    }


    // ─── Core Methods ─────────────────────────────────────────────────────────

    /**
     * Calculates the number of days a book was returned late.
     *
     * ChronoUnit.DAYS.between(start, end):
     *   Returns the number of whole days from 'start' (inclusive)
     *   to 'end' (exclusive).
     *
     *   If returnDate is AFTER dueDate  → positive number (late)
     *   If returnDate IS the dueDate    → 0 (on time, no fine)
     *   If returnDate is BEFORE dueDate → negative number (early return)
     *
     * We clamp the result to 0 using Math.max() so early returns always
     * produce 0 late days (never a negative fine).
     *
     * @param dueDate    The date by which the book should have been returned.
     * @param returnDate The actual date the book was returned.
     * @return Number of late days (0 if returned on time or early).
     */
    public static long calculateLateDays(LocalDate dueDate, LocalDate returnDate) {

        /*
         * ChronoUnit.DAYS.between(dueDate, returnDate):
         *   Counts whole days from dueDate to returnDate.
         *   Positive  → returnDate is after dueDate (late)
         *   Zero      → returned exactly on due date
         *   Negative  → returned before due date (early)
         *
         * Math.max(0, ...) ensures we never return a negative number.
         * A member who returns a book 3 days early has 0 late days, not -3.
         */
        long lateDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        return Math.max(0, lateDays);
    }

    /**
     * Calculates the fine amount for a returned book.
     *
     * Fine = lateDays × FINE_PER_DAY
     *
     * @param dueDate    The date the book was due.
     * @param returnDate The actual return date.
     * @return Fine amount in ₹ (0.0 if returned on time).
     */
    public static double calculateFine(LocalDate dueDate, LocalDate returnDate) {

        long lateDays = calculateLateDays(dueDate, returnDate);

        /*
         * Multiply late days by the daily rate.
         * If lateDays == 0 (on time or early), this returns 0.0 — no fine.
         */
        return lateDays * FINE_PER_DAY;
    }

    /**
     * Overloaded convenience method — calculates fine directly from late day count.
     *
     * Useful when you already know the number of late days and just want
     * to compute the rupee amount, without re-passing date objects.
     *
     * Method Overloading: Java allows two methods with the same name as long as
     * their parameter lists differ. The compiler picks the right one automatically
     * based on what arguments you pass.
     *
     * @param lateDays Number of days overdue (must be ≥ 0).
     * @return Fine amount in ₹.
     */
    public static double calculateFine(long lateDays) {
        return lateDays * FINE_PER_DAY;
    }

    /**
     * Calculates the due date from an issue date.
     *
     * LocalDate.plusDays(n) returns a NEW LocalDate object that is n days later.
     * LocalDate is immutable — plusDays() never modifies the original object.
     *
     * @param issueDate The date the book was issued.
     * @return The due date (issueDate + DUE_PERIOD_DAYS).
     */
    public static LocalDate calculateDueDate(LocalDate issueDate) {

        /*
         * plusDays() is from the java.time API (Java 8+).
         * It accounts for varying month lengths, leap years, etc. automatically.
         * Never use manual arithmetic like "day + 14" — it breaks at month boundaries.
         */
        return issueDate.plusDays(DUE_PERIOD_DAYS);
    }

    /**
     * Checks whether a book that has NOT yet been returned is currently overdue.
     *
     * Compares dueDate against today's date (LocalDate.now()).
     * Used for tracking currently-issued (unreturned) books.
     *
     * @param dueDate The date by which the book should be returned.
     * @return true if today is past the due date; false if still within the loan period.
     */
    public static boolean isOverdue(LocalDate dueDate) {

        /*
         * LocalDate.now() — the current date at the time this method is called.
         * .isAfter(dueDate) — returns true only if today is strictly AFTER dueDate.
         *   Due date is today  → NOT overdue (still within grace).
         *   Due date was yesterday → overdue.
         */
        return LocalDate.now().isAfter(dueDate);
    }

    /**
     * Calculates how many days a currently-borrowed (unreturned) book is overdue.
     *
     * Uses today as the "virtual return date" to estimate the fine so far.
     * If the book is not yet overdue, returns 0.
     *
     * @param dueDate The due date of the active issue record.
     * @return Current number of overdue days (0 if still within loan period).
     */
    public static long currentOverdueDays(LocalDate dueDate) {

        /*
         * We treat today as the return date to estimate current lateness.
         * Delegates to calculateLateDays() to reuse the same clamping logic.
         */
        return calculateLateDays(dueDate, LocalDate.now());
    }

    /**
     * Calculates the current accrued fine for a book that has not yet been returned.
     *
     * @param dueDate The due date of the active issue record.
     * @return Fine accrued so far in ₹ (0.0 if still within the loan period).
     */
    public static double currentAccruedFine(LocalDate dueDate) {
        return calculateFine(dueDate, LocalDate.now());
    }


    // ─── Display Methods ──────────────────────────────────────────────────────

    /**
     * Prints a complete fine summary for a returned book.
     *
     * Shows all relevant dates, late days, and the final fine amount.
     * Useful for the console UI when a member returns a book.
     *
     * @param issueDate  Date the book was issued.
     * @param dueDate    Date the book was due.
     * @param returnDate Date the book was actually returned.
     */
    public static void printFineDetails(LocalDate issueDate,
                                        LocalDate dueDate,
                                        LocalDate returnDate) {

        long   lateDays = calculateLateDays(dueDate, returnDate);
        double fine     = calculateFine(lateDays);

        System.out.println("─────────────────────────────────────");
        System.out.println("         FINE CALCULATION REPORT     ");
        System.out.println("─────────────────────────────────────");
        System.out.println("  Issue Date  : " + issueDate);
        System.out.println("  Due Date    : " + dueDate);
        System.out.println("  Return Date : " + returnDate);
        System.out.println("─────────────────────────────────────");

        if (lateDays == 0) {
            System.out.println("  Status      : ✅ Returned on time");
            System.out.println("  Fine        : ₹0.00 (No fine)");
        } else {
            System.out.println("  Status      : ⚠️  OVERDUE by " + lateDays + " day(s)");
            System.out.printf( "  Fine        : ₹%.2f  (%d days × ₹%.0f/day)%n",
                               fine, lateDays, FINE_PER_DAY);
        }

        System.out.println("─────────────────────────────────────");
    }

    /**
     * Prints the current overdue status for an active (unreturned) book.
     *
     * @param issueDate The date the book was issued.
     * @param dueDate   The due date of the issue record.
     */
    public static void printCurrentOverdueStatus(LocalDate issueDate, LocalDate dueDate) {

        LocalDate today          = LocalDate.now();
        long      overdueDays    = currentOverdueDays(dueDate);
        double    accruedFine    = currentAccruedFine(dueDate);

        System.out.println("─────────────────────────────────────");
        System.out.println("      CURRENT OVERDUE STATUS         ");
        System.out.println("─────────────────────────────────────");
        System.out.println("  Issue Date  : " + issueDate);
        System.out.println("  Due Date    : " + dueDate);
        System.out.println("  Today       : " + today);
        System.out.println("─────────────────────────────────────");

        if (overdueDays == 0) {
            long daysLeft = ChronoUnit.DAYS.between(today, dueDate);
            System.out.println("  Status      : ✅ Within loan period");
            System.out.println("  Days Left   : " + daysLeft + " day(s)");
            System.out.println("  Fine So Far : ₹0.00");
        } else {
            System.out.println("  Status      : ⚠️  OVERDUE by " + overdueDays + " day(s)");
            System.out.printf( "  Fine So Far : ₹%.2f  (%d days × ₹%.0f/day)%n",
                               accruedFine, overdueDays, FINE_PER_DAY);
        }

        System.out.println("─────────────────────────────────────");
    }
}
