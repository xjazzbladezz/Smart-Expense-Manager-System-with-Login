package com.smartexpense.ui;

import com.smartexpense.model.Categories;
import com.smartexpense.service.AppContext;
import com.smartexpense.service.BudgetService;
import com.smartexpense.service.ExpenseService;
import com.smartexpense.ui.components.FancyButton;
import com.smartexpense.ui.components.RoundedPanel;
import com.smartexpense.ui.components.ScrollBody;
import com.smartexpense.ui.components.Toast;
import com.smartexpense.util.Fmt;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Per-category monthly budgets with animated progress bars and alerts. */
public class BudgetPanel extends JPanel {

    private final ExpenseService svc = AppContext.expenses();
    private final BudgetService bud = AppContext.budgets();
    private final JComboBox<YearMonth> monthBox = new JComboBox<>();
    private final JPanel summaryHolder = new JPanel(new BorderLayout());
    private final JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
    private boolean ready;

    public BudgetPanel() {
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

        FancyButton add = new FancyButton("Set budget", FancyButton.Style.PRIMARY).icon("plus");
        add.addActionListener(e -> editBudget(null));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JLabel hint = new JLabel("Set a monthly limit for each category and get warned before you overspend.");
        hint.setForeground(Theme.MUTED);
        hint.setFont(Theme.font(Font.PLAIN, 13));
        bar.add(hint, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(monthBox);
        right.add(add);
        bar.add(right, BorderLayout.EAST);

        summaryHolder.setOpaque(false);
        grid.setOpaque(false);

        ScrollBody body = new ScrollBody(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 6));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets0(0, 0, 18, 0).i;
        body.add(bar, gc);
        gc.gridy++;
        body.add(summaryHolder, gc);
        gc.gridy++;
        gc.insets = new Insets0(0, 0, 0, 0).i;
        gc.anchor = GridBagConstraints.NORTH;
        body.add(grid, gc);
        gc.gridy++;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        body.add(filler, gc);
        add(ScrollBody.wrap(body), BorderLayout.CENTER);

        ready = true;
        svc.addListener(this::onData);
        bud.addListener(this::onData);
        refresh();
    }

    /** tiny helper so the code above stays readable */
    private static final class Insets0 {
        final java.awt.Insets i;

        Insets0(int t, int l, int b, int r) {
            i = new java.awt.Insets(t, l, b, r);
        }
    }

    private void onData() {
        SwingUtilities.invokeLater(this::refresh);
    }

    public void refresh() {
        if (!ready) return;
        YearMonth ym = (YearMonth) monthBox.getSelectedItem();
        if (ym == null) ym = YearMonth.now();

        // overall summary
        summaryHolder.removeAll();
        double limit = bud.totalLimit();
        double spent = bud.totalSpent(ym);
        summaryHolder.add(new Summary(limit, spent, bud.limits().size()), BorderLayout.CENTER);
        summaryHolder.setPreferredSize(new Dimension(100, 146));

        // cards
        grid.removeAll();
        List<BudgetService.Status> list = bud.statuses(ym);
        for (BudgetService.Status st : list) grid.add(new BudgetCard(st));
        grid.add(new AddCard());
        // keep an even grid: pad with invisible filler so cards never stretch
        int pad = (3 - (grid.getComponentCount() % 3)) % 3;
        for (int i = 0; i < pad; i++) {
            JPanel f = new JPanel();
            f.setOpaque(false);
            grid.add(f);
        }
        summaryHolder.revalidate();
        summaryHolder.repaint();
        grid.revalidate();
        grid.repaint();
    }

    // ------------------------------------------------------------------ dialog

    private void editBudget(String preset) {
        JComboBox<String> cat = new JComboBox<>(Categories.EXPENSE.toArray(new String[0]));
        if (preset != null) {
            cat.setSelectedItem(preset);
            cat.setEnabled(false);
        }
        JTextField limit = new JTextField(preset != null && bud.limits().containsKey(preset)
                ? String.valueOf(Math.round(bud.limits().get(preset))) : "");
        limit.putClientProperty("JTextField.placeholderText", "e.g. 8000");

        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(new JLabel("Category"));
        p.add(cat);
        p.add(new JLabel("Monthly limit (₹)"));
        p.add(limit);
        p.setPreferredSize(new Dimension(300, 150));

        int ok = JOptionPane.showConfirmDialog(this, p, preset == null ? "Set budget" : "Edit budget",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            double v = Double.parseDouble(limit.getText().trim().replace(",", ""));
            if (v <= 0) throw new NumberFormatException();
            bud.set(String.valueOf(cat.getSelectedItem()), v);
            Toast.show(this, "Budget saved", Toast.Kind.SUCCESS);
        } catch (NumberFormatException ex) {
            Toast.show(this, "Enter a valid positive amount", Toast.Kind.ERROR);
        } catch (Exception ex) {
            Toast.show(this, "Error: " + ex.getMessage(), Toast.Kind.ERROR);
        }
    }

