package com.library.model;

public class Magazine extends LibraryItem {

    private final String issueNumber;

    public Magazine(int id, String title, String publisher, String category, boolean available, String issueNumber) {
        super(id, title, publisher, category, available);
        this.issueNumber = issueNumber;
    }

    @Override
    public String getType() { return "MAGAZINE"; }

    @Override
    public String getExtra() { return issueNumber; }

    @Override
    public double getFinePerDay() { return 1.0; }

    @Override
    public String describe() {
        return "Magazine: " + getTitle() + " by " + getCreator() + " (issue " + issueNumber + ", " + getCategory() + ")";
    }
}
