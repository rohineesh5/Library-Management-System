package util;

import java.util.regex.Pattern;

/**
 * Validator — A stateless utility class for reusable input validation.
 *
 * Design:
 * - All methods are static — no object needed.
 * - Each method returns boolean (valid = true) so callers can branch cleanly.
 * - Each method prints a specific error message when validation fails,
 * so callers only need to check the return value.
 * - Private constructor prevents instantiation.
 */
public class Validator {

    // ─── Regex Patterns ───────────────────────────────────────────────────────

    /**
     * Basic email pattern — validates the most common email formats.
     *
     * Pattern breakdown:
     * [\\w._%+-]+ → one or more word chars, dots, underscores, %, +, -
     * 
     * @ → literal @ symbol
     *   [\\w.-]+ → domain name (e.g., "gmail")
     *   \\. → literal dot
     *   [a-zA-Z]{2,} → top-level domain (e.g., "com", "org") — at least 2 chars
     *
     *   Pattern.compile() with CASE_INSENSITIVE makes it match "USER@GMAIL.COM"
     *   too.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Phone pattern — allows digits, spaces, dashes, plus sign, and parentheses.
     *
     * Valid examples: "03001234567", "+92-300-1234567", "(021) 1234567"
     * Minimum 7 digits enforced by the minimum length check in isValidPhone().
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+\\d][\\d\\s()\\-]{6,}$");

    // ─── Private Constructor ──────────────────────────────────────────────────

    /** Prevents instantiation — utility class, static methods only. */
    private Validator() {
    }

    // ─── String Validators ────────────────────────────────────────────────────

