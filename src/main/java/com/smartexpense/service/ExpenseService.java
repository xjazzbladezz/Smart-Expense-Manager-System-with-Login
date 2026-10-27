package com.smartexpense.service;

import com.smartexpense.db.TransactionDAO;
import com.smartexpense.ds.LRUCache;
import com.smartexpense.ds.TopN;
import com.smartexpense.ds.Trie;
import com.smartexpense.ds.UndoRedoManager;
import com.smartexpense.model.Transaction;
import com.smartexpense.model.TransactionType;
import com.smartexpense.util.Fmt;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Heart of the application: keeps every transaction in memory, talks to MySQL through the DAO,
 * powers undo/redo (stacks), auto-suggest (trie), top-N (heap) and cached analytics (LRU).
 */
public class ExpenseService {

    public record MonthSummary(YearMonth month, double income, double expense) {
        public double net() {
            return income - expense;
        }
    }

    public record Insight(String kind, String title, String text) {
    }

    private final int userId;
    private final TransactionDAO dao = new TransactionDAO();

    private final List<Transaction> txns = new ArrayList<>();
    private final Trie descriptionTrie = new Trie();
    private final Map<String, String> descToCategory = new HashMap<>();
    private final UndoRedoManager history = new UndoRedoManager();
    private final LRUCache<String, Object> cache = new LRUCache<>(96);
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private long cacheHits;
    private long cacheMisses;

