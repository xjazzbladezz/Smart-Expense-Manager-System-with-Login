package com.smartexpense.ui.components;

import com.smartexpense.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

/** Rounded card with a title / subtitle header and arbitrary body. */
public class Card extends RoundedPanel {

    private final JLabel subtitle = new JLabel(" ");

    public Card(String title, String subtitleText, JComponent body) {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 16));
        t.setForeground(Theme.TEXT);
        subtitle.setText(subtitleText == null ? " " : subtitleText);
        subtitle.setFont(Theme.font(Font.PLAIN, 12));
        subtitle.setForeground(Theme.MUTED);

        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        head.add(t, BorderLayout.NORTH);
        head.add(subtitle, BorderLayout.CENTER);

        add(head, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public void setSubtitle(String s) {
        subtitle.setText(s == null || s.isEmpty() ? " " : s);
    }
}
