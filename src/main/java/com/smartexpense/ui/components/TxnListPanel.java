package com.smartexpense.ui.components;

import com.smartexpense.model.Categories;
import com.smartexpense.model.Transaction;
import com.smartexpense.ui.Theme;
import com.smartexpense.util.Fmt;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/** Compact list of transactions (used on dashboard + analytics). */
public class TxnListPanel extends JPanel {

    private final String emptyText;

    public TxnListPanel(String emptyText) {
        this.emptyText = emptyText;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void setData(List<Transaction> list, boolean showRank) {
        removeAll();
        if (list.isEmpty()) {
            JLabel l = new JLabel(emptyText);
            l.setForeground(Theme.MUTED);
            l.setFont(Theme.font(Font.PLAIN, 13));
            l.setAlignmentX(LEFT_ALIGNMENT);
            add(javax.swing.Box.createVerticalStrut(24));
            add(l);
        } else {
            int i = 1;
            for (Transaction t : list) {
                Row row = new Row(t, showRank ? i : 0);
                row.setAlignmentX(LEFT_ALIGNMENT);
                add(row);
                i++;
            }
        }
        revalidate();
        repaint();
    }

    private static final class Row extends JPanel {
        private final Transaction t;
        private final int rank;
        private boolean hover;

        Row(Transaction t, int rank) {
            this.t = t;
            this.rank = rank;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 56));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            setMinimumSize(new Dimension(100, 56));
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
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth();
            int h = getHeight();
            if (hover) {
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fill(new RoundRectangle2D.Double(0, 2, w, h - 4, 14, 14));
            }
            Color cc = Categories.color(t.getCategory());
            g2.setColor(Theme.alpha(cc, 40));
            g2.fill(new RoundRectangle2D.Double(8, 10, 36, 36, 12, 12));
            g2.setColor(cc);
            g2.setFont(Theme.font(Font.BOLD, 14));
            String badge = rank > 0 ? String.valueOf(rank) : t.getCategory().substring(0, 1).toUpperCase();
            FontMetrics bf = g2.getFontMetrics();
            g2.drawString(badge, 8 + (36 - bf.stringWidth(badge)) / 2, 10 + (36 - bf.getHeight()) / 2 + bf.getAscent());

            String amount = (t.isIncome() ? "+" : "-") + Fmt.moneyWhole(t.getAmount());
            g2.setFont(Theme.font(Font.BOLD, 14));
            FontMetrics af = g2.getFontMetrics();
            int aw = af.stringWidth(amount);
            g2.setColor(t.isIncome() ? Theme.GREEN : Theme.TEXT);
            g2.drawString(amount, w - aw - 10, 24 + af.getAscent() / 2 - 4);

            g2.setFont(Theme.font(Font.PLAIN, 11));
            g2.setColor(Theme.MUTED);
            FontMetrics sf = g2.getFontMetrics();
            String right = t.getPaymentMode();
            g2.drawString(right, w - sf.stringWidth(right) - 10, 42);

            String desc = t.getDescription() == null || t.getDescription().isBlank() ? t.getCategory() : t.getDescription();
            g2.setFont(Theme.font(Font.BOLD, 13));
            g2.setColor(Theme.TEXT);
            FontMetrics df = g2.getFontMetrics();
            int maxW = w - 60 - aw - 30;
            desc = ellipsize(desc, df, Math.max(40, maxW));
            g2.drawString(desc, 56, 25);

            g2.setFont(Theme.font(Font.PLAIN, 12));
            g2.setColor(Theme.MUTED);
            g2.drawString(t.getCategory() + "  ·  " + t.getDate().format(Fmt.DATE), 56, 43);
            g2.dispose();
        }

        private static String ellipsize(String s, FontMetrics fm, int max) {
            if (fm.stringWidth(s) <= max) return s;
            while (s.length() > 1 && fm.stringWidth(s + "…") > max) s = s.substring(0, s.length() - 1);
            return s + "…";
        }
    }
}