    public ExpenseService(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    // ------------------------------------------------------------------ lifecycle

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    public void reload() throws SQLException {
        List<Transaction> fresh = dao.findAll(userId);
        txns.clear();
        txns.addAll(fresh);
        history.clear();
        onChanged();
    }

    private void onChanged() {
        txns.sort(Comparator.comparing(Transaction::getDate).reversed()
                .thenComparing(Comparator.comparingInt(Transaction::getId).reversed()));
        descriptionTrie.clear();
        descToCategory.clear();
        for (Transaction t : txns) {
            String d = t.getDescription();
            if (d == null || d.isBlank()) continue;
            descriptionTrie.insert(d);
            descToCategory.putIfAbsent(d.trim().toLowerCase(), t.getCategory());
        }
        cache.clear();
        for (Runnable r : listeners) r.run();
    }

    // ------------------------------------------------------------------ read access

    public List<Transaction> getAll() {
        return Collections.unmodifiableList(txns);
    }

    public boolean isEmpty() {
        return txns.isEmpty();
    }

    public List<String> suggestDescriptions(String prefix, int limit) {
        return descriptionTrie.suggest(prefix, limit);
    }

    public String categoryFor(String description) {
        if (description == null) return null;
        return descToCategory.get(description.trim().toLowerCase());
    }

    // ------------------------------------------------------------------ mutations (undoable)

    public void add(Transaction t) throws Exception {
        t.setUserId(userId);
        history.run(new UndoRedoManager.Command() {
            @Override
            public void execute() throws Exception {
                t.setId(dao.insert(t));
                txns.add(t);
                onChanged();
            }

            @Override
            public void undo() throws Exception {
                dao.delete(t.getId(), userId);
                txns.remove(t);
                onChanged();
            }

            @Override
            public String label() {
                return "add \"" + shortDesc(t) + "\"";
            }
        });
    }

    /** Updates {@code target} in place with the values of {@code edited}. */
    public void update(Transaction target, Transaction edited) throws Exception {
        final Transaction before = target.copy();
        final Transaction after = edited.copy();
        history.run(new UndoRedoManager.Command() {
            @Override
            public void execute() throws Exception {
                target.copyValuesFrom(after);
                dao.update(target);
                onChanged();
            }

            @Override
            public void undo() throws Exception {
                target.copyValuesFrom(before);
                dao.update(target);
                onChanged();
            }

            @Override
            public String label() {
                return "edit \"" + shortDesc(before) + "\"";
            }
        });
    }

    public void delete(List<Transaction> targets) throws Exception {
        final List<Transaction> victims = new ArrayList<>(targets);
        if (victims.isEmpty()) return;
        history.run(new UndoRedoManager.Command() {
            @Override
            public void execute() throws Exception {
                for (Transaction t : victims) {
                    dao.delete(t.getId(), userId);
                    txns.remove(t);
                }
                onChanged();
            }

            @Override
            public void undo() throws Exception {
                for (Transaction t : victims) {
                    t.setId(dao.insert(t));
                    txns.add(t);
                }
                onChanged();
            }

            @Override
            public String label() {
                return victims.size() == 1 ? "delete \"" + shortDesc(victims.get(0)) + "\""
                        : "delete " + victims.size() + " transactions";
            }
        });
    }

    public String undo() throws Exception {
        return history.undo();
    }

    public String redo() throws Exception {
        return history.redo();
    }

    public boolean canUndo() { return history.canUndo(); }
    public boolean canRedo() { return history.canRedo(); }
    public int undoDepth() { return history.undoDepth(); }
    public int redoDepth() { return history.redoDepth(); }

    // ------------------------------------------------------------------ bulk (not undoable)

    public void addAll(List<Transaction> list) throws SQLException {
        for (Transaction t : list) t.setUserId(userId);
        dao.insertBatch(list);
        reload();
    }

    public void clearAll() throws SQLException {
        dao.deleteAll(userId);
        reload();
    }

    private static String shortDesc(Transaction t) {
        String d = t.getDescription();
        if (d == null || d.isBlank()) d = t.getCategory();
        return d.length() > 26 ? d.substring(0, 25) + "…" : d;
    }

    // ------------------------------------------------------------------ analytics

    private static String key(YearMonth ym) {
        return ym == null ? "ALL" : ym.toString();
    }

    private static boolean in(Transaction t, YearMonth ym) {
        return ym == null || YearMonth.from(t.getDate()).equals(ym);
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Supplier<T> supplier) {
        Object v = cache.get(key);
        if (v != null) {
            cacheHits++;
            return (T) v;
        }
        cacheMisses++;
        T val = supplier.get();
        cache.put(key, val);
        return val;
    }

    public int cacheSize() {
        return cache.size();
    }

    public double cacheHitRate() {
        long total = cacheHits + cacheMisses;
        return total == 0 ? 0 : (double) cacheHits / total;
    }

    public int trieSize() {
        return descriptionTrie.size();
    }

    /** Total for a type; {@code ym == null} means all time. */
    public double total(TransactionType type, YearMonth ym) {
        return cached("total:" + type + ":" + key(ym), () -> {
            double sum = 0;
            for (Transaction t : txns) if (t.getType() == type && in(t, ym)) sum += t.getAmount();
            return sum;
        });
    }

    public double balance() {
        return total(TransactionType.INCOME, null) - total(TransactionType.EXPENSE, null);
    }

    public double savingsRate(YearMonth ym) {
        double inc = total(TransactionType.INCOME, ym);
        if (inc <= 0) return 0;
        return (inc - total(TransactionType.EXPENSE, ym)) / inc;
    }

    private Map<String, Double> groupExpense(YearMonth ym, Function<Transaction, String> keyFn) {
        Map<String, Double> sums = new HashMap<>();
        for (Transaction t : txns) {
            if (t.isExpense() && in(t, ym)) sums.merge(keyFn.apply(t), t.getAmount(), Double::sum);
        }
        List<Map.Entry<String, Double>> entries = new ArrayList<>(sums.entrySet());
        entries.sort(Map.Entry.<String, Double>comparingByValue().reversed());
        Map<String, Double> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : entries) sorted.put(e.getKey(), e.getValue());
        return Collections.unmodifiableMap(sorted);
    }

    public Map<String, Double> expenseByCategory(YearMonth ym) {
        return cached("cat:" + key(ym), () -> groupExpense(ym, Transaction::getCategory));
    }

    public Map<String, Double> expenseByPayment(YearMonth ym) {
        return cached("pay:" + key(ym), () -> groupExpense(ym, Transaction::getPaymentMode));
    }

