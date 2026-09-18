package com.library.model;

public class Book extends LibraryItem {

    private final String isbn;

    public Book(int id, String title, String author, String category, boolean available, String isbn) {
        super(id, title, author, category, available);   // call the parent constructor
        this.isbn = isbn;
    }

    @Override
    public String getType() { return "BOOK"; }

    @Override
    public String getExtra() { return isbn; }

    @Override
    public double getFinePerDay() { return 2.0; }

    @Override
    public String describe() {
        return "Book: " + getTitle() + " by " + getCreator() + " (ISBN " + isbn + ", " + getCategory() + ")";
    }
}
