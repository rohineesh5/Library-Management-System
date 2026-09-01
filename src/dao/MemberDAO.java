package dao;

import model.Member;

import java.util.ArrayList;

/**
 * MemberDAO — Interface defining the contract for all member-related DB operations.
 *
 * Following the same DAO pattern as BookDAO:
 *   - Any class implementing this interface must provide all methods below.
 *   - Separates database logic from business/UI logic.
 *   - Makes the implementation swappable (e.g., MySQL → PostgreSQL) without
 *     changing any code that calls these methods.
 */
public interface MemberDAO {

    /** Registers a new member in the library system. */
    void addMember(Member m);

    /** Retrieves all registered members from the database. */
    ArrayList<Member> viewMembers();

    /**
     * Searches for members whose name contains the given keyword.
     * Partial match — e.g., "ali" matches "Ali Hassan", "Syed Ali".
     */
    ArrayList<Member> searchMember(String name);

    /** Updates all editable fields of an existing member identified by member_id. */
    void updateMember(Member m);

    /** Removes a member record from the database using their member_id. */
    void deleteMember(int memberId);
}
