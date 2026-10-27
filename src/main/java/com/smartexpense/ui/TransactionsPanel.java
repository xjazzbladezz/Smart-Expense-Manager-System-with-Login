package com.smartexpense.ui;

import com.smartexpense.model.Categories;
import com.smartexpense.model.Transaction;
import com.smartexpense.model.TransactionType;
import com.smartexpense.service.AppContext;
import com.smartexpense.service.ExpenseService;
import com.smartexpense.ui.components.FancyButton;
import com.smartexpense.ui.components.RoundedPanel;
import com.smartexpense.ui.components.Toast;
import com.smartexpense.util.Fmt;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Searchable, filterable, sortable ledger with add / edit / delete. */
public class TransactionsPanel extends JPanel {

    private static final String ALL = "All";

    private final ExpenseService svc = AppContext.expenses();
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField();
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{ALL, "Income", "Expense"});
    private final JComboBox<String> catBox = new JComboBox<>();
    private final JComboBox<Object> monthBox = new JComboBox<>();
    private final JLabel summary = new JLabel(" ");
    private boolean building;

    public TransactionsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));

        // ---- toolbar
        search.putClientProperty("JTextField.placeholderText", "Search description, category, payment mode…");
        search.putClientProperty("JTextField.leadingIcon", Icons.of("search", 16, Theme.MUTED));
        search.putClientProperty("JTextField.showClearButton", true);
        search.setPreferredSize(new Dimension(330, 40));

        catBox.addItem(ALL);
        for (String c : Categories.EXPENSE) catBox.addItem(c);
        for (String c : Categories.INCOME) catBox.addItem(c);

        YearMonth now = YearMonth.now();
        monthBox.addItem(ALL);
        for (int i = 0; i < 12; i++) monthBox.addItem(now.minusMonths(i));
        monthBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean sel, boolean focus) {
                Component c = super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof YearMonth ym) setText(ym.format(Fmt.MONTH));
                return c;
            }
        });
        for (JComboBox<?> b : List.of(typeBox, catBox, monthBox)) b.setPreferredSize(new Dimension(150, 40));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setOpaque(false);
        filters.add(search);
        filters.add(typeBox);
        filters.add(catBox);
        filters.add(monthBox);

        FancyButton add = new FancyButton("Add", FancyButton.Style.PRIMARY).icon("plus");
        FancyButton edit = new FancyButton("Edit", FancyButton.Style.SECONDARY).icon("edit");
        FancyButton del = new FancyButton("Delete", FancyButton.Style.DANGER).icon("trash");
        add.addActionListener(e -> addNew());
        edit.addActionListener(e -> editSelected());
        del.addActionListener(e -> deleteSelected());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(edit);
        actions.add(del);
        actions.add(add);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(filters, BorderLayout.WEST);
        toolbar.add(actions, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        // ---- table
        styleTable();
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.CARD);
        sp.setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(30);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        card.add(sp, BorderLayout.CENTER);

        summary.setForeground(Theme.MUTED);
        summary.setFont(Theme.font(Font.PLAIN, 12));
        summary.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        card.add(summary, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        // ---- events
        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rebuild();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rebuild();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rebuild();
            }
        };
        search.getDocument().addDocumentListener(dl);
        typeBox.addActionListener(e -> rebuild());
        catBox.addActionListener(e -> rebuild());
        monthBox.addActionListener(e -> rebuild());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) editSelected();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                popup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                popup(e);
            }

            private void popup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && !table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                JPopupMenu m = new JPopupMenu();
                JMenuItem i1 = new JMenuItem("Edit");
                JMenuItem i2 = new JMenuItem("Delete");
                i1.addActionListener(a -> editSelected());
                i2.addActionListener(a -> deleteSelected());
                m.add(i1);
                m.add(i2);
                m.show(table, e.getX(), e.getY());
            }
        });

        svc.addListener(() -> SwingUtilities.invokeLater(this::rebuild));
        rebuild();
    }

    // ------------------------------------------------------------------ table setup

    private void styleTable() {
        table.setRowHeight(52);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setBackground(Theme.CARD);
        table.setFont(Theme.font(Font.PLAIN, 13));
        table.setFocusable(true);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(Theme.font(Font.BOLD, 12));
        header.setPreferredSize(new Dimension(100, 42));

        TableRowSorter<Model> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        int[] widths = {120, 330, 190, 130, 110, 150};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.getColumnModel().getColumn(0).setCellRenderer(new Cell(Cell.Kind.DATE));
        table.getColumnModel().getColumn(1).setCellRenderer(new Cell(Cell.Kind.TEXT));
        table.getColumnModel().getColumn(2).setCellRenderer(new Cell(Cell.Kind.CATEGORY));
        table.getColumnModel().getColumn(3).setCellRenderer(new Cell(Cell.Kind.MUTED));
        table.getColumnModel().getColumn(4).setCellRenderer(new Cell(Cell.Kind.TYPE));
        table.getColumnModel().getColumn(5).setCellRenderer(new Cell(Cell.Kind.AMOUNT));
    }

    // ------------------------------------------------------------------ filtering

    private void rebuild() {
        if (building) return;
        building = true;
        try {
            String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String type = (String) typeBox.getSelectedItem();
            String cat = (String) catBox.getSelectedItem();
            Object mo = monthBox.getSelectedItem();
            YearMonth ym = mo instanceof YearMonth y ? y : null;

            List<Transaction> out = new ArrayList<>();
            double inc = 0;
            double exp = 0;
            for (Transaction t : svc.getAll()) {
                if (!ALL.equals(type) && !t.getType().label().equals(type)) continue;
                if (cat != null && !ALL.equals(cat) && !t.getCategory().equals(cat)) continue;
                if (ym != null && !YearMonth.from(t.getDate()).equals(ym)) continue;
                if (!q.isEmpty()) {
                    String hay = ((t.getDescription() == null ? "" : t.getDescription()) + " " + t.getCategory() + " "
                            + t.getPaymentMode() + " " + t.getDate().format(Fmt.DATE)).toLowerCase();
                    if (!hay.contains(q)) continue;
                }
                out.add(t);
                if (t.isIncome()) inc += t.getAmount();
                else exp += t.getAmount();
            }
            model.setRows(out);
            summary.setText(out.size() + " transactions   ·   Income " + Fmt.moneyWhole(inc)
                    + "   ·   Expenses " + Fmt.moneyWhole(exp) + "   ·   Net " + Fmt.moneyWhole(inc - exp));
        } finally {
            building = false;
        }
    }

    // ------------------------------------------------------------------ actions

    public void addNew() {
        Transaction t = TransactionDialog.show(this, null);
        if (t == null) return;
        try {
            svc.add(t);
            Toast.show(this, "Transaction added", Toast.Kind.SUCCESS);
        } catch (Exception ex) {
            error(ex);
        }
    }

    private List<Transaction> selected() {
        List<Transaction> list = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) {
            list.add(model.rows.get(table.convertRowIndexToModel(viewRow)));
        }
        return list;
    }

    private void editSelected() {
        List<Transaction> sel = selected();
        if (sel.isEmpty()) {
            Toast.show(this, "Select a transaction first", Toast.Kind.INFO);
            return;
        }
        Transaction target = sel.get(0);
        Transaction edited = TransactionDialog.show(this, target);
        if (edited == null) return;
        try {
            svc.update(target, edited);
            Toast.show(this, "Transaction updated", Toast.Kind.SUCCESS);
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void deleteSelected() {
        List<Transaction> sel = selected();
        if (sel.isEmpty()) {
            Toast.show(this, "Select a transaction first", Toast.Kind.INFO);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                sel.size() == 1 ? "Delete this transaction?" : "Delete " + sel.size() + " transactions?",
                "Confirm delete", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            svc.delete(sel);
            Toast.show(this, "Deleted — press Ctrl+Z to undo", Toast.Kind.WARN);
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void error(Exception ex) {
        Toast.show(this, "Error: " + ex.getMessage(), Toast.Kind.ERROR);
    }

    // ------------------------------------------------------------------ model

    private static final class Model extends AbstractTableModel {
        private static final String[] COLS = {"DATE", "DESCRIPTION", "CATEGORY", "PAYMENT", "TYPE", "AMOUNT"};
        private List<Transaction> rows = new ArrayList<>();

        void setRows(List<Transaction> r) {
            this.rows = r;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLS.length;
        }

        @Override
        public String getColumnName(int c) {
            return COLS[c];
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 0 -> LocalDate.class;
                case 5 -> Double.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int r, int c) {
            Transaction t = rows.get(r);
            return switch (c) {
                case 0 -> t.getDate();
                case 1 -> t.getDescription();
                case 2 -> t.getCategory();
                case 3 -> t.getPaymentMode();
                case 4 -> t.getType().label();
                default -> t.isIncome() ? t.getAmount() : -t.getAmount();
            };
        }
    }

    // ------------------------------------------------------------------ renderer

    private static final class Cell extends DefaultTableCellRenderer {
        enum Kind {DATE, TEXT, CATEGORY, MUTED, TYPE, AMOUNT}

        private final Kind kind;
        private Color pill;
        private Color bg = Theme.CARD;
        private String text = "";

        Cell(Kind kind) {
            this.kind = kind;
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            bg = isSelected ? Theme.mix(Theme.CARD, Theme.ACCENT, 0.22f) : Theme.CARD;
            pill = null;
            switch (kind) {
                case DATE -> {
                    text = ((LocalDate) value).format(Fmt.DATE);
                    setForeground(Theme.MUTED);
                }
                case TEXT -> {
                    text = value == null ? "" : value.toString();
                    setForeground(Theme.TEXT);
                }
                case MUTED -> {
                    text = value == null ? "" : value.toString();
                    setForeground(Theme.MUTED);
                }
                case CATEGORY -> {
                    text = String.valueOf(value);
                    pill = Categories.color(text);
                }
                case TYPE -> {
                    text = String.valueOf(value);
                    pill = "Income".equals(text) ? Theme.GREEN : Theme.RED;
                }
                case AMOUNT -> {
                    double v = (Double) value;
                    text = (v >= 0 ? "+" : "-") + Fmt.money(Math.abs(v));
                    setForeground(v >= 0 ? Theme.GREEN : Theme.TEXT);
                }
            }
            setFont(Theme.font(kind == Kind.AMOUNT ? Font.BOLD : Font.PLAIN, 13));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int baseY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            if (pill != null) {
                int w = fm.stringWidth(text) + 22;
                int h = 26;
                int y = (getHeight() - h) / 2;
                g2.setColor(Theme.alpha(pill, 38));
                g2.fill(new RoundRectangle2D.Double(10, y, w, h, h, h));
                g2.setColor(pill);
                g2.drawString(text, 21, baseY);
            } else if (kind == Kind.AMOUNT) {
                g2.setColor(getForeground());
                g2.drawString(text, getWidth() - fm.stringWidth(text) - 20, baseY);
            } else {
                g2.setColor(getForeground());
                String s = text;
                int max = getWidth() - 24;
                while (s.length() > 1 && fm.stringWidth(s) > max) s = s.substring(0, s.length() - 2) + "…";
                g2.drawString(s, 12, baseY);
            }
            g2.dispose();
        }
    }
}
