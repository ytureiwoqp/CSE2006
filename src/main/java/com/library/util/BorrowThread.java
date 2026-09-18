package com.library.util;

import com.library.exception.LibraryException;
import com.library.service.LibraryService;

import java.sql.SQLException;

/** A thread (created by extending Thread) in which one member tries to borrow an item. */
public class BorrowThread extends Thread {

    private final LibraryService service;
    private final int itemId;
    private final int memberId;
    private String result = "not run yet";

    public BorrowThread(String name, LibraryService service, int itemId, int memberId) {
        super(name);
        this.service = service;
        this.itemId = itemId;
        this.memberId = memberId;
    }

    @Override
    public void run() {
        try {
            result = "SUCCESS - " + service.issueItem(itemId, memberId);
        } catch (LibraryException e) {
            result = "REJECTED - " + e.getMessage();
        } catch (SQLException e) {
            result = "DATABASE ERROR - " + e.getMessage();
        }
    }

    public String getResult() {
        return result;
    }
}
