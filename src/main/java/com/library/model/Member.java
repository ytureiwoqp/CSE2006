package com.library.model;

public class Member {

    private final int id;
    private final String name;
    private final String email;
    private final MemberType type;

    public Member(int id, String name, String email, MemberType type) {
        this.id = id;
        this.name = name.trim();
        this.email = email.trim();
        this.type = type;
    }

    /** Fine for returning an item daysLate days after the due date. */
    public double calculateFine(int daysLate, double finePerDay) {
        int chargeableDays = Math.max(0, daysLate - type.getGraceDays());
        return chargeableDays * finePerDay * type.getFineFactor();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public MemberType getType() { return type; }

    @Override
    public String toString() {
        return name + " (" + type + ", " + email + ")";
    }
}