    private void removeBudget(String category) {
        int ok = JOptionPane.showConfirmDialog(this, "Remove the budget for " + category + "?", "Remove budget",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            bud.remove(category);
            Toast.show(this, "Budget removed", Toast.Kind.INFO);
        } catch (Exception ex) {
            Toast.show(this, "Error: " + ex.getMessage(), Toast.Kind.ERROR);
        }
    }

    // ------------------------------------------------------------------ widgets

    private abstract static class Animated extends RoundedPanel {
        protected float progress;
        private Timer timer;

        Animated() {
            super(null);
        }

        void play() {
            progress = 0;
            final long start = System.nanoTime();
            timer = new Timer(15, null);
            timer.addActionListener(e -> {
                float t = Math.min(1f, (System.nanoTime() - start) / 1e6f / 800f);
                progress = 1f - (float) Math.pow(1 - t, 3);
                if (t >= 1f) timer.stop();
                repaint();
            });
            timer.start();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            play();
        }

        @Override
        public void removeNotify() {
            if (timer != null) timer.stop();
            super.removeNotify();
        }
    }

    private static Color statusColor(double ratio) {
        if (ratio >= 1.0) return Theme.RED;
        if (ratio >= 0.8) return Theme.AMBER;
        return Theme.GREEN;
    }

    private static void bar(Graphics2D g, int x, int y, int w, int h, double ratio, float progress, Color c) {
        g.setColor(Theme.CARD_ALT);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        double fill = Math.min(1.0, ratio) * progress;
        if (fill > 0.001) {
            double fw = Math.max(h, w * fill);
            g.setPaint(new GradientPaint(x, 0, c, x + (float) fw, 0, Theme.mix(c, Color.WHITE, 0.25f)));
            g.fill(new RoundRectangle2D.Double(x, y, fw, h, h, h));
        }
    }

    /** Big overall tile. */
    private static final class Summary extends Animated {
        private final double limit;
        private final double spent;
        private final int count;

        Summary(double limit, double spent, int count) {
            this.limit = limit;
            this.spent = spent;
            this.count = count;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            g.setFont(Theme.font(Font.PLAIN, 13));
            g.setColor(Theme.MUTED);
            g.drawString("Total monthly budget  ·  " + count + (count == 1 ? " category" : " categories"), 26, 36);

            if (count == 0) {
                g.setFont(Theme.font(Font.BOLD, 20));
                g.setColor(Theme.TEXT);
                g.drawString("No budgets yet", 26, 74);
                g.setFont(Theme.font(Font.PLAIN, 13));
                g.setColor(Theme.MUTED);
                g.drawString("Click \"Set budget\" to create your first category limit.", 26, 100);
                g.dispose();
                return;
            }
            double ratio = limit <= 0 ? 0 : spent / limit;
            Color c = statusColor(ratio);
            g.setFont(Theme.font(Font.BOLD, 28));
            g.setColor(Theme.TEXT);
            String left = Fmt.moneyWhole(spent * progress);
            g.drawString(left, 26, 76);
            FontMetrics fm = g.getFontMetrics();
            g.setFont(Theme.font(Font.PLAIN, 14));
            g.setColor(Theme.MUTED);
            g.drawString("of " + Fmt.moneyWhole(limit), 26 + fm.stringWidth(left) + 10, 76);

            String pct = Fmt.pct(ratio);
            g.setFont(Theme.font(Font.BOLD, 22));
            g.setColor(c);
            fm = g.getFontMetrics();
            g.drawString(pct, w - fm.stringWidth(pct) - 26, 76);

            bar(g, 26, 94, w - 52, 12, ratio, progress, c);
            g.setFont(Theme.font(Font.PLAIN, 12));
            g.setColor(Theme.MUTED);
            double rem = limit - spent;
            g.drawString(rem >= 0 ? Fmt.moneyWhole(rem) + " left to spend" : Fmt.moneyWhole(-rem) + " over the limit", 26, 124);
            g.dispose();
        }
    }

    /** One category tile. */
    private final class BudgetCard extends Animated {
        private final BudgetService.Status st;
        private boolean hover;

