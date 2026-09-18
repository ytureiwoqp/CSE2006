package com.library.exception;

/** Our own checked exception: thrown when a library rule is broken. */
public class LibraryException extends Exception {

    private static final long serialVersionUID = 1L;

    public LibraryException(String message) {
        super(message);
    }
}