    public double spentIn(String category, YearMonth ym) {
        return expenseByCategory(ym).getOrDefault(category, 0.0);
    }

    public List<MonthSummary> lastMonths(int n, YearMonth end) {
        return cached("months:" + n + ":" + key(end), () -> {
            List<MonthSummary> out = new ArrayList<>();
            for (int i = n - 1; i >= 0; i--) {
                YearMonth ym = end.minusMonths(i);
                out.add(new MonthSummary(ym, total(TransactionType.INCOME, ym), total(TransactionType.EXPENSE, ym)));
            }
            return Collections.unmodifiableList(out);
        });
    }

    /** Daily expense totals for a month (only up to today for the current month). */
    public double[] dailyExpense(YearMonth ym) {
        return cached("daily:" + key(ym), () -> {
            int len = ym.equals(YearMonth.now()) ? LocalDate.now().getDayOfMonth() : ym.lengthOfMonth();
            double[] arr = new double[len];
            for (Transaction t : txns) {
                if (t.isExpense() && in(t, ym)) {
                    int d = t.getDate().getDayOfMonth();
                    if (d <= len) arr[d - 1] += t.getAmount();
                }
            }
            return arr;
        });
    }

    /** Expense per weekday, Monday..Sunday. */
    public double[] weekdayExpense(YearMonth ym) {
        return cached("week:" + key(ym), () -> {
            double[] arr = new double[7];
            for (Transaction t : txns) {
                if (t.isExpense() && in(t, ym)) arr[t.getDate().getDayOfWeek().getValue() - 1] += t.getAmount();
            }
            return arr;
        });
    }

    public List<Transaction> topExpenses(YearMonth ym, int n) {
        return cached("top:" + n + ":" + key(ym), () -> {
            TopN<Transaction> heap = new TopN<>(n, Comparator.comparingDouble(Transaction::getAmount));
            for (Transaction t : txns) if (t.isExpense() && in(t, ym)) heap.offer(t);
            return Collections.unmodifiableList(heap.toSortedList());
        });
    }

    public List<Transaction> recent(int n) {
        return new ArrayList<>(txns.subList(0, Math.min(n, txns.size())));
    }

    /** Projected spend at month end (for the current month) or the actual total (for past months). */
    public double forecast(YearMonth ym) {
        double spent = total(TransactionType.EXPENSE, ym);
        if (!ym.equals(YearMonth.now())) return spent;
        int day = Math.max(1, LocalDate.now().getDayOfMonth());
        return spent / day * ym.lengthOfMonth();
    }

    public double avgDailySpend(YearMonth ym) {
        double spent = total(TransactionType.EXPENSE, ym);
        int days = ym.equals(YearMonth.now()) ? Math.max(1, LocalDate.now().getDayOfMonth()) : ym.lengthOfMonth();
        return spent / days;
    }

    /** 0..100 score from savings rate (60 pts) and budget discipline (40 pts). */
    public int healthScore(YearMonth ym, Map<String, Double> budgets) {
        double inc = total(TransactionType.INCOME, ym);
        double exp = total(TransactionType.EXPENSE, ym);
        if (inc <= 0 && exp <= 0) return 0;
        double rate = inc <= 0 ? 0 : (inc - exp) / inc;
        double savingsPts = Math.max(0, Math.min(1, rate / 0.30)) * 60;
        double budgetPts = 30;
        if (!budgets.isEmpty()) {
            int over = 0;
            for (Map.Entry<String, Double> b : budgets.entrySet()) {
                if (spentIn(b.getKey(), ym) > b.getValue()) over++;
            }
            budgetPts = 40.0 * (1.0 - (double) over / budgets.size());
        }
        return (int) Math.round(Math.max(0, Math.min(100, savingsPts + budgetPts)));
    }

