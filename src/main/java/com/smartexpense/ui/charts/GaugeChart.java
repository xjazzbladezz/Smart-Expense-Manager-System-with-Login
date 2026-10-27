package com.smartexpense.ui.charts;

import com.smartexpense.ui.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/** Semi-circular score gauge (0..100). */
public class GaugeChart extends AnimatedPanel {

    private int score;
    private String verdict = "";

    public GaugeChart() {
        setPreferredSize(new Dimension(260, 160));
    }

    public void setScore(int score, String verdict) {
        this.score = Math.max(0, Math.min(100, score));
        this.verdict = verdict;
        replay();
    }

    public static Color colorFor(int score) {
        if (score >= 75) return Theme.GREEN;
        if (score >= 50) return Theme.ACCENT_2;
        if (score >= 30) return Theme.AMBER;
        return Theme.RED;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        int w = getWidth();
        int h = getHeight();
        double stroke = 16;
        double r = Math.min(w / 2.0 - stroke, h - stroke - 34);
        r = Math.max(r, 40);
        double cx = w / 2.0;
        double cy = h - 30;

        g.setStroke(new BasicStroke((float) stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.CARD_ALT);
        g.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 180, -180, Arc2D.OPEN));

        double shown = score * progress;
        Color c = colorFor(score);
        if (shown > 0.5) {
            g.setColor(c);
            g.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 180, -180.0 * shown / 100.0, Arc2D.OPEN));
            double ang = Math.toRadians(180 - 180.0 * shown / 100.0);
            double kx = cx + r * Math.cos(ang);
            double ky = cy - r * Math.sin(ang);
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(kx - 4, ky - 4, 8, 8));
        }

        g.setFont(Theme.font(Font.BOLD, 34));
        g.setColor(Theme.TEXT);
        String s = String.valueOf(Math.round(shown));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (int) (cx - fm.stringWidth(s) / 2.0), (int) (cy - 4));

        g.setFont(Theme.font(Font.BOLD, 13));
        g.setColor(c);
        fm = g.getFontMetrics();
        g.drawString(verdict, (int) (cx - fm.stringWidth(verdict) / 2.0), (int) (cy + 22));

        g.setFont(Theme.font(Font.PLAIN, 11));
        g.setColor(Theme.MUTED);
        fm = g.getFontMetrics();
        g.drawString("0", (int) (cx - r - fm.stringWidth("0") / 2.0), (int) (cy + 22));
        g.drawString("100", (int) (cx + r - fm.stringWidth("100") / 2.0), (int) (cy + 22));
        g.dispose();
    }
}
