package com.smartexpense.model;

public class User {
    private final int id;
    private final String username;
    private final String fullName;

    public User(int id, String username, String fullName) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }

    public String initials() {
        String n = (fullName == null || fullName.isBlank()) ? username : fullName.trim();
        String[] parts = n.split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