    /**
     * Checks that a string is not null and not blank (empty after trimming).
     *
     * @param value     The string to check.
     * @param fieldName A label used in the error message (e.g., "Title", "Author").
     * @return true if the string has content; false if null or blank.
     */
    public static boolean isNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            System.out.println("  ❌ Validation Error: " + fieldName + " cannot be empty.");
            return false;
        }
        return true;
    }

    /**
     * Validates an email address against the EMAIL_PATTERN regex.
     *
     * Checks both that it is non-empty AND matches the expected format.
     *
     * @param email The email address to validate.
     * @return true if the email is non-empty and well-formed; false otherwise.
     */
    public static boolean isValidEmail(String email) {
        if (!isNotEmpty(email, "Email"))
            return false;

        /*
         * Pattern.matcher(input).matches() returns true only if the ENTIRE
         * input string matches the pattern (anchored match — same as ^...$).
         */
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            System.out.println("  ❌ Validation Error: Invalid email format → \"" + email + "\"");
            System.out.println("     Expected format: user@example.com");
            return false;
        }
        return true;
    }

    /**
     * Validates a phone number against the PHONE_PATTERN regex.
     *
     * @param phone The phone number string to validate.
     * @return true if the phone is non-empty and matches expected format; false
     *         otherwise.
     */
    public static boolean isValidPhone(String phone) {
        if (!isNotEmpty(phone, "Phone"))
            return false;

        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            System.out.println("  ❌ Validation Error: Invalid phone number → \"" + phone + "\"");
            System.out.println("     Expected format: 03001234567 or +92-300-1234567");
            return false;
        }
        return true;
    }

    // ─── Numeric Validators ───────────────────────────────────────────────────

    /**
     * Checks that an integer is strictly greater than zero.
     *
     * Used to validate quantity — a book must have at least 1 copy.
     *
     * @param value     The number to check.
     * @param fieldName Label for the error message (e.g., "Quantity").
     * @return true if value > 0; false otherwise.
     */
    public static boolean isPositive(int value, String fieldName) {
        if (value <= 0) {
            System.out.println("  ❌ Validation Error: " + fieldName
                    + " must be greater than 0. Got: " + value);
            return false;
        }
        return true;
    }

    /**
     * Checks that an integer is zero or greater (non-negative).
     *
     * Used for 'available' — a book can have 0 copies available if all are issued.
     *
     * @param value     The number to check.
     * @param fieldName Label for the error message (e.g., "Available").
     * @return true if value >= 0; false otherwise.
     */
    public static boolean isNonNegative(int value, String fieldName) {
        if (value < 0) {
            System.out.println("  ❌ Validation Error: " + fieldName
                    + " cannot be negative. Got: " + value);
            return false;
        }
        return true;
    }

    // ─── Cross-Field Validators ───────────────────────────────────────────────

    /**
     * Checks that 'available' does not exceed 'quantity'.
     *
     * A library cannot have more copies available than it actually owns.
     * For example: quantity=3, available=5 is physically impossible.
     *
     * @param available Copies currently available.
     * @param quantity  Total copies owned.
     * @return true if available <= quantity; false otherwise.
     */
    public static boolean isAvailableWithinQuantity(int available, int quantity) {
        if (available > quantity) {
            System.out.println("  ❌ Validation Error: Available copies (" + available
                    + ") cannot exceed total Quantity (" + quantity + ").");
            return false;
        }
        return true;
    }

    // ─── Composite Validators (validate a whole object at once) ───────────────

    /**
     * Validates all fields required to add or update a Book.
     *
     * Runs every check and collects all errors before returning,
     * so the user sees ALL problems in one shot instead of one at a time.
     *
     * @return true if ALL fields are valid; false if any check fails.
     */
    public static boolean validateBook(String title, String author,
            String category, int quantity, int available) {
        boolean valid = true;

        // Run all checks — don't short-circuit so ALL errors are printed
        if (!isNotEmpty(title, "Title"))
            valid = false;
        if (!isNotEmpty(author, "Author"))
            valid = false;
        if (!isNotEmpty(category, "Category"))
            valid = false;
        if (!isPositive(quantity, "Quantity"))
            valid = false;
        if (!isNonNegative(available, "Available"))
            valid = false;

        // Only run cross-field check if both numeric checks passed
        if (valid && !isAvailableWithinQuantity(available, quantity))
            valid = false;

        return valid;
    }

    /**
     * Validates all fields required to add or update a Member.
     *
     * @return true if ALL fields are valid; false if any check fails.
     */
    public static boolean validateMember(String name, String email, String phone) {
        boolean valid = true;

        if (!isNotEmpty(name, "Name"))
            valid = false;
        if (!isValidEmail(email))
            valid = false;
        if (!isValidPhone(phone))
            valid = false;

        return valid;
    }

    // ─── SQL Error Code Interpreter ───────────────────────────────────────────

    /**
     * Translates a MySQL error code (from SQLException.getErrorCode()) into
     * a human-readable message and prints it.
     *
     * MySQL error codes of interest:
     * 1062 — Duplicate entry: UNIQUE constraint violated
     * 1048 — Column cannot be null: NOT NULL constraint violated
     * 1452 — Foreign key constraint fails (e.g., invalid book_id or member_id)
     * 1146 — Table doesn't exist
     * 0 — Unknown / not a MySQL-specific code
     *
     * @param errorCode The value from SQLException.getErrorCode().
     * @param context   A label describing what operation failed (e.g., "adding
     *                  member").
     */
    public static void handleSQLError(int errorCode, String context) {
        System.out.println("  ❌ Database error while " + context + ":");

        switch (errorCode) {
            case 1062 ->
                System.out.println("     Cause : Duplicate entry — a record with this value already exists.");
            case 1048 ->
                System.out.println("     Cause : A required field is missing (NULL not allowed).");
            case 1452 ->
                System.out.println("     Cause : Invalid reference — the Book ID or Member ID does not exist.");
            case 1146 ->
                System.out.println("     Cause : Database table not found. Please check your DB setup.");
            case 1045 ->
                System.out.println("     Cause : Access denied — check your database username and password.");
            case 0 ->
                System.out.println("     Cause : Unknown SQL error. See stack trace for details.");
            default ->
                System.out.println("     Cause : SQL error code " + errorCode
                        + ". See MySQL documentation for details.");
        }
    }
}