        BudgetCard(BudgetService.Status st) {
            this.st = st;
            setPreferredSize(new Dimension(300, 176));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setLayout(null);

            FancyButton edit = new FancyButton("", FancyButton.Style.GHOST).icon("edit").compact();
            FancyButton del = new FancyButton("", FancyButton.Style.GHOST).icon("trash").compact();
            edit.setBounds(0, 0, 32, 32);
            del.setBounds(0, 0, 32, 32);
            edit.setToolTipText("Edit limit");
            del.setToolTipText("Remove budget");
            edit.addActionListener(e -> editBudget(st.category()));
            del.addActionListener(e -> removeBudget(st.category()));
            add(edit);
            add(del);
            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    del.setLocation(getWidth() - 44, 12);
                    edit.setLocation(getWidth() - 78, 12);
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = getMousePosition() != null;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            double ratio = st.ratio();
            Color c = statusColor(ratio);
            Color cat = Categories.color(st.category());

            if (hover) {
                g.setColor(Theme.alpha(cat, 90));
                g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, getHeight() - 1, 22, 22));
            }
            g.setColor(Theme.alpha(cat, 45));
            g.fill(new RoundRectangle2D.Double(20, 20, 40, 40, 14, 14));
            g.setColor(cat);
            g.setFont(Theme.font(Font.BOLD, 17));
            String ch = st.category().substring(0, 1);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ch, 20 + (40 - fm.stringWidth(ch)) / 2, 20 + (40 - fm.getHeight()) / 2 + fm.getAscent());

            g.setFont(Theme.font(Font.BOLD, 15));
            g.setColor(Theme.TEXT);
            g.drawString(st.category(), 72, 38);
            String tag = ratio >= 1 ? "Over budget" : ratio >= 0.8 ? "Almost there" : "On track";
            g.setFont(Theme.font(Font.BOLD, 11));
            fm = g.getFontMetrics();
            int tw = fm.stringWidth(tag) + 16;
            g.setColor(Theme.alpha(c, 40));
            g.fill(new RoundRectangle2D.Double(72, 46, tw, 20, 20, 20));
            g.setColor(c);
            g.drawString(tag, 80, 60);

            g.setFont(Theme.font(Font.BOLD, 22));
            g.setColor(Theme.TEXT);
            String spent = Fmt.moneyWhole(st.spent() * progress);
            g.drawString(spent, 22, 108);
            fm = g.getFontMetrics();
            g.setFont(Theme.font(Font.PLAIN, 13));
            g.setColor(Theme.MUTED);
            g.drawString("/ " + Fmt.moneyWhole(st.limit()), 22 + fm.stringWidth(spent) + 8, 108);

            String pct = Fmt.pct(ratio);
            g.setFont(Theme.font(Font.BOLD, 15));
            g.setColor(c);
            fm = g.getFontMetrics();
            g.drawString(pct, w - fm.stringWidth(pct) - 22, 108);

            bar(g, 22, 120, w - 44, 10, ratio, progress, c);

            g.setFont(Theme.font(Font.PLAIN, 12));
            g.setColor(Theme.MUTED);
            double rem = st.remaining();
            g.drawString(rem >= 0 ? Fmt.moneyWhole(rem) + " remaining" : Fmt.moneyWhole(-rem) + " over limit", 22, 154);
            g.dispose();
        }
    }

    /** "+ New budget" tile. */
    private final class AddCard extends RoundedPanel {
        private boolean hover;

        AddCard() {
            super(null);
            fill(Theme.alpha(Theme.CARD, 120));
            setPreferredSize(new Dimension(300, 176));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    editBudget(null);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            Theme.aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setStroke(new java.awt.BasicStroke(1.5f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND,
                    10f, new float[]{6f, 6f}, 0f));
            g.setColor(hover ? Theme.ACCENT : Theme.BORDER);
            g.draw(new RoundRectangle2D.Double(8, 8, w - 16, h - 16, 18, 18));
            Icons.draw(g, "plus", w / 2 - 14, h / 2 - 28, 28, hover ? Theme.ACCENT : Theme.MUTED);
            g.setFont(Theme.font(Font.BOLD, 13));
            g.setColor(hover ? Theme.TEXT : Theme.MUTED);
            String s = "New budget";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, (w - fm.stringWidth(s)) / 2, h / 2 + 30);
            g.dispose();
        }
    }

    @SuppressWarnings("unused")
    private static List<JComponent> unused() {
        return new ArrayList<>();
    }
}
