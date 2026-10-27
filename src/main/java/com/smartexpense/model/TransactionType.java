package com.smartexpense.model;

public enum TransactionType {
    INCOME("Income"),
    EXPENSE("Expense");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static TransactionType parse(String s) {
        if (s == null) return null;
        String v = s.trim().toLowerCase();
        if (v.startsWith("inc") || v.equals("credit") || v.equals("cr")) return INCOME;
        if (v.startsWith("exp") || v.equals("debit") || v.equals("dr")) return EXPENSE;
        return null;
    }

    @Override
    public String toString() {
        return label;
    }
}
