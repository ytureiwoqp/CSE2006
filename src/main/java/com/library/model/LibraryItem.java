package com.library.model;

/** Abstract parent class of Book and Magazine. Fields are private (encapsulation). */
public abstract class LibraryItem implements Borrowable {

    private final int id;
    private final String title;
    private final String creator;     // author or publisher
    private final String category;
    private final boolean available;

    protected LibraryItem(int id, String title, String creator, String category, boolean available) {
        this.id = id;
        this.title = title.trim();
        this.creator = creator.trim();
        this.category = category.trim().isEmpty() ? "General" : category.trim();
        this.available = available;
    }

    public abstract String getType();      // "BOOK" or "MAGAZINE"

    public abstract String getExtra();     // ISBN or issue number

    public abstract String describe();

    @Override
    public boolean isAvailable() {
        return available;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCreator() { return creator; }
    public String getCategory() { return category; }

    @Override
    public String toString() {
        return describe() + " [" + (available ? "Available" : "Issued") + "]";
    }
}
