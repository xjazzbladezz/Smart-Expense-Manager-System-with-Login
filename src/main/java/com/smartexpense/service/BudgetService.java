package com.smartexpense.service;

import com.smartexpense.db.BudgetDAO;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class BudgetService {

    public record Status(String category, double limit, double spent) {
        public double ratio() {
            return limit <= 0 ? 0 : spent / limit;
        }

        public double remaining() {
            return limit - spent;
        }
    }

    private final int userId;
    private final BudgetDAO dao = new BudgetDAO();
    private final ExpenseService expenses;
    private final Map<String, Double> limits = new LinkedHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public BudgetService(int userId, ExpenseService expenses) {
        this.userId = userId;
        this.expenses = expenses;
    }

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    private void fire() {
        for (Runnable r : listeners) r.run();
    }

    public void load() throws SQLException {
        limits.clear();
        limits.putAll(dao.findAll(userId));
        fire();
    }

    public Map<String, Double> limits() {
        return Collections.unmodifiableMap(limits);
    }

    public void set(String category, double limit) throws SQLException {
        dao.upsert(userId, category, limit);
        limits.put(category, limit);
        fire();
    }

    public void remove(String category) throws SQLException {
        dao.delete(userId, category);
        limits.remove(category);
        fire();
    }

    public List<Status> statuses(YearMonth ym) {
        List<Status> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : limits.entrySet()) {
            out.add(new Status(e.getKey(), e.getValue(), expenses.spentIn(e.getKey(), ym)));
        }
        out.sort(Comparator.comparingDouble(Status::ratio).reversed());
        return out;
    }

    public double totalLimit() {
        double s = 0;
        for (double v : limits.values()) s += v;
        return s;
    }

    public double totalSpent(YearMonth ym) {
        double s = 0;
        for (String c : limits.keySet()) s += expenses.spentIn(c, ym);
        return s;
    }

    public int onTrackCount(YearMonth ym) {
        int n = 0;
        for (Status st : statuses(ym)) if (st.ratio() < 0.8) n++;
        return n;
    }
}
