package com.smartexpense.ui.components;

import com.smartexpense.ui.Icons;
import com.smartexpense.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** Sidebar navigation item. */
public class NavButton extends JToggleButton {

    private final String iconName;
    private boolean hover;

    public NavButton(String text, String iconName) {
        super(text);
        this.iconName = iconName;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.font(java.awt.Font.BOLD, 14));
        setPreferredSize(new Dimension(200, 46));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
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
        boolean on = isSelected();

        if (on) {
            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, w, h, 16, 16);
            g2.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.ACCENT, 70), w, 0, Theme.alpha(Theme.ACCENT, 14)));
            g2.fill(shape);
            g2.setColor(Theme.ACCENT);
            g2.fill(new RoundRectangle2D.Double(0, 10, 4, h - 20, 4, 4));
        } else if (hover) {
            g2.setColor(new Color(255, 255, 255, 12));
            g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 16, 16));
        }

        Color c = on ? Color.WHITE : (hover ? Theme.TEXT : Theme.MUTED);
        Icons.draw(g2, iconName, 18, (h - 20) / 2, 20, on ? Theme.mix(Theme.ACCENT, Color.WHITE, 0.55f) : c);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(c);
        g2.drawString(getText(), 52, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
