package com.smartexpense.model;

import java.time.LocalDate;

public class Transaction {
    private int id;
    private int userId;
    private LocalDate date;
    private TransactionType type;
    private String category;
    private double amount;
    private String description;
    private String paymentMode;

    public Transaction() {
    }

    public Transaction(int id, int userId, LocalDate date, TransactionType type, String category,
                       double amount, String description, String paymentMode) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.paymentMode = paymentMode;
    }

    public Transaction copy() {
        return new Transaction(id, userId, date, type, category, amount, description, paymentMode);
    }

    /** Copies every editable field (not the id / user) from another transaction. */
    public void copyValuesFrom(Transaction o) {
        this.date = o.date;
        this.type = o.type;
        this.category = o.category;
        this.amount = o.amount;
        this.description = o.description;
        this.paymentMode = o.paymentMode;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public boolean isExpense() { return type == TransactionType.EXPENSE; }
    public boolean isIncome() { return type == TransactionType.INCOME; }

    @Override
    public String toString() {
        return "Transaction{" + id + ", " + date + ", " + type + ", " + category + ", " + amount + ", " + description + "}";
    }
}
