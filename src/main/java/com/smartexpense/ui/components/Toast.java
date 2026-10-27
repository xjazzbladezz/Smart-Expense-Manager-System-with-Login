package com.smartexpense.ui.components;

import com.smartexpense.ui.Icons;
import com.smartexpense.ui.Theme;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Floating notification that fades in, waits, fades out. */
public final class Toast extends JComponent {

    public enum Kind {SUCCESS, ERROR, INFO, WARN}

    private static final String PROP = "smartexpense.toast";

    private final String message;
    private final Color accent;
    private final String icon;
    private float alpha;
    private int tick;

    private Toast(String message, Kind kind) {
        this.message = message;
        switch (kind) {
            case SUCCESS -> {
                accent = Theme.GREEN;
                icon = "good";
            }
            case ERROR -> {
                accent = Theme.RED;
                icon = "bad";
            }
            case WARN -> {
                accent = Theme.AMBER;
                icon = "warn";
            }
            default -> {
                accent = Theme.ACCENT_2;
                icon = "info";
            }
        }
        setFont(Theme.font(java.awt.Font.BOLD, 13));
        FontMetrics fm = getFontMetrics(getFont());
        setSize(new Dimension(Math.min(520, fm.stringWidth(message) + 84), 52));
    }

    public static void show(Component anchor, String message, Kind kind) {
        SwingUtilities.invokeLater(() -> {
            JRootPane root = SwingUtilities.getRootPane(anchor);
            if (root == null) return;
            JLayeredPane layer = root.getLayeredPane();
            Object old = layer.getClientProperty(PROP);
            if (old instanceof Toast t) layer.remove(t);

            Toast toast = new Toast(message, kind);
            toast.setLocation(layer.getWidth() - toast.getWidth() - 28, layer.getHeight() - toast.getHeight() - 28);
            layer.add(toast, JLayeredPane.POPUP_LAYER);
            layer.putClientProperty(PROP, toast);
            layer.repaint();

            Timer timer = new Timer(16, null);
            timer.addActionListener(e -> {
                toast.tick++;
                int t = toast.tick;
                if (t < 14) toast.alpha = t / 14f;
                else if (t < 14 + 170) toast.alpha = 1f;
                else if (t < 14 + 170 + 20) toast.alpha = 1f - (t - 184) / 20f;
                else {
                    timer.stop();
                    layer.remove(toast);
                    layer.repaint();
                    return;
                }
                toast.setLocation(layer.getWidth() - toast.getWidth() - 28,
                        layer.getHeight() - toast.getHeight() - 28 + Math.round((1 - Math.min(1f, toast.alpha)) * 14));
                toast.repaint();
            });
            timer.start();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 18, 18);
        g2.setColor(new Color(0x1E2338));
        g2.fill(shape);
        g2.setColor(Theme.alpha(accent, 150));
        g2.draw(shape);
        g2.setColor(Theme.alpha(accent, 40));
        g2.fill(new RoundRectangle2D.Double(10, 10, 32, 32, 12, 12));
        Icons.draw(g2, icon, 17, 17, 18, accent);
        g2.setFont(getFont());
        g2.setColor(Theme.TEXT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(message, 56, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }

    @Override
    public boolean contains(int x, int y) {
        return false; // click-through
    }
}
