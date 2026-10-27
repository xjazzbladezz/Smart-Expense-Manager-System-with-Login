package com.smartexpense.model;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Central registry of categories, payment modes and their colours. */
public final class Categories {

    public static final List<String> EXPENSE = List.of(
            "Food & Dining", "Groceries", "Rent & Housing", "Transport", "Utilities", "Shopping",
            "Entertainment", "Health", "Education", "Travel", "Subscriptions", "Other");

    public static final List<String> INCOME = List.of(
            "Salary", "Freelance", "Investments", "Gifts", "Other Income");

    public static final List<String> PAYMENT_MODES = List.of(
            "UPI", "Cash", "Credit Card", "Debit Card", "Net Banking", "Wallet");

    private static final Map<String, Color> COLORS = new HashMap<>();
    private static final Color[] FALLBACK = {
            new Color(0x7C5CFF), new Color(0x22D3EE), new Color(0x34D399), new Color(0xFBBF24),
            new Color(0xF472B6), new Color(0xFB923C), new Color(0x60A5FA), new Color(0xA78BFA)
    };

    static {
        put("Food & Dining", 0xFB923C);
        put("Groceries", 0x34D399);
        put("Rent & Housing", 0x7C5CFF);
        put("Transport", 0x38BDF8);
        put("Utilities", 0xFBBF24);
        put("Shopping", 0xF472B6);
        put("Entertainment", 0xA78BFA);
        put("Health", 0xF87171);
        put("Education", 0x2DD4BF);
        put("Travel", 0x60A5FA);
        put("Subscriptions", 0xE879F9);
        put("Other", 0x94A3B8);
        put("Others", 0x64748B);

        put("Salary", 0x34D399);
        put("Freelance", 0x22D3EE);
        put("Investments", 0xA3E635);
        put("Gifts", 0xF9A8D4);
        put("Other Income", 0x94A3B8);

        put("UPI", 0x7C5CFF);
        put("Cash", 0x34D399);
        put("Credit Card", 0xF472B6);
        put("Debit Card", 0x38BDF8);
        put("Net Banking", 0xFBBF24);
        put("Wallet", 0xFB923C);
    }

    private Categories() {
    }

    private static void put(String name, int rgb) {
        COLORS.put(name, new Color(rgb));
    }

    public static Color color(String name) {
        if (name == null) return FALLBACK[0];
        Color c = COLORS.get(name);
        if (c != null) return c;
        return FALLBACK[Math.abs(name.hashCode()) % FALLBACK.length];
    }

    public static List<String> forType(TransactionType type) {
        return type == TransactionType.INCOME ? INCOME : EXPENSE;
    }
}
