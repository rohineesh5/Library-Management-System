package dao;

import database.DBConnection;
import model.Member;
import util.Validator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * MemberDAOImpl — Concrete implementation of the MemberDAO interface.
 *
 * Handles all SQL operations for the 'members' table.
 * Follows the same structure and style as BookDAOImpl:
 *   - SQL queries stored as private constants
 *   - PreparedStatement for all operations
 *   - try-with-resources for automatic resource cleanup
 *   - Graceful handling of invalid IDs (rowsAffected check)
 */
public class MemberDAOImpl implements MemberDAO {

    // ─── SQL Constants ────────────────────────────────────────────────────────

    /**
     * INSERT a new member row.
     *
     * member_id is AUTO_INCREMENT — MySQL generates it automatically.
     * We provide only: name, email, phone.
     *
     * ? placeholders (in order):
     *   1 → name
     *   2 → email
     *   3 → phone
     */
    private static final String INSERT_MEMBER_SQL =
            "INSERT INTO members (name, email, phone) VALUES (?, ?, ?)";

    /**
     * SELECT all member rows from the table.
     *
     * No WHERE clause → returns every registered member.
     * Columns returned: member_id, name, email, phone.
     */
    private static final String SELECT_ALL_MEMBERS_SQL =
            "SELECT * FROM members";

    /**
     * Partial-match search on the 'name' column.
     *
     * LIKE ?  → the '?' will be bound with "%" + keyword + "%" in Java.
     * This lets queries like "ali" match "Ali Hassan" or "Syed Ali Raza".
     *
     * MySQL's LIKE is case-insensitive by default for utf8mb4_general_ci columns.
     */
    private static final String SELECT_MEMBER_BY_NAME_SQL =
            "SELECT * FROM members WHERE name LIKE ?";

    /**
     * UPDATE all editable fields for the member matching member_id.
     *
     * SET clause: name, email, phone — all three editable columns.
     * WHERE member_id = ?  → ensures only ONE specific row is changed.
     *
     * ? placeholder order:
     *   1 → name
     *   2 → email
     *   3 → phone
     *   4 → member_id  (WHERE clause — always last)
     */
    private static final String UPDATE_MEMBER_SQL =
            "UPDATE members SET name = ?, email = ?, phone = ? WHERE member_id = ?";

    /**
     * DELETE the row matching the given member_id.
     *
     * WHERE member_id = ?  → targets exactly one row.
     * Missing WHERE would erase the entire members table!
     *
     * ? placeholder order:
     *   1 → member_id
     */
    private static final String DELETE_MEMBER_SQL =
            "DELETE FROM members WHERE member_id = ?";

    /**
     * Check whether an email already exists in the members table.
     *
     * SELECT COUNT(*) returns the number of rows matching the email.
     *   0 → email is free to use
     *   1 → email is already taken (UNIQUE constraint would reject the INSERT anyway,
     *       but checking BEFORE the INSERT gives us a user-friendly message instead
     *       of a raw SQL exception)
     */
    private static final String EMAIL_EXISTS_SQL =
            "SELECT COUNT(*) FROM members WHERE email = ?";


    // ─── addMember() ─────────────────────────────────────────────────────────

