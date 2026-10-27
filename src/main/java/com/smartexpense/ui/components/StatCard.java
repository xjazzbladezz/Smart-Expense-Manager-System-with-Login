package com.smartexpense.ui.components;

import com.smartexpense.ui.Icons;
import com.smartexpense.ui.Theme;

import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.DoubleFunction;

/** KPI tile with count-up animation and a soft glow in the accent colour. */
public class StatCard extends RoundedPanel {

    private final String title;
    private final String iconName;
    private final Color accent;

    private double shown;
    private double target;
    private DoubleFunction<String> formatter = v -> String.valueOf(Math.round(v));
    private String sub = " ";
    private Color subColor = Theme.MUTED;
    private Timer timer;

    public StatCard(String title, String iconName, Color accent) {
        this.title = title;
        this.iconName = iconName;
        this.accent = accent;
        setPreferredSize(new Dimension(220, 124));
    }

    public void setValue(double value, DoubleFunction<String> formatter, String sub, Color subColor) {
        this.formatter = formatter;
        this.sub = sub == null ? " " : sub;
        this.subColor = subColor == null ? Theme.MUTED : subColor;
        final double from = shown;
        this.target = value;
        if (timer != null) timer.stop();
        final long start = System.nanoTime();
        timer = new Timer(15, null);
        timer.addActionListener(e -> {
            float t = Math.min(1f, (System.nanoTime() - start) / 1e6f / 700f);
            float eased = 1f - (float) Math.pow(1 - t, 3);
            shown = from + (target - from) * eased;
            if (t >= 1f) {
                shown = target;
                timer.stop();
            }
            repaint();
        });
        timer.start();
    }

    @Override
    public void removeNotify() {
        if (timer != null) timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth();
        int h = getHeight();

        // glow in the top-right corner
        g2.setClip(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 22, 22));
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(w - 20, 10), 120f,
                new float[]{0f, 1f}, new Color[]{Theme.alpha(accent, 55), Theme.alpha(accent, 0)}));
        g2.fillRect(0, 0, w, h);
        g2.setClip(null);

        // icon badge
        g2.setPaint(new GradientPaint(w - 62, 18, Theme.alpha(accent, 70), w - 20, 60, Theme.alpha(accent, 25)));
        g2.fill(new RoundRectangle2D.Double(w - 62, 18, 42, 42, 14, 14));
        Icons.draw(g2, iconName, w - 62 + 11, 18 + 11, 20, accent);

        g2.setFont(Theme.font(Font.PLAIN, 13));
        g2.setColor(Theme.MUTED);
        g2.drawString(title, 22, 34);

        g2.setFont(Theme.font(Font.BOLD, 28));
        g2.setColor(Theme.TEXT);
        String v = formatter.apply(shown);
        FontMetrics fm = g2.getFontMetrics();
        while (fm.stringWidth(v) > w - 44 && g2.getFont().getSize() > 16) {
            g2.setFont(g2.getFont().deriveFont(g2.getFont().getSize2D() - 1));
            fm = g2.getFontMetrics();
        }
        g2.drawString(v, 22, 76);

        g2.setFont(Theme.font(Font.PLAIN, 12));
        g2.setColor(subColor);
        g2.drawString(sub, 22, 102);
        g2.dispose();
    }
}
