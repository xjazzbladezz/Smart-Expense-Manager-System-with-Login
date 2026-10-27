package com.smartexpense.ui;

import com.smartexpense.model.Categories;
import com.smartexpense.model.Transaction;
import com.smartexpense.model.TransactionType;
import com.smartexpense.service.AppContext;
import com.smartexpense.ui.components.FancyButton;
import com.smartexpense.ui.components.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/** Modal add / edit dialog with live auto-suggest powered by the Trie. */
public class TransactionDialog extends JDialog {

    private Transaction result;
    private TransactionType type = TransactionType.EXPENSE;

    private final FancyButton expenseBtn = new FancyButton("Expense", FancyButton.Style.SECONDARY);
    private final FancyButton incomeBtn = new FancyButton("Income", FancyButton.Style.SECONDARY);
    private final JTextField amountField = new JTextField();
    private final JComboBox<String> categoryBox = new JComboBox<>();
    private final JComboBox<String> modeBox = new JComboBox<>(Categories.PAYMENT_MODES.toArray(new String[0]));
    private final JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
    private final JTextField descField = new JTextField();
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel error = new JLabel(" ");
    private boolean updatingFromChip;

    private TransactionDialog(Component parent, Transaction existing) {
        super(SwingUtilities.getWindowAncestor(parent), existing == null ? "Add Transaction" : "Edit Transaction",
                ModalityType.APPLICATION_MODAL);
        setUndecorated(false);
        getContentPane().setBackground(Theme.BG);

        RoundedPanel card = new RoundedPanel(new GridBagLayout());
        card.arc(26);
        card.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        JLabel title = new JLabel(existing == null ? "New transaction" : "Edit transaction");
        title.setFont(Theme.font(Font.BOLD, 20));
        title.setForeground(Theme.TEXT);
        JLabel sub = new JLabel(existing == null ? "Record money coming in or going out" : "Update the details below");
        sub.setFont(Theme.font(Font.PLAIN, 12));
        sub.setForeground(Theme.MUTED);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        card.add(title, gc);
        gc.gridy++;
        gc.insets = new Insets(2, 0, 16, 0);
        card.add(sub, gc);

        // type toggle
        JPanel seg = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        seg.setOpaque(false);
        seg.add(expenseBtn);
        seg.add(incomeBtn);
        expenseBtn.addActionListener(e -> setType(TransactionType.EXPENSE));
        incomeBtn.addActionListener(e -> setType(TransactionType.INCOME));
        gc.gridy++;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 14, 0);
        card.add(seg, gc);

        amountField.putClientProperty("JTextField.placeholderText", "0.00");
        amountField.setFont(Theme.font(Font.BOLD, 20));
        amountField.putClientProperty("JTextField.leadingIcon", Icons.of("rupee", 18, Theme.MUTED));
        addRow(card, gc, "Amount", amountField);

        gc.gridy++;
        gc.gridwidth = 1;
        gc.weightx = 0.5;
        gc.insets = new Insets(0, 0, 0, 8);
        card.add(label("Category"), gc);
        gc.gridx = 1;
        gc.insets = new Insets(0, 8, 0, 0);
        card.add(label("Date"), gc);
        gc.gridy++;
        gc.gridx = 0;
        gc.insets = new Insets(4, 0, 14, 8);
        card.add(categoryBox, gc);
        gc.gridx = 1;
        gc.insets = new Insets(4, 8, 14, 0);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(dateSpinner, "dd MMM yyyy");
        dateSpinner.setEditor(editor);
        card.add(dateSpinner, gc);