    /**
     * Registers a new library member by inserting a row into the 'members' table.
     *
     * Before inserting, calls emailExists() to detect duplicate emails early
     * and give a clear message — rather than relying on MySQL's UNIQUE violation.
     *
     * memberId in the Member object is ignored — MySQL generates it via AUTO_INCREMENT.
     *
     * @param m Member object containing name, email, and phone to insert.
     */
    @Override
    public void addMember(Member m) {

        /*
         * Duplicate-email pre-check:
         * If the email already exists, we reject immediately with a clear message.
         * This is a business rule check — separate from the DB constraint.
         *
         * Dual-layer protection:
         *   Layer 1: emailExists() → friendly user message
         *   Layer 2: MySQL UNIQUE constraint → catches any race condition
         */
        if (emailExists(m.getEmail())) {
            System.out.println("  ❌ Registration failed: The email '" + m.getEmail()
                    + "' is already registered to another member.");
            System.out.println("     Please use a different email address.");
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_MEMBER_SQL)) {

            pstmt.setString(1, m.getName());
            pstmt.setString(2, m.getEmail());
            pstmt.setString(3, m.getPhone());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Member registered successfully: " + m.getName());
            } else {
                System.out.println("⚠️  Member was not registered. No rows affected.");
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "registering member");
            e.printStackTrace();
        }
    }


    // ─── emailExists() ───────────────────────────────────────────────────────

    /**
     * Checks if the given email address already exists in the members table.
     *
     * Uses SELECT COUNT(*) — returns a single integer row immediately.
     * More efficient than SELECT * when we only need to know IF a row exists.
     *
     * This is a private helper — only called internally by addMember().
     *
     * @param email The email address to look up.
     * @return true if the email is already in the table; false if it is free.
     */
    private boolean emailExists(String email) {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(EMAIL_EXISTS_SQL)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    /*
                     * rs.getInt(1) reads the first (and only) column — COUNT(*).
                     * COUNT(*) > 0 means at least one member already has this email.
                     */
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "checking email existence");
            e.printStackTrace();
        }

        // If a DB error occurred, return false to allow the INSERT to proceed
        // (MySQL's UNIQUE constraint will still catch the duplicate if needed)
        return false;
    }


    // ─── viewMembers() ───────────────────────────────────────────────────────

    /**
     * Retrieves all members registered in the library system.
     *
     * Iterates through the ResultSet row by row, builds a Member object
     * per row using column names, and collects them into an ArrayList.
     *
     * @return ArrayList<Member> containing all members; empty list if none exist.
     */
    @Override
    public ArrayList<Member> viewMembers() {

        ArrayList<Member> members = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_MEMBERS_SQL);

             /*
              * ResultSet is declared in try-with-resources so it is closed
              * automatically alongside the Connection and PreparedStatement.
              * This avoids DB cursor leaks.
              */
             ResultSet rs = pstmt.executeQuery()) {

            /*
             * rs.next() advances the internal cursor to the next row.
             * Returns true if a row exists, false when all rows are exhausted.
             * The while loop collects every member into the list.
             */
            while (rs.next()) {

                /*
                 * Read each column by its exact name in the 'members' table.
                 * Using column names (not indexes) keeps this code safe if
                 * the column order in the DB schema ever changes.
                 */
                members.add(new Member(
                        rs.getInt("member_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone")
                ));
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "fetching members");
            e.printStackTrace();
        }

        // Returns the filled list, or an empty list if the table is empty or an error occurred
        return members;
    }


    // ─── searchMember() ──────────────────────────────────────────────────────

    /**
     * Searches for members whose name contains the given keyword (partial match).
     *
     * The '%' wildcard is added around the keyword in Java before binding,
     * because PreparedStatement does not allow '%' inside the '?' itself.
     *
     * Example: searchMember("ali") matches "Ali Hassan", "Syed Ali", "Wali Khan".
     *
     * @param name The keyword to search for within member names.
     * @return ArrayList<Member> of all matching members; empty if none found.
     */
    @Override
    public ArrayList<Member> searchMember(String name) {

        ArrayList<Member> members = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_MEMBER_BY_NAME_SQL)) {

            /*
             * Wrap keyword with '%' wildcards to enable partial matching.
             * "%" + name + "%" becomes the LIKE pattern, e.g., "%ali%".
             * This must be done in Java — you cannot embed it in the SQL '?'.
             */
            pstmt.setString(1, "%" + name + "%");

            try (ResultSet rs = pstmt.executeQuery()) {

                // Collect all matching rows into the list
                while (rs.next()) {
                    members.add(new Member(
                            rs.getInt("member_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("phone")
                    ));
                }
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "searching member");
            e.printStackTrace();
        }

        return members;
    }


    // ─── updateMember() ──────────────────────────────────────────────────────

    /**
     * Updates all editable fields (name, email, phone) for the member
     * identified by member_id.
     *
     * If member_id does not match any row, executeUpdate() returns 0
     * and we report gracefully — no exception is thrown.
     *
     * @param m Member object containing the updated data AND the target member_id.
     */
    @Override
    public void updateMember(Member m) {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_MEMBER_SQL)) {

            /*
             * Bind values in the exact order of '?' placeholders in the SQL:
             *
             *   SET  name  = ?   → position 1
             *   SET  email = ?   → position 2
             *   SET  phone = ?   → position 3
             *   WHERE member_id = ?  → position 4 (always last — identifies the row)
             *
             * Binding member_id last is a consistent convention:
             * SET columns first, WHERE target last.
             */
            pstmt.setString(1, m.getName());
            pstmt.setString(2, m.getEmail());
            pstmt.setString(3, m.getPhone());
            pstmt.setInt(4, m.getMemberId());    // WHERE member_id = ?

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Member updated successfully. ID: " + m.getMemberId());
            } else {
                // Graceful invalid-ID handling — warns without crashing
                System.out.println("⚠️  No member found with ID: " + m.getMemberId() + ". Nothing was updated.");
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "updating member");
            e.printStackTrace();
        }
    }


    // ─── deleteMember() ──────────────────────────────────────────────────────

    /**
     * Removes a member record from the 'members' table using their member_id.
     *
     * If the member_id does not exist (invalid, already deleted, or wrong input),
     * executeUpdate() returns 0 and we report the situation clearly.
     *
     * @param memberId The primary key of the member to delete.
     */
    @Override
    public void deleteMember(int memberId) {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_MEMBER_SQL)) {

            /*
             * Only one parameter — position 1 — the member_id in the WHERE clause.
             * PreparedStatement prevents SQL injection attacks like:
             *   memberId = "1 OR 1=1" → which would delete every row.
             */
            pstmt.setInt(1, memberId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Member deleted successfully. ID: " + memberId);
            } else {
                // Graceful handling — no crash, just a clear warning message
                System.out.println("⚠️  No member found with ID: " + memberId + ". Nothing was deleted.");
            }

        } catch (SQLException e) {
            Validator.handleSQLError(e.getErrorCode(), "deleting member");
            e.printStackTrace();
        }
    }
}
