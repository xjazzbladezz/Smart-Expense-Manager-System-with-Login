package com.smartexpense.ui;

import com.smartexpense.model.Transaction;
import com.smartexpense.db.DBConnection;
import com.smartexpense.service.AppContext;
import com.smartexpense.service.DataSeeder;
import com.smartexpense.service.ExcelService;
import com.smartexpense.service.ExpenseService;
import com.smartexpense.ui.components.FancyButton;
import com.smartexpense.ui.components.RoundedPanel;
import com.smartexpense.ui.components.ScrollBody;
import com.smartexpense.ui.components.Toast;
import com.smartexpense.util.Fmt;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/** Excel import / export, demo data and a live look at the in-memory data structures. */
public class DataPanel extends JPanel {

    private final ExpenseService svc = AppContext.expenses();
    private final JLabel statTxn = value();
    private final JLabel statTrie = value();
    private final JLabel statCache = value();
    private final JLabel statHit = value();
    private final JLabel statUndo = value();
    private final JLabel statRedo = value();

    public DataPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        ScrollBody body = new ScrollBody(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 6));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 18, 0);

        // ---- Excel
        JPanel excel = new JPanel(new GridLayout(1, 3, 14, 0));
        excel.setOpaque(false);
        excel.add(action("Import from Excel", "Read an .xlsx file with columns Date, Description, Category, "
                        + "Payment Mode, Type, Amount and add every valid row.", "Choose file…", "upload",
                FancyButton.Style.PRIMARY, this::importExcel));
        excel.add(action("Export to Excel", "Save all your transactions as a styled workbook with a Summary sheet "
                        + "that uses live SUMIFS formulas.", "Export .xlsx", "download",
                FancyButton.Style.PRIMARY, this::exportExcel));
        excel.add(action("Download template", "Get a ready-to-fill sample sheet so your own data imports "
                        + "perfectly the first time.", "Save template", "file",
                FancyButton.Style.SECONDARY, this::saveTemplate));
        body.add(section("Excel data", "Move data in and out of the app using Apache POI.", excel), gc);

        // ---- Demo / maintenance
        gc.gridy++;
        JPanel maint = new JPanel(new GridLayout(1, 2, 14, 0));
        maint.setOpaque(false);
        maint.add(action("Load demo data", "Generate six months of realistic income and spending so you can "
                        + "explore the charts instantly.", "Generate demo data", "spark",
                FancyButton.Style.SECONDARY, this::loadDemo));
        maint.add(action("Clear all transactions", "Permanently delete every transaction for this account. "
                        + "Budgets are kept.", "Delete everything", "trash",
                FancyButton.Style.DANGER, this::clearAll));
        body.add(section("Data tools", "Seed or reset your workspace.", maint), gc);

        // ---- Internals
        gc.gridy++;
        gc.insets = new Insets(0, 0, 0, 0);
        JPanel stats = new JPanel(new GridLayout(2, 3, 14, 14));
        stats.setOpaque(false);
        stats.add(stat("Transactions in memory", statTxn));
        stats.add(stat("Trie words (auto-suggest)", statTrie));
        stats.add(stat("LRU cache entries", statCache));
        stats.add(stat("Cache hit rate", statHit));
        stats.add(stat("Undo stack depth", statUndo));
        stats.add(stat("Redo stack depth", statRedo));
        body.add(section("Under the hood", "Live view of the data structures powering the app  ·  MySQL at "
                + DBConnection.describe(), stats), gc);

        add(ScrollBody.wrap(body), BorderLayout.CENTER);
        svc.addListener(() -> SwingUtilities.invokeLater(this::refresh));
        refresh();
    }

    private static JLabel value() {
        JLabel l = new JLabel("0");
        l.setFont(Theme.font(Font.BOLD, 24));
        l.setForeground(Theme.TEXT);
        return l;
    }

    private static JPanel stat(String name, JLabel value) {
        RoundedPanel p = new RoundedPanel(new BorderLayout(0, 4));
        p.fill(Theme.CARD_ALT);
        p.arc(18);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel n = new JLabel(name);
        n.setFont(Theme.font(Font.PLAIN, 12));
        n.setForeground(Theme.MUTED);
        p.add(n, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private static JPanel section(String title, String sub, JPanel content) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 22, 22));
        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 16));
        t.setForeground(Theme.TEXT);
        JLabel s = new JLabel(sub);
        s.setFont(Theme.font(Font.PLAIN, 12));
        s.setForeground(Theme.MUTED);
        head.add(t, BorderLayout.NORTH);
        head.add(s, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static JPanel action(String title, String text, String btnText, String icon,
                                 FancyButton.Style style, Runnable r) {
        RoundedPanel p = new RoundedPanel(new BorderLayout(0, 10));
        p.fill(Theme.CARD_ALT);
        p.arc(18);
        p.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 14));
        t.setForeground(Theme.TEXT);
        JLabel d = new JLabel("<html><body style='width:230px'>" + text + "</body></html>");
        d.setFont(Theme.font(Font.PLAIN, 12));
        d.setForeground(Theme.MUTED);
        FancyButton b = new FancyButton(btnText, style).icon(icon);
        b.addActionListener(e -> r.run());
        JPanel south = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        south.setOpaque(false);
        south.add(b);
        p.add(t, BorderLayout.NORTH);
        p.add(d, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(260, 170));
        return p;
    }

    private void refresh() {
        statTxn.setText(String.valueOf(svc.getAll().size()));
        statTrie.setText(String.valueOf(svc.trieSize()));
        statCache.setText(String.valueOf(svc.cacheSize()));
        statHit.setText(Fmt.pct(svc.cacheHitRate()));
        statUndo.setText(String.valueOf(svc.undoDepth()));
        statRedo.setText(String.valueOf(svc.redoDepth()));
    }

    // ------------------------------------------------------------------ actions

    private File choose(boolean save, String name) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Excel workbook (*.xlsx)", "xlsx"));
        if (save) {
            fc.setSelectedFile(new File(name));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".xlsx")) f = new File(f.getParentFile(), f.getName() + ".xlsx");
            return f;
        }
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        return fc.getSelectedFile();
    }

    private void importExcel() {
        File f = choose(false, null);
        if (f == null) return;
        new SwingWorker<ExcelService.ImportResult, Void>() {
            @Override
            protected ExcelService.ImportResult doInBackground() throws Exception {
                return ExcelService.importFile(f);
            }

            @Override
            protected void done() {
                try {
                    ExcelService.ImportResult r = get();
                    svc.addAll(r.imported());
                    Toast.show(DataPanel.this, "Imported " + r.imported().size() + " transactions",
                            r.imported().isEmpty() ? Toast.Kind.WARN : Toast.Kind.SUCCESS);
                    if (!r.errors().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        int n = 0;
                        for (String e : r.errors()) {
                            if (n++ >= 12) {
                                sb.append("…and ").append(r.errors().size() - 12).append(" more");
                                break;
                            }
                            sb.append(e).append('\n');
                        }
                        JOptionPane.showMessageDialog(DataPanel.this, sb.toString(),
                                r.errors().size() + " rows skipped", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    Toast.show(DataPanel.this, "Import failed: " + rootMessage(ex), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private void exportExcel() {
        List<Transaction> all = svc.getAll();
        if (all.isEmpty()) {
            Toast.show(this, "Nothing to export yet", Toast.Kind.INFO);
            return;
        }
        File f = choose(true, "transactions_" + LocalDate.now() + ".xlsx");
        if (f == null) return;
        try {
            ExcelService.export(all, f);
            Toast.show(this, "Exported " + all.size() + " rows to " + f.getName(), Toast.Kind.SUCCESS);
        } catch (Exception ex) {
            Toast.show(this, "Export failed: " + rootMessage(ex), Toast.Kind.ERROR);
        }
    }

    private void saveTemplate() {
        File f = choose(true, "import_template.xlsx");
        if (f == null) return;
        try {
            ExcelService.writeTemplate(f);
            Toast.show(this, "Template saved: " + f.getName(), Toast.Kind.SUCCESS);
        } catch (Exception ex) {
            Toast.show(this, "Could not save: " + rootMessage(ex), Toast.Kind.ERROR);
        }
    }

    private void loadDemo() {
        new SwingWorker<List<Transaction>, Void>() {
            @Override
            protected List<Transaction> doInBackground() {
                return DataSeeder.generate(svc.getUserId());
            }

            @Override
            protected void done() {
                try {
                    svc.addAll(get());
                    Toast.show(DataPanel.this, "Demo data loaded", Toast.Kind.SUCCESS);
                } catch (Exception ex) {
                    Toast.show(DataPanel.this, "Failed: " + rootMessage(ex), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private void clearAll() {
        int ok = JOptionPane.showConfirmDialog(this,
                "This permanently deletes ALL your transactions.\nThis cannot be undone. Continue?",
                "Clear all data", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            svc.clearAll();
            Toast.show(this, "All transactions deleted", Toast.Kind.WARN);
        } catch (Exception ex) {
            Toast.show(this, "Failed: " + rootMessage(ex), Toast.Kind.ERROR);
        }
    }

    static String rootMessage(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
}
