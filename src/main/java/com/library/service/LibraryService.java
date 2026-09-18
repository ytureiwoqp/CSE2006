package com.library.service;

import com.library.dao.Database;
import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.LibraryItem;
import com.library.model.Magazine;
import com.library.model.Member;
import com.library.model.MemberType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/** All library operations. Data is stored with JDBC; the business rules are checked here. */
public class LibraryService {

    // An item counts as issued while it has a loan with no return date.
    private static final String ITEM_SELECT =
            "SELECT i.*, (SELECT COUNT(*) FROM loans l WHERE l.item_id = i.id AND l.return_date IS NULL) AS out_count "
            + "FROM items i ";

    private final Database db = Database.getInstance();
    private final Stack<String> recentActivity = new Stack<>();

    // ------------------------------------------------------------ items (Create / Read / Delete)

    public int addItem(LibraryItem item) throws SQLException, LibraryException {
        if (item.getTitle().isEmpty()) {
            throw new LibraryException("Title cannot be empty");
        }
        String sql = "INSERT INTO items (item_type, title, creator, extra, category) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getType());
            ps.setString(2, item.getTitle());
            ps.setString(3, item.getCreator());
            ps.setString(4, item.getExtra());
            ps.setString(5, item.getCategory());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public List<LibraryItem> getAllItems() throws SQLException {
        return queryItems(ITEM_SELECT + "ORDER BY i.id", null);
    }

    public LibraryItem getItem(int itemId) throws SQLException, LibraryException {
        List<LibraryItem> found = queryItems(ITEM_SELECT + "WHERE i.id = ?", itemId);
        if (found.isEmpty()) {
            throw new LibraryException("No item found with id " + itemId);
        }
        return found.get(0);
    }

    /** Search by keyword in the title, creator or category. */
    public List<LibraryItem> searchItems(String keyword) throws SQLException {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return queryItems(ITEM_SELECT + "WHERE LOWER(i.title || ' ' || i.creator || ' ' || i.category) LIKE ?", pattern);
    }

    /** Overloaded version: search, then keep only "BOOK" or "MAGAZINE" results. */
    public List<LibraryItem> searchItems(String keyword, String type) throws SQLException {
        List<LibraryItem> result = new ArrayList<>();
        for (LibraryItem item : searchItems(keyword)) {
            if (item.getType().equalsIgnoreCase(type.trim())) {
                result.add(item);
            }
        }
        return result;
    }

    public void deleteItem(int itemId) throws SQLException, LibraryException {
        getItem(itemId);   // throws LibraryException if it does not exist
        if (countRows("SELECT COUNT(*) FROM loans WHERE item_id = ?", itemId) > 0) {
            throw new LibraryException("Item " + itemId + " has loan history and cannot be deleted");
        }
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM items WHERE id = ?")) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------ members

    public int addMember(String name, String email, MemberType type) throws SQLException, LibraryException {
        if (name.trim().isEmpty() || !email.contains("@")) {
            throw new LibraryException("A name and a valid e-mail address are required");
        }
        if (countRows("SELECT COUNT(*) FROM members WHERE email = ?", email.trim()) > 0) {
            throw new LibraryException("A member with the e-mail " + email.trim() + " already exists");
        }
        String sql = "INSERT INTO members (name, email, member_type) VALUES (?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.setString(2, email.trim());
            ps.setString(3, type.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public List<Member> getAllMembers() throws SQLException {
        return queryMembers("SELECT * FROM members ORDER BY id", null);
    }

    public Member getMember(int memberId) throws SQLException, LibraryException {
        List<Member> found = queryMembers("SELECT * FROM members WHERE id = ?", memberId);
        if (found.isEmpty()) {
            throw new LibraryException("No member found with id " + memberId);
        }
        return found.get(0);
    }

    // ------------------------------------------------------------ issue and return

    /**
     * synchronized: only one thread at a time can check availability and issue, so two people
     * can never borrow the same copy.
     */
    public synchronized String issueItem(int itemId, int memberId) throws SQLException, LibraryException {
        LibraryItem item = getItem(itemId);
        Member member = getMember(memberId);

        if (!item.isAvailable()) {
            throw new LibraryException("'" + item.getTitle() + "' is already issued to someone else");
        }
        int limit = member.getType().getMaxItems();
        if (countRows("SELECT COUNT(*) FROM loans WHERE member_id = ? AND return_date IS NULL", memberId) >= limit) {
            throw new LibraryException(member.getName() + " has reached the limit of " + limit + " items");
        }

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO loans (item_id, member_id) VALUES (?, ?)")) {
            ps.setInt(1, itemId);
            ps.setInt(2, memberId);
            ps.executeUpdate();
        }
        recentActivity.push(member.getName() + " borrowed '" + item.getTitle() + "'");
        return "Issued '" + item.getTitle() + "' to " + member.getName()
                + ". Please return it within " + member.getType().getLoanDays() + " days.";
    }

    /** Returns an item. daysLate is how many days after the due date it came back (0 = on time). */
    public synchronized String returnItem(int itemId, int daysLate) throws SQLException, LibraryException {
        if (daysLate < 0) {
            throw new LibraryException("Days late cannot be negative");
        }
        int loanId = -1;
        int memberId = -1;
        String sql = "SELECT id, member_id FROM loans WHERE item_id = ? AND return_date IS NULL";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    loanId = rs.getInt("id");
                    memberId = rs.getInt("member_id");
                }
            }
        }
        if (loanId == -1) {
            throw new LibraryException("Item " + itemId + " is not currently issued");
        }

        LibraryItem item = getItem(itemId);
        Member member = getMember(memberId);
        double fine = member.calculateFine(daysLate, item.getFinePerDay());

        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE loans SET return_date = CURRENT_DATE, fine = ? WHERE id = ?")) {
            ps.setDouble(1, fine);
            ps.setInt(2, loanId);
            ps.executeUpdate();
        }
        recentActivity.push(member.getName() + " returned '" + item.getTitle() + "'");
        return "'" + item.getTitle() + "' returned by " + member.getName() + ". "
                + (fine > 0 ? String.format("Late fine to collect: %.2f", fine) : "No fine due.");
    }

    // ------------------------------------------------------------ reports

    public List<String> getActiveLoans() throws SQLException {
        String sql = "SELECT l.item_id, i.title, m.name, l.issue_date FROM loans l "
                + "JOIN items i ON l.item_id = i.id JOIN members m ON l.member_id = m.id "
                + "WHERE l.return_date IS NULL ORDER BY l.id";
        List<String> lines = new ArrayList<>();
        try (Connection c = db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lines.add(String.format("Item %d '%s' is with %s (issued %s)",
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
        }
        return lines;
    }

    public List<String> getMemberHistory(int memberId) throws SQLException, LibraryException {
        getMember(memberId);   // throws LibraryException if unknown
        String sql = "SELECT i.title, l.issue_date, l.return_date, l.fine FROM loans l "
                + "JOIN items i ON l.item_id = i.id WHERE l.member_id = ? ORDER BY l.id DESC";
        List<String> lines = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String returned = rs.getString("return_date");
                    String status = (returned == null)
                            ? "not returned yet"
                            : String.format("returned %s, fine %.2f", returned, rs.getDouble("fine"));
                    lines.add("'" + rs.getString("title") + "' issued " + rs.getString("issue_date") + ", " + status);
                }
            }
        }
        return lines;
    }

    /** 2-D array: rows = {books, magazines}, columns = {available, issued}. */
    public int[][] getInventoryCounts() throws SQLException {
        int[][] counts = new int[2][2];
        for (LibraryItem item : getAllItems()) {
            int row = (item instanceof Book) ? 0 : 1;
            int col = item.isAvailable() ? 0 : 1;
            counts[row][col]++;
        }
        return counts;
    }

    /** Most recent action first (the Stack is read from the top down). */
    public List<String> getRecentActivity() {
        List<String> newestFirst = new ArrayList<>();
        for (int i = recentActivity.size() - 1; i >= 0; i--) {
            newestFirst.add(recentActivity.get(i));
        }
        return newestFirst;
    }

    // ------------------------------------------------------------ private JDBC helpers

    private List<LibraryItem> queryItems(String sql, Object param) throws SQLException {
        List<LibraryItem> items = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) {
                ps.setObject(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String title = rs.getString("title");
                    String creator = rs.getString("creator");
                    String category = rs.getString("category");
                    String extra = rs.getString("extra");
                    boolean available = rs.getInt("out_count") == 0;
                    if (rs.getString("item_type").equals("MAGAZINE")) {
                        items.add(new Magazine(id, title, creator, category, available, extra));
                    } else {
                        items.add(new Book(id, title, creator, category, available, extra));
                    }
                }
            }
        }
        return items;
    }

    private List<Member> queryMembers(String sql, Object param) throws SQLException {
        List<Member> members = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) {
                ps.setObject(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(new Member(rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                            MemberType.valueOf(rs.getString("member_type"))));
                }
            }
        }
        return members;
    }

    private int countRows(String sql, Object param) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
