package com.library.model;

/** Enum with a constructor: each member type carries its own borrowing rules. */
public enum MemberType {

    STUDENT(3, 14, 0, 1.0),
    FACULTY(6, 30, 7, 0.5);

    private final int maxItems;
    private final int loanDays;
    private final int graceDays;       // late days that are not charged
    private final double fineFactor;   // 1.0 = full fine, 0.5 = half fine

    MemberType(int maxItems, int loanDays, int graceDays, double fineFactor) {
        this.maxItems = maxItems;
        this.loanDays = loanDays;
        this.graceDays = graceDays;
        this.fineFactor = fineFactor;
    }

    public int getMaxItems() { return maxItems; }
    public int getLoanDays() { return loanDays; }
    public int getGraceDays() { return graceDays; }
    public double getFineFactor() { return fineFactor; }
}