    public List<Insight> insights(YearMonth ym, Map<String, Double> budgets) {
        List<Insight> out = new ArrayList<>();
        double exp = total(TransactionType.EXPENSE, ym);
        double inc = total(TransactionType.INCOME, ym);
        if (exp <= 0 && inc <= 0) {
            out.add(new Insight("info", "No activity", "Nothing recorded for " + ym.format(Fmt.MONTH)
                    + " yet. Add a transaction to unlock insights."));
            return out;
        }

        Map<String, Double> cats = expenseByCategory(ym);
        if (!cats.isEmpty()) {
            Map.Entry<String, Double> top = cats.entrySet().iterator().next();
            out.add(new Insight("info", "Biggest spending category",
                    top.getKey() + " takes " + Fmt.pct(top.getValue() / exp) + " of your spending ("
                            + Fmt.moneyWhole(top.getValue()) + ")."));
        }

        YearMonth prev = ym.minusMonths(1);
        double prevExp = total(TransactionType.EXPENSE, prev);
        if (prevExp > 0 && exp > 0) {
            double change = (exp - prevExp) / prevExp;
            if (change > 0.05) {
                out.add(new Insight("warn", "Spending is up",
                        "You spent " + Fmt.pct(change) + " more than in " + prev.format(Fmt.MONTH) + "."));
            } else if (change < -0.05) {
                out.add(new Insight("good", "Spending is down",
                        "Nice! You spent " + Fmt.pct(-change) + " less than in " + prev.format(Fmt.MONTH) + "."));
            } else {
                out.add(new Insight("info", "Steady month", "Your spending is about the same as last month."));
            }
        }

        if (inc > 0) {
            double rate = (inc - exp) / inc;
            if (rate >= 0.30) {
                out.add(new Insight("good", "Excellent savings",
                        "You are saving " + Fmt.pct(rate) + " of your income — above the 30% goal."));
            } else if (rate >= 0.10) {
                out.add(new Insight("info", "Healthy savings",
                        "You are saving " + Fmt.pct(rate) + " of your income. Aim for 30% to build wealth faster."));
            } else if (rate >= 0) {
                out.add(new Insight("warn", "Low savings",
                        "Only " + Fmt.pct(rate) + " of your income is being saved this month."));
            } else {
                out.add(new Insight("bad", "Overspending",
                        "You spent " + Fmt.moneyWhole(exp - inc) + " more than you earned this month."));
            }
        }

        for (Map.Entry<String, Double> b : budgets.entrySet()) {
            double spent = spentIn(b.getKey(), ym);
            double pct = b.getValue() <= 0 ? 0 : spent / b.getValue();
            if (pct >= 1.0) {
                out.add(new Insight("bad", b.getKey() + " budget exceeded",
                        "Spent " + Fmt.moneyWhole(spent) + " against a limit of " + Fmt.moneyWhole(b.getValue()) + "."));
            } else if (pct >= 0.8) {
                out.add(new Insight("warn", b.getKey() + " nearly used",
                        Fmt.pct(pct) + " of the " + Fmt.moneyWhole(b.getValue()) + " budget is gone."));
            }
        }

        if (ym.equals(YearMonth.now()) && exp > 0) {
            double f = forecast(ym);
            out.add(new Insight(inc > 0 && f > inc ? "warn" : "info", "Month-end forecast",
                    "At this pace you will spend about " + Fmt.moneyWhole(f) + " by " +
                            ym.atEndOfMonth().format(Fmt.DATE) + "."));
        }

        double[] week = weekdayExpense(ym);
        int best = 0;
        for (int i = 1; i < 7; i++) if (week[i] > week[best]) best = i;
        if (week[best] > 0) {
            String day = DayOfWeek.of(best + 1).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
            out.add(new Insight("info", "Priciest weekday",
                    day + "s are your heaviest spending day (" + Fmt.moneyWhole(week[best]) + " in total)."));
        }

        List<Transaction> top = topExpenses(ym, 1);
        if (!top.isEmpty()) {
            Transaction t = top.get(0);
            out.add(new Insight("info", "Largest single expense",
                    Fmt.moneyWhole(t.getAmount()) + " on " + shortDesc(t) + " (" + t.getDate().format(Fmt.DATE) + ")."));
        }
        return out;
    }
}