        gc.gridy++;
        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 0, 8);
        card.add(label("Payment mode"), gc);
        gc.gridy++;
        gc.insets = new Insets(4, 0, 14, 8);
        card.add(modeBox, gc);

        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.gridy++;
        card.add(label("Description"), gc);
        gc.gridy++;
        gc.insets = new Insets(4, 0, 6, 0);
        descField.putClientProperty("JTextField.placeholderText", "e.g. Dinner at Biryani House");
        card.add(descField, gc);
        gc.gridy++;
        gc.insets = new Insets(0, 0, 10, 0);
        chips.setOpaque(false);
        chips.setPreferredSize(new Dimension(380, 32));
        card.add(chips, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 8, 0);
        error.setForeground(Theme.RED);
        error.setFont(Theme.font(Font.PLAIN, 12));
        card.add(error, gc);

        FancyButton cancel = new FancyButton("Cancel", FancyButton.Style.SECONDARY);
        FancyButton save = new FancyButton(existing == null ? "Save transaction" : "Save changes", FancyButton.Style.PRIMARY);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave(existing));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.add(cancel);
        btns.add(save);
        gc.gridy++;
        gc.insets = new Insets(4, 0, 0, 0);
        card.add(btns, gc);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.BG);
        wrap.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        wrap.add(card, BorderLayout.CENTER);
        setContentPane(wrap);

        for (JComponent c : new JComponent[]{categoryBox, modeBox, dateSpinner, descField}) {
            c.setPreferredSize(new Dimension(c.getPreferredSize().width, 38));
        }
        amountField.setPreferredSize(new Dimension(300, 46));

        descField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshChips();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshChips();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshChips();
            }
        });

        // initial values
        if (existing != null) {
            setType(existing.getType());
            amountField.setText(String.valueOf(existing.getAmount()));
            categoryBox.setSelectedItem(existing.getCategory());
            modeBox.setSelectedItem(existing.getPaymentMode());
            dateSpinner.setValue(Date.from(existing.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            descField.setText(existing.getDescription() == null ? "" : existing.getDescription());
        } else {
            setType(TransactionType.EXPENSE);
        }

        getRootPane().setDefaultButton(save);
        pack();
        setMinimumSize(new Dimension(460, getHeight()));
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.MUTED);
        return l;
    }

    private static void addRow(JPanel card, GridBagConstraints gc, String label, JComponent field) {
        gc.gridy++;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        card.add(label(label), gc);
        gc.gridy++;
        gc.insets = new Insets(4, 0, 14, 0);
        card.add(field, gc);
    }

    private void setType(TransactionType t) {
        this.type = t;
        expenseBtn.active(t == TransactionType.EXPENSE, Theme.RED);
        incomeBtn.active(t == TransactionType.INCOME, Theme.GREEN);
        Object keep = categoryBox.getSelectedItem();
        categoryBox.setModel(new DefaultComboBoxModel<>(Categories.forType(t).toArray(new String[0])));
        if (keep != null && Categories.forType(t).contains(keep.toString())) categoryBox.setSelectedItem(keep);
    }

    private void refreshChips() {
        if (updatingFromChip) return;
        chips.removeAll();
        String text = descField.getText();
        if (text != null && !text.isBlank()) {
            List<String> sugg = AppContext.expenses().suggestDescriptions(text, 4);
            for (String s : sugg) {
                if (s.equalsIgnoreCase(text.trim())) continue;
                FancyButton b = new FancyButton(s.length() > 26 ? s.substring(0, 25) + "…" : s, FancyButton.Style.GHOST)
                        .compact();
                b.setToolTipText(s);
                b.addActionListener(e -> {
                    updatingFromChip = true;
                    descField.setText(s);
                    updatingFromChip = false;
                    String cat = AppContext.expenses().categoryFor(s);
                    if (cat != null) {
                        if (Categories.INCOME.contains(cat)) setType(TransactionType.INCOME);
                        else setType(TransactionType.EXPENSE);
                        categoryBox.setSelectedItem(cat);
                    }
                    chips.removeAll();
                    chips.revalidate();
                    chips.repaint();
                    amountField.requestFocusInWindow();
                });
                chips.add(b);
            }
        }
        chips.revalidate();
        chips.repaint();
    }

    private void onSave(Transaction existing) {
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", "").replace("₹", ""));
        } catch (NumberFormatException ex) {
            error.setText("Enter a valid amount, e.g. 450 or 1299.50");
            return;
        }
        if (amount <= 0) {
            error.setText("Amount must be greater than zero");
            return;
        }
        if (amount > 99_999_999) {
            error.setText("Amount is too large");
            return;
        }
        LocalDate date = ((Date) dateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String category = String.valueOf(categoryBox.getSelectedItem());
        String desc = descField.getText().trim();
        if (desc.isEmpty()) desc = category;
        if (desc.length() > 200) desc = desc.substring(0, 200);

        Transaction t = new Transaction();
        if (existing != null) {
            t.setId(existing.getId());
            t.setUserId(existing.getUserId());
        }
        t.setDate(date);
        t.setType(type);
        t.setCategory(category);
        t.setAmount(Math.round(amount * 100.0) / 100.0);
        t.setDescription(desc);
        t.setPaymentMode(String.valueOf(modeBox.getSelectedItem()));
        result = t;
        dispose();
    }

    /** @return the filled transaction (new object), or null when cancelled */
    public static Transaction show(Component parent, Transaction existing) {
        TransactionDialog d = new TransactionDialog(parent, existing);
        d.setVisible(true);
        return d.result;
    }
}
