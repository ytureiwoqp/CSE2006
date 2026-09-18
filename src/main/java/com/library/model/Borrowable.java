package com.library.model;

/** Interface: anything the library can lend out. */
public interface Borrowable {

    boolean isAvailable();

    double getFinePerDay();
}
