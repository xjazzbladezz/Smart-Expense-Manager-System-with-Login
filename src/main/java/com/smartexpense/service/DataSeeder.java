package com.smartexpense.service;

import com.smartexpense.model.Transaction;
import com.smartexpense.model.TransactionType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Generates six months of realistic demo data so the dashboard looks alive on first launch. */
public final class DataSeeder {

    private DataSeeder() {
    }

    private record Spec(String category, String[] descriptions, int min, int max, int perMonthMin,
                        int perMonthMax, String[] modes) {
    }

    private static final Spec[] SPECS = {
            new Spec("Food & Dining",
                    new String[]{"Dinner at Biryani House", "Zomato order", "Swiggy order", "Cafe Coffee Day",
                            "Lunch with friends", "Domino's Pizza", "Starbucks coffee", "Street food"},
                    180, 1400, 8, 13, new String[]{"UPI", "Credit Card", "Cash"}),
            new Spec("Groceries",
                    new String[]{"BigBasket order", "Blinkit groceries", "Local vegetable market", "Reliance Fresh",
                            "DMart shopping", "Milk and bread"},
                    250, 2600, 5, 8, new String[]{"UPI", "Debit Card", "Cash"}),
            new Spec("Transport",
                    new String[]{"Uber ride", "Ola cab", "Metro recharge", "Petrol", "Auto rickshaw", "Rapido bike"},
                    40, 1800, 8, 13, new String[]{"UPI", "Cash", "Wallet"}),
            new Spec("Shopping",
                    new String[]{"Amazon order", "Flipkart order", "Myntra clothes", "Nike shoes", "Croma gadgets"},
                    399, 6500, 2, 4, new String[]{"Credit Card", "UPI"}),
            new Spec("Entertainment",
                    new String[]{"Movie tickets", "BookMyShow", "Gaming top-up", "Concert pass", "Bowling night"},
                    200, 2200, 2, 4, new String[]{"UPI", "Credit Card"}),
            new Spec("Health",
                    new String[]{"Pharmacy", "Doctor consultation", "Gym membership", "Lab tests"},
                    150, 3200, 1, 2, new String[]{"UPI", "Debit Card"}),
            new Spec("Education",
                    new String[]{"Udemy course", "Books", "Coursera subscription", "Exam fee"},
                    300, 3500, 0, 2, new String[]{"Net Banking", "UPI"}),
            new Spec("Travel",
                    new String[]{"IRCTC train ticket", "Flight booking", "Hotel stay", "Weekend trip"},
                    900, 9000, 0, 1, new String[]{"Credit Card", "Net Banking"})
    };

    public static List<Transaction> generate(int userId) {
        Random rnd = new Random(2005);
        LocalDate today = LocalDate.now();
        YearMonth now = YearMonth.from(today);
        List<Transaction> out = new ArrayList<>();

        for (int back = 5; back >= 0; back--) {
            YearMonth ym = now.minusMonths(back);
            int maxDay = back == 0 ? today.getDayOfMonth() : ym.lengthOfMonth();

            // income
            add(out, userId, ym, 1, maxDay, TransactionType.INCOME, "Salary", 85000, "Monthly salary", "Net Banking");
            if (rnd.nextInt(10) < 6) {
                add(out, userId, ym, 8 + rnd.nextInt(12), maxDay, TransactionType.INCOME, "Freelance",
                        6000 + rnd.nextInt(20000), "Freelance project payment", "UPI");
            }
            if (rnd.nextInt(10) < 5) {
                add(out, userId, ym, 12 + rnd.nextInt(10), maxDay, TransactionType.INCOME, "Investments",
                        900 + rnd.nextInt(3500), "Mutual fund dividend", "Net Banking");
            }

            // fixed costs
            add(out, userId, ym, 3, maxDay, TransactionType.EXPENSE, "Rent & Housing", 22000, "House rent", "Net Banking");
            add(out, userId, ym, 6, maxDay, TransactionType.EXPENSE, "Utilities", 1400 + rnd.nextInt(900),
                    "Electricity bill", "UPI");
            add(out, userId, ym, 7, maxDay, TransactionType.EXPENSE, "Utilities", 999, "Jio Fiber internet", "UPI");
            add(out, userId, ym, 9, maxDay, TransactionType.EXPENSE, "Utilities", 399, "Mobile recharge", "UPI");
            add(out, userId, ym, 11, maxDay, TransactionType.EXPENSE, "Subscriptions", 649, "Netflix", "Credit Card");
            add(out, userId, ym, 12, maxDay, TransactionType.EXPENSE, "Subscriptions", 119, "Spotify", "Credit Card");
            add(out, userId, ym, 14, maxDay, TransactionType.EXPENSE, "Subscriptions", 219, "iCloud storage", "Credit Card");

            // variable spending
            for (Spec s : SPECS) {
                int count = s.perMonthMin + rnd.nextInt(s.perMonthMax - s.perMonthMin + 1);
                for (int i = 0; i < count; i++) {
                    int day = 1 + rnd.nextInt(maxDay);
                    double amt = s.min + rnd.nextDouble() * (s.max - s.min);
                    amt = Math.round(amt / 10.0) * 10.0;
                    add(out, userId, ym, day, maxDay, TransactionType.EXPENSE, s.category,
                            amt, s.descriptions[rnd.nextInt(s.descriptions.length)],
                            s.modes[rnd.nextInt(s.modes.length)]);
                }
            }
        }
        return out;
    }

    private static void add(List<Transaction> out, int userId, YearMonth ym, int day, int maxDay,
                            TransactionType type, String category, double amount, String desc, String mode) {
        if (day > maxDay) return;
        out.add(new Transaction(0, userId, ym.atDay(day), type, category, amount, desc, mode));
    }
}
