package com.smartexpense.ui;

import com.smartexpense.service.AppContext;
import com.smartexpense.service.BudgetService;
import com.smartexpense.service.ExpenseService;
import com.smartexpense.ui.charts.BarChart;
import com.smartexpense.ui.charts.DonutChart;
import com.smartexpense.ui.charts.LineChart;
import com.smartexpense.ui.components.Card;
import com.smartexpense.ui.components.RoundedPanel;
import com.smartexpense.ui.components.ScrollBody;
import com.smartexpense.util.Fmt;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.RoundRectangle2D;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Insights, weekday pattern, payment-mode split and 12-month savings trend. */
public class AnalyticsPanel extends JPanel {

    private final ExpenseService svc = AppContext.expenses();
    private final BudgetService bud = AppContext.budgets();
    private final JComboBox<YearMonth> monthBox = new JComboBox<>();
    private final JPanel insightList = new JPanel();
    private final BarChart weekChart = new BarChart();
    private final DonutChart payChart = new DonutChart();
    private final LineChart trend = new LineChart();
    private Card insightCard;
    private boolean ready;

    public AnalyticsPanel() {
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
        monthBox.setPreferredSize(new Dimension(190, 40));
        monthBox.addActionListener(e -> {
            if (ready) refresh();
        });

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JLabel hint = new JLabel("Smart insights generated from your spending patterns.");
        hint.setForeground(Theme.MUTED);
        hint.setFont(Theme.font(Font.PLAIN, 13));
        bar.add(hint, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(monthBox);
        bar.add(right, BorderLayout.EAST);

        insightList.setOpaque(false);
        insightList.setLayout(new BoxLayout(insightList, BoxLayout.Y_AXIS));
        insightCard = new Card("Insights", " ", insightList);

        ScrollBody body = new ScrollBody(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 6));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(0, 0, 18, 0);
        body.add(bar, gc);

        gc.gridy++;
        body.add(insightCard, gc);

        gc.gridy++;
        body.add(twin(new Card("Spending by Weekday", "Which days cost you most", weekChart),
                new Card("Payment Modes", "How you pay", payChart), 0.55, 300), gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 0, 0);
        Card trendCard = new Card("Net Savings Trend", "Income minus expenses, last 12 months", trend);
        trendCard.setPreferredSize(new Dimension(900, 320));
        body.add(trendCard, gc);

        add(ScrollBody.wrap(body), BorderLayout.CENTER);

        ready = true;
        svc.addListener(() -> SwingUtilities.invokeLater(this::refresh));
        bud.addListener(() -> SwingUtilities.invokeLater(this::refresh));
        refresh();
    }

    private static JPanel twin(JPanel l, JPanel r, double lw, int height) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = lw;
        c.insets = new Insets(0, 0, 0, 9);
        p.add(l, c);
        c.gridx = 1;
        c.weightx = 1 - lw;
        c.insets = new Insets(0, 9, 0, 0);
        p.add(r, c);
        p.setPreferredSize(new Dimension(900, height));
        return p;
    }

    public void refresh() {
        if (!ready) return;
        YearMonth ym = (YearMonth) monthBox.getSelectedItem();
        if (ym == null) ym = YearMonth.now();

        insightList.removeAll();
        List<ExpenseService.Insight> list = svc.insights(ym, bud.limits());
        for (ExpenseService.Insight in : list) {
            InsightRow row = new InsightRow(in);
            row.setAlignmentX(LEFT_ALIGNMENT);
            insightList.add(row);
            insightList.add(javax.swing.Box.createVerticalStrut(8));
        }
        insightCard.setSubtitle(list.size() + " observations for " + ym.format(Fmt.MONTH));
        insightList.revalidate();
        insightList.repaint();

        double[] wk = svc.weekdayExpense(ym);
        weekChart.setSeries("Spent", Theme.ACCENT, null, null);
        weekChart.setData(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), wk, null);

        payChart.setData(svc.expenseByPayment(ym), "Spent", svc.total(com.smartexpense.model.TransactionType.EXPENSE, ym));

        List<ExpenseService.MonthSummary> ms = svc.lastMonths(12, YearMonth.now());
        String[] labels = new String[ms.size()];
        double[] vals = new double[ms.size()];
        for (int i = 0; i < ms.size(); i++) {
            labels[i] = ms.get(i).month().format(Fmt.MONTH_YEAR_SHORT);
            vals[i] = ms.get(i).net();
        }
        trend.setData("Net savings", labels, vals, Theme.ACCENT_2);
    }

    private static final class InsightRow extends RoundedPanel {
        private final ExpenseService.Insight in;
        private final Color color;
        private final String icon;

        InsightRow(ExpenseService.Insight in) {
            super(null);
            this.in = in;
            arc(18);
            fill(Theme.CARD_ALT);
            switch (in.kind()) {
                case "good" -> {
                    color = Theme.GREEN;
                    icon = "good";
                }
                case "warn" -> {
                    color = Theme.AMBER;
                    icon = "warn";
                }
                case "bad" -> {
                    color = Theme.RED;
                    icon = "bad";
                }
                default -> {
                    color = Theme.ACCENT_2;
                    icon = "info";
                }
            }
            setPreferredSize(new Dimension(300, 66));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            g.setColor(Theme.alpha(color, 36));
            g.fill(new RoundRectangle2D.Double(14, 13, 40, 40, 14, 14));
            Icons.draw(g, icon, 24, 23, 20, color);
            g.setFont(Theme.font(Font.BOLD, 14));
            g.setColor(Theme.TEXT);
            g.drawString(in.title(), 70, 29);
            g.setFont(Theme.font(Font.PLAIN, 12));
            g.setColor(Theme.MUTED);
            FontMetrics fm = g.getFontMetrics();
            String s = in.text();
            int max = getWidth() - 90;
            while (s.length() > 1 && fm.stringWidth(s) > max) s = s.substring(0, s.length() - 2);
            if (!s.equals(in.text())) s += "…";
            g.drawString(s, 70, 50);
            g.dispose();
        }
    }

    @SuppressWarnings("unused")
    private static List<String> unused() {
        return new ArrayList<>();
    }
}
