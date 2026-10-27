package com.smartexpense.ui.components;

import com.smartexpense.ui.Icons;
import com.smartexpense.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

/** Custom painted button with hover / press feedback. */
public class FancyButton extends JButton {

    public enum Style {PRIMARY, SECONDARY, DANGER, GHOST}

    private final Style style;
    private String iconName;
    private boolean hover;
    private boolean press;
    private boolean active;
    private Color activeColor = Theme.ACCENT;
    private int arc = 14;
    private int padX = 18;
    private int height = 40;
    private float fontSize = 13f;

    public FancyButton(String text, Style style) {
        super(text);
        this.style = style;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.font(java.awt.Font.BOLD, 13));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                press = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                press = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                press = false;
                repaint();
            }
        });
    }

    public FancyButton icon(String name) {
        this.iconName = name;
        return this;
    }

    public FancyButton compact() {
        this.height = 32;
        this.padX = 12;
        this.arc = 10;
        this.fontSize = 12f;
        setFont(Theme.font(java.awt.Font.BOLD, 12));
        return this;
    }

    public FancyButton active(boolean on, Color color) {
        this.active = on;
        this.activeColor = color;
        repaint();
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = padX * 2;
        String t = getText();
        if (t != null && !t.isEmpty()) w += fm.stringWidth(t);
        if (iconName != null) w += (height > 34 ? 18 : 15) + (t != null && !t.isEmpty() ? 8 : 0);
        return new Dimension(Math.max(w, height), height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, arc, arc);
        Color text;

        if (!isEnabled()) {
            g2.setColor(Theme.alpha(Theme.CARD_ALT, 160));
            g2.fill(shape);
            text = Theme.MUTED;
        } else if (active) {
            g2.setColor(Theme.alpha(activeColor, hover ? 70 : 52));
            g2.fill(shape);
            g2.setColor(Theme.alpha(activeColor, 200));
            g2.draw(shape);
            text = activeColor;
        } else {
            switch (style) {
                case PRIMARY -> {
                    Color a = hover ? Theme.mix(Theme.ACCENT, Color.WHITE, 0.12f) : Theme.ACCENT;
                    Color b = hover ? Theme.mix(new Color(0x4F8CFF), Color.WHITE, 0.12f) : new Color(0x4F8CFF);
                    if (press) {
                        a = Theme.mix(a, Color.BLACK, 0.15f);
                        b = Theme.mix(b, Color.BLACK, 0.15f);
                    }
                    g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
                    g2.fill(shape);
                    text = Color.WHITE;
                }
                case DANGER -> {
                    g2.setColor(Theme.alpha(Theme.RED, hover ? 60 : 32));
                    g2.fill(shape);
                    g2.setColor(Theme.alpha(Theme.RED, 130));
                    g2.draw(shape);
                    text = Theme.RED;
                }
                case GHOST -> {
                    if (hover) {
                        g2.setColor(new Color(255, 255, 255, press ? 26 : 16));
                        g2.fill(shape);
                    }
                    text = hover ? Theme.TEXT : Theme.MUTED;
                }
                default -> {
                    g2.setColor(hover ? Theme.mix(Theme.CARD_ALT, Color.WHITE, 0.06f) : Theme.CARD_ALT);
                    g2.fill(shape);
                    g2.setColor(hover ? Theme.alpha(Theme.ACCENT, 160) : Theme.BORDER);
                    g2.draw(shape);
                    text = Theme.TEXT;
                }
            }
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String label = getText() == null ? "" : getText();
        int iconSize = h > 34 ? 18 : 15;
        int gap = (iconName != null && !label.isEmpty()) ? 8 : 0;
        int contentW = fm.stringWidth(label) + (iconName != null ? iconSize + gap : 0);
        int x = (w - contentW) / 2;
        if (iconName != null) {
            Icons.draw(g2, iconName, x, (h - iconSize) / 2, iconSize, text);
            x += iconSize + gap;
        }
        g2.setColor(text);
        g2.drawString(label, x, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
