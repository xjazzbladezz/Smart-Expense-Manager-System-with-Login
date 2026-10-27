package com.smartexpense.ui;

import com.smartexpense.model.User;
import com.smartexpense.service.AppContext;
import com.smartexpense.service.ExpenseService;
import com.smartexpense.ui.components.FancyButton;
import com.smartexpense.ui.components.NavButton;
import com.smartexpense.ui.components.Toast;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Application shell: sidebar navigation, header with actions, swappable pages. */
public class MainFrame extends JFrame {

    private final ExpenseService svc = AppContext.expenses();
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JLabel pageTitle = new JLabel();
    private final JLabel pageSub = new JLabel();
    private final FancyButton undoBtn = new FancyButton("Undo", FancyButton.Style.SECONDARY).icon("undo");
    private final FancyButton redoBtn = new FancyButton("Redo", FancyButton.Style.SECONDARY).icon("redo");
    private final Map<String, String[]> meta = new LinkedHashMap<>();
    private final Map<String, NavButton> navs = new LinkedHashMap<>();
    private TransactionsPanel transactions;

    public MainFrame(User user) {
        super("SmartSpend — Smart Personal Expense & Budget Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        meta.put("dashboard", new String[]{"Dashboard", "Your money at a glance"});
        meta.put("transactions", new String[]{"Transactions", "Search, filter, edit and manage every entry"});
        meta.put("budgets", new String[]{"Budgets", "Stay inside your monthly limits"});
        meta.put("analytics", new String[]{"Analytics", "Patterns, insights and trends"});
        meta.put("data", new String[]{"Excel Data", "Import, export and manage your data"});

        transactions = new TransactionsPanel();
        pages.setOpaque(false);
        pages.add(new DashboardPanel(), "dashboard");
        pages.add(transactions, "transactions");
        pages.add(new BudgetPanel(), "budgets");
        pages.add(new AnalyticsPanel(), "analytics");
        pages.add(new DataPanel(), "data");

        add(sidebar(user), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(Theme.BG);
        main.setBorder(BorderFactory.createEmptyBorder(24, 30, 10, 24));
        main.add(header(), BorderLayout.NORTH);
        main.add(pages, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        installShortcuts();
        svc.addListener(() -> SwingUtilities.invokeLater(this::updateUndoButtons));
        updateUndoButtons();
        show("dashboard");

        setSize(1440, 900);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ pieces

    private JPanel sidebar(User user) {
        JPanel side = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.SIDEBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER);
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        side.setOpaque(false);
        side.setLayout(new javax.swing.BoxLayout(side, javax.swing.BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(248, 100));
        side.setBorder(BorderFactory.createEmptyBorder(24, 16, 20, 16));

        JPanel brand = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.aa(g2);
                g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT, 42, 42, Theme.ACCENT_2));
                g2.fill(new RoundRectangle2D.Double(6, 2, 42, 42, 14, 14));
                Icons.draw(g2, "wallet", 14, 10, 26, Color.WHITE);
                g2.setFont(Theme.font(Font.BOLD, 20));
                g2.setColor(Theme.TEXT);
                g2.drawString("SmartSpend", 62, 22);
                g2.setFont(Theme.font(Font.PLAIN, 11));
                g2.setColor(Theme.MUTED);
                g2.drawString("Expense & Budget Manager", 62, 38);
                g2.dispose();
            }
        };
        brand.setOpaque(false);
        brand.setPreferredSize(new Dimension(216, 50));
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        brand.setAlignmentX(LEFT_ALIGNMENT);
        side.add(brand);
        side.add(javax.swing.Box.createVerticalStrut(30));

        ButtonGroup group = new ButtonGroup();
        String[][] items = {
                {"dashboard", "Dashboard", "dashboard"},
                {"transactions", "Transactions", "list"},
                {"budgets", "Budgets", "wallet"},
                {"analytics", "Analytics", "chart"},
                {"data", "Excel Data", "data"}};
        for (String[] it : items) {
            NavButton b = new NavButton(it[1], it[2]);
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.addActionListener(e -> show(it[0]));
            if (it[0].equals("dashboard")) b.setSelected(true);
            group.add(b);
            navs.put(it[0], b);
            side.add(b);
            side.add(javax.swing.Box.createVerticalStrut(6));
        }

        side.add(javax.swing.Box.createVerticalGlue());

        // user chip
        JPanel chip = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.aa(g2);
                g2.setColor(Theme.CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 18, 18));
                g2.setColor(Theme.BORDER);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 18, 18));
                g2.setPaint(new GradientPaint(12, 12, Theme.PINK, 52, 52, Theme.ACCENT));
                g2.fill(new Ellipse2D.Double(12, 12, 40, 40));
                g2.setFont(Theme.font(Font.BOLD, 14));
                g2.setColor(Color.WHITE);
                String in = user.initials();
                int sw = g2.getFontMetrics().stringWidth(in);
                g2.drawString(in, 12 + (40 - sw) / 2, 12 + 25);
                g2.setFont(Theme.font(Font.BOLD, 13));
                g2.setColor(Theme.TEXT);
                g2.drawString(clip(user.getFullName(), 18), 62, 30);
                g2.setFont(Theme.font(Font.PLAIN, 11));
                g2.setColor(Theme.MUTED);
                g2.drawString("@" + user.getUsername(), 62, 46);
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setPreferredSize(new Dimension(216, 64));
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        chip.setAlignmentX(LEFT_ALIGNMENT);
        side.add(chip);
        side.add(javax.swing.Box.createVerticalStrut(10));

        FancyButton logout = new FancyButton("Sign out", FancyButton.Style.GHOST).icon("logout");
        logout.setAlignmentX(LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        logout.addActionListener(e -> signOut());
        side.add(logout);
        return side;
    }

    private static String clip(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    private JPanel header() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);

        JPanel left = new JPanel(new BorderLayout(0, 2));
        left.setOpaque(false);
        pageTitle.setFont(Theme.font(Font.BOLD, 28));
        pageTitle.setForeground(Theme.TEXT);
        pageSub.setFont(Theme.font(Font.PLAIN, 13));
        pageSub.setForeground(Theme.MUTED);
        left.add(pageTitle, BorderLayout.NORTH);
        left.add(pageSub, BorderLayout.CENTER);
        h.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel date = new JLabel(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy",
                java.util.Locale.ENGLISH)));
        date.setForeground(Theme.MUTED);
        date.setFont(Theme.font(Font.PLAIN, 13));
        date.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        undoBtn.addActionListener(e -> undo());
        redoBtn.addActionListener(e -> redo());
        undoBtn.setToolTipText("Undo (Ctrl+Z)");
        redoBtn.setToolTipText("Redo (Ctrl+Y)");
        FancyButton add = new FancyButton("Add transaction", FancyButton.Style.PRIMARY).icon("plus");
        add.setToolTipText("New transaction (Ctrl+N)");
        add.addActionListener(e -> newTransaction());
        right.add(date);
        right.add(undoBtn);
        right.add(redoBtn);
        right.add(add);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    // ------------------------------------------------------------------ behaviour

    private void show(String key) {
        cards.show(pages, key);
        NavButton nb = navs.get(key);
        if (nb != null) nb.setSelected(true);
        String[] m = meta.get(key);
        pageTitle.setText(m[0]);
        pageSub.setText(m[1]);
    }

    private void newTransaction() {
        transactions.addNew();
    }

    private void undo() {
        try {
            String l = svc.undo();
            Toast.show(getRootPane(), l == null ? "Nothing to undo" : "Undid: " + l,
                    l == null ? Toast.Kind.INFO : Toast.Kind.SUCCESS);
        } catch (Exception ex) {
            Toast.show(getRootPane(), "Undo failed: " + DataPanel.rootMessage(ex), Toast.Kind.ERROR);
        }
    }

    private void redo() {
        try {
            String l = svc.redo();
            Toast.show(getRootPane(), l == null ? "Nothing to redo" : "Redid: " + l,
                    l == null ? Toast.Kind.INFO : Toast.Kind.SUCCESS);
        } catch (Exception ex) {
            Toast.show(getRootPane(), "Redo failed: " + DataPanel.rootMessage(ex), Toast.Kind.ERROR);
        }
    }

    private void updateUndoButtons() {
        undoBtn.setEnabled(svc.canUndo());
        redoBtn.setEnabled(svc.canRedo());
    }

    private void signOut() {
        AppContext.clear();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void installShortcuts() {
        JComponent root = getRootPane();
        bind(root, "ctrl Z", "undo", this::undo);
        bind(root, "ctrl Y", "redo", this::redo);
        bind(root, "ctrl shift Z", "redo2", this::redo);
        bind(root, "ctrl N", "new", this::newTransaction);
        String[] keys = {"dashboard", "transactions", "budgets", "analytics", "data"};
        for (int i = 0; i < keys.length; i++) {
            final String k = keys[i];
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, java.awt.event.InputEvent.ALT_DOWN_MASK), "go" + i);
            root.getActionMap().put("go" + i, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    show(k);
                }
            });
        }
    }

    private static void bind(JComponent c, String stroke, String name, Runnable r) {
        KeyStroke ks = KeyStroke.getKeyStroke(stroke.replace("ctrl", "control"));
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }
}
