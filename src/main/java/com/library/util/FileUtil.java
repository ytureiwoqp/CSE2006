package com.library.util;

import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.LibraryItem;
import com.library.model.Magazine;
import com.library.service.LibraryService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/** File input/output: character streams for text files, byte streams for copying. */
public class FileUtil {

    /**
     * Reads a CSV file with a BufferedReader (character stream) and adds each line to the library.
     * Line format: type,title,creator,isbn-or-issue,category   (fields must not contain commas)
     *
     * @return {imported, skipped}
     */
    public static int[] importItems(LibraryService service, String fileName) throws IOException {
        int imported = 0;
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;                       // ignore blank lines and comments
                }
                String[] parts = line.split(",");
                if (parts.length < 5) {
                    System.out.println("  skipped (needs 5 columns): " + line);
                    skipped++;
                    continue;
                }
                try {
                    String type = parts[0].trim().toUpperCase();
                    if (type.equals("BOOK")) {
                        service.addItem(new Book(0, parts[1], parts[2], parts[4], true, parts[3].trim()));
                    } else if (type.equals("MAGAZINE")) {
                        service.addItem(new Magazine(0, parts[1], parts[2], parts[4], true, parts[3].trim()));
                    } else {
                        System.out.println("  skipped (unknown type '" + parts[0] + "'): " + line);
                        skipped++;
                        continue;
                    }
                    imported++;
                } catch (LibraryException | SQLException e) {
                    System.out.println("  skipped (" + e.getMessage() + "): " + line);
                    skipped++;
                }
            }
        }
        return new int[] {imported, skipped};
    }

    /** Writes the catalogue to a CSV file with a PrintWriter (character stream). */
    public static void exportCatalogue(List<LibraryItem> items, String fileName) throws IOException {
        File parent = new File(fileName).getParentFile();
        if (parent != null) {
            parent.mkdirs();                        // create the folder if needed
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            out.println("id,type,title,creator,isbn_or_issue,category,status");
            for (LibraryItem item : items) {
                out.println(item.getId() + "," + item.getType() + "," + item.getTitle() + ","
                        + item.getCreator() + "," + item.getExtra() + "," + item.getCategory() + ","
                        + (item.isAvailable() ? "Available" : "Issued"));
            }
        }
    }

    /** Copies a file byte by byte with FileInputStream / FileOutputStream (byte streams). */
    public static long copyFile(String source, String destination) throws IOException {
        long total = 0;
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                total += count;
            }
        }
        return total;
    }
}
