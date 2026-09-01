package model;

/**
 * Model class representing a library Member.
 * Follows the same encapsulation pattern as Book.java.
 *
 * Maps directly to the 'members' table in the database:
 *   member_id | name | email | phone
 */
public class Member {

    // ─── Private Fields ───────────────────────────────────────────────────────

    /**
     * member_id: Primary key in the database.
     * AUTO_INCREMENT — MySQL generates it; we never set it manually on INSERT.
     */
    private int memberId;

    /**
     * name: Full name of the library member.
     * Maps to the 'name' column (VARCHAR).
     */
    private String name;

    /**
     * email: Contact email address of the member.
     * Maps to the 'email' column (VARCHAR).
     * Typically UNIQUE in the DB — no two members share the same email.
     */
    private String email;

    /**
     * phone: Contact phone number of the member.
     * Stored as String, not int — to preserve leading zeros and allow
     * formats like "+92-300-1234567" or "03001234567".
     * Maps to the 'phone' column (VARCHAR).
     */
    private String phone;


    // ─── Default Constructor ──────────────────────────────────────────────────

    /**
     * No-arg constructor — required for creating an empty Member object
     * before populating fields one by one using setters.
     */
    public Member() {
    }


    // ─── Parameterized Constructor ────────────────────────────────────────────

    /**
     * Full constructor — used when fetching a member record from the database
     * (all columns including member_id are available).
     *
     * @param memberId Auto-generated primary key from DB
     * @param name     Full name of the member
     * @param email    Email address
     * @param phone    Phone number
     */
    public Member(int memberId, String name, String email, String phone) {
        this.memberId = memberId;
        this.name     = name;
        this.email    = email;
        this.phone    = phone;
    }

    /**
     * Insert constructor — used when registering a NEW member.
     * memberId is excluded because MySQL AUTO_INCREMENT generates it.
     *
     * @param name  Full name of the member
     * @param email Email address
     * @param phone Phone number
     */
    public Member(String name, String email, String phone) {
        this.name  = name;
        this.email = email;
        this.phone = phone;
    }


    // ─── Getters ──────────────────────────────────────────────────────────────

    /** Returns the member's database-assigned ID. */
    public int getMemberId()  { return memberId; }

    /** Returns the member's full name. */
    public String getName()   { return name; }

    /** Returns the member's email address. */
    public String getEmail()  { return email; }

    /** Returns the member's phone number. */
    public String getPhone()  { return phone; }


    // ─── Setters ──────────────────────────────────────────────────────────────

    /** Sets the member ID — typically called after fetching from the DB. */
    public void setMemberId(int memberId)  { this.memberId = memberId; }

    /** Sets the member's full name. */
    public void setName(String name)       { this.name  = name; }

    /** Sets the member's email address. */
    public void setEmail(String email)     { this.email = email; }

    /** Sets the member's phone number. */
    public void setPhone(String phone)     { this.phone = phone; }


    // ─── toString() ───────────────────────────────────────────────────────────

    /**
     * Human-readable representation of a Member object.
     * Useful for printing search results and debugging.
     */
    @Override
    public String toString() {
        return "Member [memberId=" + memberId
                + ", name="  + name
                + ", email=" + email
                + ", phone=" + phone + "]";
    }
}
