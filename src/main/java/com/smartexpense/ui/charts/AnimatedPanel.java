package com.smartexpense.ui.charts;

import com.smartexpense.ui.Theme;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Base class for charts: eased 0..1 "progress" animation plus a shared tooltip painter. */
public abstract class AnimatedPanel extends JPanel {

    protected float progress = 1f;
    private Timer timer;

    protected AnimatedPanel() {
        setOpaque(false);
    }

    public void replay() {
        if (timer != null) timer.stop();
        progress = 0f;
        final long start = System.nanoTime();
        final float durationMs = 850f;
        timer = new Timer(15, null);
        timer.addActionListener(e -> {
            float t = Math.min(1f, (System.nanoTime() - start) / 1e6f / durationMs);
            progress = 1f - (float) Math.pow(1 - t, 3);
            if (t >= 1f) timer.stop();
            repaint();
        });
        timer.start();
    }

    @Override
    public void removeNotify() {
        if (timer != null) timer.stop();
        super.removeNotify();
    }

    /** Nice axis step for a raw value (1, 2, 2.5, 5 x 10^n). */
    protected static double niceStep(double raw) {
        if (raw <= 0) return 1;
        double exp = Math.pow(10, Math.floor(Math.log10(raw)));
        double f = raw / exp;
        double[] steps = {1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10};
        double nice = 10;
        for (double st : steps) {
            if (f <= st + 1e-9) {
                nice = st;
                break;
            }
        }
        return nice * exp;
    }

    protected void drawTooltip(Graphics2D g, int mx, int my, String title, String[] lines, Color[] dots) {
        Font tf = Theme.font(Font.BOLD, 12);
        Font lf = Theme.font(Font.PLAIN, 12);
        g.setFont(tf);
        FontMetrics tfm = g.getFontMetrics();
        g.setFont(lf);
        FontMetrics lfm = g.getFontMetrics();

        int w = tfm.stringWidth(title);
        for (String l : lines) w = Math.max(w, lfm.stringWidth(l) + 16);
        w += 24;
        int lineH = 20;
        int h = 16 + 20 + lines.length * lineH;
        int x = mx + 16;
        int y = my - h / 2;
        if (x + w > getWidth() - 4) x = mx - w - 16;
        if (x < 4) x = 4;
        y = Math.max(4, Math.min(getHeight() - h - 4, y));

        g.setColor(new Color(0x232942));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 14, 14));
        g.setColor(Theme.BORDER);
        g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 14, 14));

        g.setFont(tf);
        g.setColor(Theme.TEXT);
        g.drawString(title, x + 12, y + 22);
        g.setFont(lf);
        int ty = y + 22 + 18;
        for (int i = 0; i < lines.length; i++) {
            if (dots != null && i < dots.length && dots[i] != null) {
                g.setColor(dots[i]);
                g.fillOval(x + 12, ty - 9, 8, 8);
                g.setColor(Theme.TEXT);
                g.drawString(lines[i], x + 26, ty);
            } else {
                g.setColor(Theme.TEXT);
                g.drawString(lines[i], x + 12, ty);
            }
            ty += lineH;
        }
    }
}
