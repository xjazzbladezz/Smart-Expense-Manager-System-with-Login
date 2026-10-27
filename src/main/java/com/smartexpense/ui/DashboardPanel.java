package com.smartexpense.ui;

import com.smartexpense.model.TransactionType;
import com.smartexpense.service.AppContext;
import com.smartexpense.service.BudgetService;
import com.smartexpense.service.ExpenseService;
import com.smartexpense.ui.charts.BarChart;
import com.smartexpense.ui.charts.DonutChart;
import com.smartexpense.ui.charts.GaugeChart;
import com.smartexpense.ui.charts.LineChart;
import com.smartexpense.ui.components.Card;
import com.smartexpense.ui.components.ScrollBody;
import com.smartexpense.ui.components.StatCard;
import com.smartexpense.ui.components.TxnListPanel;
import com.smartexpense.util.Fmt;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final ExpenseService svc = AppContext.expenses();
    private final BudgetService bud = AppContext.budgets();

    private final JComboBox<YearMonth> monthBox = new JComboBox<>();
    private final StatCard balanceCard = new StatCard("Total Balance", "wallet", Theme.ACCENT);
    private final StatCard incomeCard = new StatCard("Income", "download", Theme.GREEN);
    private final StatCard expenseCard = new StatCard("Expenses", "upload", Theme.RED);
    private final StatCard savingsCard = new StatCard("Savings Rate", "trend", Theme.ACCENT_2);
    private final BarChart barChart = new BarChart();
    private final DonutChart donut = new DonutChart();
    private final LineChart line = new LineChart();
    private final GaugeChart gauge = new GaugeChart();
    private final TxnListPanel recent = new TxnListPanel("No transactions yet — add your first one!");
    private final TxnListPanel top = new TxnListPanel("No expenses this month.");
    private final JLabel forecastValue = metric();
    private final JLabel avgValue = metric();
    private final JLabel budgetValue = metric();
    private Card donutCard;
    private Card lineCard;
    private Card topCard;
    private boolean ready;

    public DashboardPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) monthBox.addItem(now.minusMonths(i));
        monthBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                                   boolean sel, boolean focus) {
                java.awt.Component c = super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof YearMonth ym) setText(ym.format(Fmt.MONTH));
                return c;
            }
        });
        monthBox.setPreferredSize(new Dimension(190, 38));
        monthBox.addActionListener(e -> {
            if (ready) refresh();
        });

        ScrollBody body = new ScrollBody(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 6));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.insets = new Insets(0, 0, 18, 0);

        // period selector row
        JPanel pick = new JPanel(new BorderLayout());
        pick.setOpaque(false);
        JLabel hint = new JLabel("Overview for");
        hint.setForeground(Theme.MUTED);
        hint.setFont(Theme.font(Font.PLAIN, 13));
        JPanel pickRight = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        pickRight.setOpaque(false);
        pickRight.add(hint);
        pickRight.add(monthBox);
        pick.add(pickRight, BorderLayout.EAST);
        body.add(pick, gc);

        // KPI row
        gc.gridy++;
        JPanel kpis = new JPanel(new GridLayout(1, 4, 18, 0));
        kpis.setOpaque(false);
        kpis.add(balanceCard);
        kpis.add(incomeCard);
        kpis.add(expenseCard);
        kpis.add(savingsCard);
        body.add(kpis, gc);

        // charts row 1
        gc.gridy++;
        body.add(row(new Card("Income vs Expense", "Last 6 months", barChart),
                donutCard = new Card("Spending by Category", " ", donut), 0.62, 340), gc);

        // charts row 2
        gc.gridy++;
        body.add(row(lineCard = new Card("Daily Spending", " ", line),
                new Card("Financial Health", "Savings + budget discipline", healthBody()), 0.62, 320), gc);

        // lists row
        gc.gridy++;
        gc.insets = new Insets(0, 0, 0, 0);
        body.add(row(new Card("Recent Transactions", "Latest activity", recent),
                topCard = new Card("Top Expenses", " ", top), 0.5, 0), gc);

        JScrollPane sp = ScrollBody.wrap(body);
        add(sp, BorderLayout.CENTER);

        ready = true;
        svc.addListener(this::onData);
        bud.addListener(this::onData);
        refresh();
    }

    private void onData() {
        javax.swing.SwingUtilities.invokeLater(this::refresh);
    }

    private static JLabel metric() {
        JLabel l = new JLabel("-");
        l.setFont(Theme.font(Font.BOLD, 14));
        l.setForeground(Theme.TEXT);
        return l;
    }

    private JPanel healthBody() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(gauge, BorderLayout.CENTER);

        JPanel stats = new JPanel(new GridLayout(3, 2, 8, 6));
        stats.setOpaque(false);
        stats.add(small("Projected month-end spend"));
        stats.add(forecastValue);
        stats.add(small("Average daily spend"));
        stats.add(avgValue);
        stats.add(small("Budgets on track"));
        stats.add(budgetValue);
        forecastValue.setHorizontalAlignment(JLabel.RIGHT);
        avgValue.setHorizontalAlignment(JLabel.RIGHT);
        budgetValue.setHorizontalAlignment(JLabel.RIGHT);
        p.add(stats, BorderLayout.SOUTH);
        return p;
    }

    private static JLabel small(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.font(Font.PLAIN, 12));
        l.setForeground(Theme.MUTED);
        return l;
    }

    private static JPanel row(JPanel left, JPanel right, double leftWeight, int height) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = leftWeight;
        c.insets = new Insets(0, 0, 0, 9);
        p.add(left, c);
        c.gridx = 1;
        c.weightx = 1 - leftWeight;
        c.insets = new Insets(0, 9, 0, 0);
        p.add(right, c);
        if (height > 0) p.setPreferredSize(new Dimension(900, height));
        return p;
    }

    public void refresh() {
        if (!ready) return;
        YearMonth ym = (YearMonth) monthBox.getSelectedItem();
        if (ym == null) ym = YearMonth.now();
        YearMonth prev = ym.minusMonths(1);

        double inc = svc.total(TransactionType.INCOME, ym);
        double exp = svc.total(TransactionType.EXPENSE, ym);
        double pInc = svc.total(TransactionType.INCOME, prev);
        double pExp = svc.total(TransactionType.EXPENSE, prev);

        balanceCard.setValue(svc.balance(), Fmt::money, "All-time net worth",
                svc.balance() >= 0 ? Theme.GREEN : Theme.RED);
        incomeCard.setValue(inc, Fmt::moneyWhole, delta(inc, pInc, true), deltaColor(inc, pInc, true));
        expenseCard.setValue(exp, Fmt::moneyWhole, delta(exp, pExp, false), deltaColor(exp, pExp, false));
        double rate = svc.savingsRate(ym);
        savingsCard.setValue(rate * 100, v -> String.format("%.1f%%", v),
                "Saved " + Fmt.moneyWhole(Math.max(0, inc - exp)), rate >= 0.2 ? Theme.GREEN : Theme.MUTED);

        // bar chart: last 6 months
        List<ExpenseService.MonthSummary> months = svc.lastMonths(6, ym);
        List<String> labels = new ArrayList<>();
        double[] a = new double[months.size()];
        double[] b = new double[months.size()];
        for (int i = 0; i < months.size(); i++) {
            labels.add(months.get(i).month().format(Fmt.MONTH_SHORT));
            a[i] = months.get(i).income();
            b[i] = months.get(i).expense();
        }
        barChart.setData(labels, a, b);

        donut.setData(svc.expenseByCategory(ym), "Spent", exp);
        donutCard.setSubtitle(ym.format(Fmt.MONTH));

        double[] daily = svc.dailyExpense(ym);
        String[] dl = new String[daily.length];
        for (int i = 0; i < dl.length; i++) dl[i] = ym.atDay(i + 1).format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH));
        line.setData("Spent", dl, daily, Theme.ACCENT);
        lineCard.setSubtitle("Day-by-day expenses in " + ym.format(Fmt.MONTH));

        int score = svc.healthScore(ym, bud.limits());
        gauge.setScore(score, verdict(score, inc, exp));
        forecastValue.setText(Fmt.moneyWhole(svc.forecast(ym)));
        avgValue.setText(Fmt.moneyWhole(svc.avgDailySpend(ym)));
        budgetValue.setText(bud.limits().isEmpty() ? "No budgets set" : bud.onTrackCount(ym) + " of " + bud.limits().size());

        recent.setData(svc.recent(6), false);
        top.setData(svc.topExpenses(ym, 6), true);
        topCard.setSubtitle("Biggest spends in " + ym.format(Fmt.MONTH));
    }

    private static String verdict(int score, double inc, double exp) {
        if (inc <= 0 && exp <= 0) return "No data";
        if (score >= 75) return "Excellent";
        if (score >= 50) return "Good";
        if (score >= 30) return "Needs attention";
        return "Critical";
    }

    private static String delta(double cur, double prev, boolean higherIsGood) {
        if (prev <= 0) return cur > 0 ? "No data last month" : " ";
        double d = (cur - prev) / prev;
        String arrow = d >= 0 ? "▲ " : "▼ ";
        return arrow + Fmt.pct(Math.abs(d)) + " vs last month";
    }

    private static Color deltaColor(double cur, double prev, boolean higherIsGood) {
        if (prev <= 0) return Theme.MUTED;
        boolean up = cur >= prev;
        return (up == higherIsGood) ? Theme.GREEN : Theme.RED;
    }
}
