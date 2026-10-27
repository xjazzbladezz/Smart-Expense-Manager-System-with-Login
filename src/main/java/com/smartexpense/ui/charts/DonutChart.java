package com.smartexpense.ui.charts;

import com.smartexpense.model.Categories;
import com.smartexpense.ui.Theme;
import com.smartexpense.util.Fmt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Animated, hoverable donut chart with a legend. */
public class DonutChart extends AnimatedPanel {

    private final List<String> keys = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();
    private double total;
    private String centerTitle = "Total";
    private String centerValue = "";
    private int hover = -1;
    private int maxSlices = 7;

    public DonutChart() {
        setPreferredSize(new Dimension(360, 240));
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = hit(e.getX(), e.getY());
                if (idx != hover) {
                    hover = idx;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hover != -1) {
                    hover = -1;
                    repaint();
                }
            }
        };
        addMouseMotionListener(ma);
        addMouseListener(ma);
    }

    public DonutChart maxSlices(int n) {
        this.maxSlices = n;
        return this;
    }

    public void setData(Map<String, Double> data, String centerTitle, double centerAmount) {
        this.centerTitle = centerTitle;
        this.centerValue = Fmt.moneyShort(centerAmount);
        keys.clear();
        values.clear();
        total = 0;
        double others = 0;
        int i = 0;
        for (Map.Entry<String, Double> e : data.entrySet()) {
            if (e.getValue() <= 0) continue;
            if (i < maxSlices) {
                keys.add(e.getKey());
                values.add(e.getValue());
            } else {
                others += e.getValue();
            }
            total += e.getValue();
            i++;
        }
        if (others > 0) {
            keys.add("Others");
            values.add(others);
        }
        hover = -1;
        replay();
    }

    private double[] geometry() {
        int size = Math.min(getHeight() - 16, (int) (getWidth() * 0.52));
        size = Math.max(size, 80);
        double cx = 8 + size / 2.0;
        double cy = getHeight() / 2.0;
        double stroke = size * 0.15;
        double r = (size - stroke) / 2.0 - 4;
        return new double[]{cx, cy, r, stroke};
    }

    private int hit(int mx, int my) {
        if (total <= 0) return -1;
        double[] g = geometry();
        double dx = mx - g[0];
        double dy = my - g[1];
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (Math.abs(dist - g[2]) > g[3] / 2 + 6) return -1;
        double theta = (Math.toDegrees(Math.atan2(dx, -dy)) + 360) % 360;
        double acc = 0;
        for (int i = 0; i < values.size(); i++) {
            acc += values.get(i) / total * 360;
            if (theta <= acc) return i;
        }
        return values.size() - 1;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        double[] geo = geometry();
        double cx = geo[0], cy = geo[1], r = geo[2], stroke = geo[3];

        // track ring
        g.setStroke(new BasicStroke((float) stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.alpha(Theme.CARD_ALT, 255));
        g.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 0, 360, Arc2D.OPEN));

        if (total <= 0) {
            g.setFont(Theme.font(Font.PLAIN, 13));
            g.setColor(Theme.MUTED);
            String s = "No data yet";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, (int) (cx - fm.stringWidth(s) / 2.0), (int) cy + 5);
            g.dispose();
            return;
        }

        double start = 90;
        double gap = values.size() > 1 ? 3.0 : 0;
        for (int i = 0; i < values.size(); i++) {
            double full = values.get(i) / total * 360.0 * progress;
            double sweep = Math.max(0.6, full - gap);
            float sw = (float) (i == hover ? stroke + 8 : stroke);
            g.setStroke(new BasicStroke(sw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            Color c = Categories.color(keys.get(i));
            g.setColor(hover == -1 || hover == i ? c : Theme.alpha(c, 110));
            g.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, start - gap / 2, -sweep, Arc2D.OPEN));
            start -= full;
        }

        // centre text
        String top;
        String mid;
        String bottom = null;
        if (hover >= 0) {
            top = keys.get(hover);
            mid = Fmt.moneyShort(values.get(hover));
            bottom = Fmt.pct1(values.get(hover) / total);
        } else {
            top = centerTitle;
            mid = centerValue;
        }
        g.setFont(Theme.font(Font.PLAIN, 12));
        g.setColor(Theme.MUTED);
        FontMetrics fm = g.getFontMetrics();
        String t = fit(top, fm, (int) (r * 1.5));
        g.drawString(t, (int) (cx - fm.stringWidth(t) / 2.0), (int) (cy - 8));
        g.setFont(Theme.font(Font.BOLD, 22));
        g.setColor(Theme.TEXT);
        fm = g.getFontMetrics();
        g.drawString(mid, (int) (cx - fm.stringWidth(mid) / 2.0), (int) (cy + 16));
        if (bottom != null) {
            g.setFont(Theme.font(Font.PLAIN, 12));
            g.setColor(Theme.MUTED);
            fm = g.getFontMetrics();
            g.drawString(bottom, (int) (cx - fm.stringWidth(bottom) / 2.0), (int) (cy + 34));
        }

        // legend
        int lx = (int) (cx + r + stroke / 2 + 28);
        int rowH = 26;
        int ly = (int) (cy - (values.size() * rowH) / 2.0) + 4;
        int right = getWidth() - 4;
        for (int i = 0; i < values.size(); i++) {
            int y = ly + i * rowH;
            Color c = Categories.color(keys.get(i));
            boolean dim = hover != -1 && hover != i;
            g.setColor(dim ? Theme.alpha(c, 100) : c);
            g.fillRoundRect(lx, y + 4, 10, 10, 4, 4);
            g.setFont(Theme.font(hover == i ? Font.BOLD : Font.PLAIN, 12));
            fm = g.getFontMetrics();
            String pct = Fmt.pct(values.get(i) / total);
            int pw = fm.stringWidth(pct);
            g.setColor(dim ? Theme.alpha(Theme.MUTED, 120) : Theme.MUTED);
            g.drawString(pct, right - pw, y + 14);
            g.setColor(dim ? Theme.alpha(Theme.TEXT, 120) : Theme.TEXT);
            g.drawString(fit(keys.get(i), fm, right - pw - lx - 28), lx + 18, y + 14);
        }
        g.dispose();
    }

    private static String fit(String s, FontMetrics fm, int max) {
        if (fm.stringWidth(s) <= max) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
