package com.library;

import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.LibraryItem;
import com.library.model.Magazine;
import com.library.model.Member;
import com.library.model.MemberType;
import com.library.service.LibraryService;
import com.library.util.BorrowThread;
import com.library.util.FileUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Console menu for the Library Management System.
 * Run with no arguments for the normal menu, or with --memory to use a temporary database.
 */
public class Main {

    private static final String DEFAULT_CSV = "data/sample_items.csv";
    private static final Scanner scanner = new Scanner(System.in);
    private static LibraryService service;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--memory")) {
            System.setProperty("db.url", "jdbc:h2:mem:library;DB_CLOSE_DELAY=-1");
        }
        try {
            service = new LibraryService();
        } catch (IllegalStateException e) {
            System.out.println("Cannot start: " + e.getMessage());
            return;
        }

        System.out.println("=== Library Management System ===");
        boolean running = true;
        while (running) {
            printMenu();
            try {
                int choice = readInt("Choose an option: ");
                switch (choice) {
                    case 1:  addBook(); break;
                    case 2:  addMagazine(); break;
                    case 3:  registerMember(); break;
                    case 4:  listItems(); break;
                    case 5:  searchItems(); break;
                    case 6:  listMembers(); break;
                    case 7:  issueItem(); break;
                    case 8:  returnItem(); break;
                    case 9:  printLines(service.getActiveLoans()); break;
                    case 10: memberHistory(); break;
                    case 11: deleteItem(); break;
                    case 12: inventorySummary(); break;
                    case 13: importCsv(); break;
                    case 14: exportCatalogue(); break;
                    case 15: printLines(service.getRecentActivity()); break;
                    case 16: threadDemo(); break;
                    case 0:  running = false; break;
                    default: System.out.println("Invalid option - choose a number from 0 to 16.");
                }
            } catch (NoSuchElementException e) {
                System.out.println("\nNo more input - exiting.");
                running = false;
            } catch (LibraryException | SQLException | IOException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println(" 1. Add book              9. Items currently on loan");
        System.out.println(" 2. Add magazine         10. Member loan history");
        System.out.println(" 3. Register member      11. Delete item");
        System.out.println(" 4. List items           12. Inventory summary");
        System.out.println(" 5. Search items         13. Import items from CSV");
        System.out.println(" 6. List members         14. Export catalogue + backup copy");
        System.out.println(" 7. Issue item           15. Recent activity");
        System.out.println(" 8. Return item          16. Two-thread borrowing demo");
        System.out.println(" 0. Exit");
    }

    // ---------------------------------------------------------------- menu actions

    private static void addBook() throws SQLException, LibraryException {
        String title = readRequired("Title: ");
        String author = readLine("Author: ");
        String isbn = readLine("ISBN: ");
        String category = readLine("Category (blank = General): ");
        int id = service.addItem(new Book(0, title, author, category, true, isbn));
        System.out.println("Book saved with id " + id + ".");
    }

    private static void addMagazine() throws SQLException, LibraryException {
        String title = readRequired("Title: ");
        String publisher = readLine("Publisher: ");
        String issue = readLine("Issue number: ");
        String category = readLine("Category (blank = General): ");
        int id = service.addItem(new Magazine(0, title, publisher, category, true, issue));
        System.out.println("Magazine saved with id " + id + ".");
    }

    private static void registerMember() throws SQLException, LibraryException {
        String name = readRequired("Name: ");
        String email = readRequired("E-mail: ");
        MemberType type;
        try {
            type = MemberType.valueOf(readRequired("Type (student/faculty): ").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new LibraryException("Member type must be student or faculty");
        }
        int id = service.addMember(name, email, type);
        System.out.println("Member registered with id " + id + " (" + type.getMaxItems()
                + " items for " + type.getLoanDays() + " days).");
    }

    private static void listItems() throws SQLException {
        List<LibraryItem> items = service.getAllItems();
        // anonymous class: sort the items by title
        Collections.sort(items, new Comparator<LibraryItem>() {
            @Override
            public int compare(LibraryItem a, LibraryItem b) {
                return a.getTitle().compareToIgnoreCase(b.getTitle());
            }
        });
        printItems(items);
    }

    private static void searchItems() throws SQLException {
        String keyword = readRequired("Search for (title, author or category): ");
        String type = readLine("Only books or magazines? (book/magazine, blank = both): ");
        List<LibraryItem> matches;
        if (type.isEmpty()) {
            matches = service.searchItems(keyword);          // overloaded method, 1 argument
        } else {
            matches = service.searchItems(keyword, type);    // overloaded method, 2 arguments
        }
        printItems(matches);
    }

    private static void listMembers() throws SQLException {
        List<Member> members = service.getAllMembers();
        if (members.isEmpty()) {
            System.out.println("(no members yet)");
        }
        for (Member m : members) {
            System.out.printf("%3d  %s%n", m.getId(), m);
        }
    }

    private static void issueItem() throws SQLException, LibraryException {
        int itemId = readInt("Item id: ");
        int memberId = readInt("Member id: ");
        System.out.println(service.issueItem(itemId, memberId));
    }

    private static void returnItem() throws SQLException, LibraryException {
        int itemId = readInt("Item id being returned: ");
        int daysLate = readInt("Days after the due date (0 if on time): ");
        System.out.println(service.returnItem(itemId, daysLate));
    }

    private static void memberHistory() throws SQLException, LibraryException {
        printLines(service.getMemberHistory(readInt("Member id: ")));
    }

    private static void deleteItem() throws SQLException, LibraryException {
        int itemId = readInt("Item id to delete: ");
        service.deleteItem(itemId);
        System.out.println("Item " + itemId + " deleted.");
    }

    private static void inventorySummary() throws SQLException {
        int[][] counts = service.getInventoryCounts();
        String[] labels = {"Books", "Magazines"};
        System.out.println("             Available  Issued");
        for (int i = 0; i < counts.length; i++) {
            System.out.printf("%-12s %8d %7d%n", labels[i], counts[i][0], counts[i][1]);
        }
    }

    private static void importCsv() throws IOException {
        String file = readLine("CSV file (blank = " + DEFAULT_CSV + "): ");
        if (file.isEmpty()) {
            file = DEFAULT_CSV;
        }
        int[] result = FileUtil.importItems(service, file);
        System.out.println("Imported " + result[0] + " item(s), skipped " + result[1] + ".");
    }

    private static void exportCatalogue() throws SQLException, IOException {
        FileUtil.exportCatalogue(service.getAllItems(), "output/catalogue.csv");
        long bytes = FileUtil.copyFile("output/catalogue.csv", "output/catalogue-backup.csv");
        System.out.println("Saved output/catalogue.csv and a backup copy (" + bytes + " bytes).");
    }

    /** Two threads try to borrow the same item at the same moment; synchronized lets only one win. */
    private static void threadDemo() {
        int itemId = readInt("Item id both members want: ");
        int member1 = readInt("First member id: ");
        int member2 = readInt("Second member id: ");

        BorrowThread t1 = new BorrowThread("Borrower-1", service, itemId, member1);
        BorrowThread t2 = new BorrowThread("Borrower-2", service, itemId, member2);
        System.out.println("Thread state before start(): " + t1.getState());   // NEW
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted while waiting for the threads.");
        }
        System.out.println("Thread state after finishing: " + t1.getState());  // TERMINATED
        System.out.println(t1.getName() + ": " + t1.getResult());
        System.out.println(t2.getName() + ": " + t2.getResult());
    }

    // ---------------------------------------------------------------- helpers

    private static void printItems(List<LibraryItem> items) {
        if (items.isEmpty()) {
            System.out.println("(no items)");
        }
        for (LibraryItem item : items) {
            System.out.printf("%3d  %s%n", item.getId(), item);
        }
    }

    private static void printLines(List<String> lines) {
        if (lines.isEmpty()) {
            System.out.println("(nothing to show)");
        }
        for (String line : lines) {
            System.out.println(line);
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String readRequired(String prompt) {
        String text = readLine(prompt);
        while (text.isEmpty()) {
            System.out.println("This field cannot be empty.");
            text = readLine(prompt);
        }
        return text;
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }
}
