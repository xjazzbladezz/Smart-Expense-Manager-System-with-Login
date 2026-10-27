package com.smartexpense.service;

import com.smartexpense.model.User;

/** Holds the logged-in user and the services bound to that user. */
public final class AppContext {

    private static User user;
    private static ExpenseService expenses;
    private static BudgetService budgets;

    private AppContext() {
    }

    public static void init(User u) {
        user = u;
        expenses = new ExpenseService(u.getId());
        budgets = new BudgetService(u.getId(), expenses);
    }

    public static void clear() {
        user = null;
        expenses = null;
        budgets = null;
    }

    public static User user() { return user; }
    public static ExpenseService expenses() { return expenses; }
    public static BudgetService budgets() { return budgets; }
}
