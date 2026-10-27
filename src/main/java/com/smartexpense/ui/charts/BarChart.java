package com.smartexpense.ui.charts;

import com.smartexpense.ui.Theme;
import com.smartexpense.util.Fmt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Grouped (or single-series) animated bar chart with hover tooltip. */
public class BarChart extends AnimatedPanel {

    private List<String> labels = new ArrayList<>();
    private double[] a = new double[0];
    private double[] b;
    private String nameA = "Income";
    private String nameB = "Expense";
    private Color colA = Theme.GREEN;
    private Color colB = Theme.RED;
    private int hover = -1;

    public BarChart() {
        setPreferredSize(new Dimension(520, 280));
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = indexAt(e.getX());
                if (idx != hover) {
                    hover = idx;
                }
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
        };
        addMouseMotionListener(ma);
        addMouseListener(ma);
    }

    public void setSeries(String nameA, Color colA, String nameB, Color colB) {
        this.nameA = nameA;
        this.colA = colA;
        this.nameB = nameB;
        this.colB = colB;
    }

    public void setData(List<String> labels, double[] a, double[] b) {
        this.labels = new ArrayList<>(labels);
        this.a = a;
        this.b = b;
        this.hover = -1;
        replay();
    }

    private static final int PAD_L = 54;
    private static final int PAD_R = 8;
    private static final int PAD_T = 34;
    private static final int PAD_B = 28;

    private int indexAt(int x) {
        int n = labels.size();
        if (n == 0) return -1;
        double groupW = (getWidth() - PAD_L - PAD_R) / (double) n;
        int idx = (int) ((x - PAD_L) / groupW);
        return (x < PAD_L || idx < 0 || idx >= n) ? -1 : idx;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.aa(g);
        int w = getWidth();
        int h = getHeight();
        int plotW = w - PAD_L - PAD_R;
        int plotH = h - PAD_T - PAD_B;
        int base = PAD_T + plotH;
        int n = labels.size();

        double max = 0;
        for (double v : a) max = Math.max(max, v);
        if (b != null) for (double v : b) max = Math.max(max, v);
        double step = niceStep(max / 4.0);
        double top = Math.max(step * 4, step);
        if (max > top) top = Math.ceil(max / step) * step;

        // legend
        g.setFont(Theme.font(Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int lx = PAD_L;
        g.setColor(colA);
        g.fillRoundRect(lx, 8, 10, 10, 4, 4);
        g.setColor(Theme.MUTED);
        g.drawString(nameA, lx + 16, 17);
        if (b != null) {
            lx += 16 + fm.stringWidth(nameA) + 22;
            g.setColor(colB);
            g.fillRoundRect(lx, 8, 10, 10, 4, 4);
            g.setColor(Theme.MUTED);
            g.drawString(nameB, lx + 16, 17);
        }

        // grid + y labels
        int lines = (int) Math.round(top / step);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 5f}, 0f));
        for (int i = 0; i <= lines; i++) {
            double val = step * i;
            int y = (int) (base - val / top * plotH);
            g.setColor(Theme.alpha(Theme.BORDER, i == 0 ? 255 : 150));
            g.drawLine(PAD_L, y, w - PAD_R, y);
            String s = Fmt.moneyShort(val);
            g.setColor(Theme.MUTED);
            g.drawString(s, PAD_L - 8 - fm.stringWidth(s), y + 4);
        }
        g.setStroke(new BasicStroke(1f));

        if (n == 0) {
            g.dispose();
            return;
        }

        double groupW = plotW / (double) n;
        boolean two = b != null;
        double barW = Math.min(two ? groupW * 0.30 : groupW * 0.46, two ? 34 : 52);
        double gap = 6;

        for (int i = 0; i < n; i++) {
            double gx = PAD_L + groupW * i + groupW / 2;
            if (i == hover) {
                g.setColor(new Color(255, 255, 255, 12));
                g.fill(new RoundRectangle2D.Double(PAD_L + groupW * i + 4, PAD_T - 6, groupW - 8, plotH + 6, 14, 14));
            }
            if (two) {
                drawBar(g, gx - barW - gap / 2, base, barW, a[i], top, plotH, colA, i == hover);
                drawBar(g, gx + gap / 2, base, barW, b[i], top, plotH, colB, i == hover);
            } else {
                drawBar(g, gx - barW / 2, base, barW, a[i], top, plotH, colA, i == hover);
            }
            g.setFont(Theme.font(i == hover ? Font.BOLD : Font.PLAIN, 12));
            fm = g.getFontMetrics();
            g.setColor(i == hover ? Theme.TEXT : Theme.MUTED);
            String lab = labels.get(i);
            g.drawString(lab, (int) (gx - fm.stringWidth(lab) / 2.0), h - 8);
        }

        if (hover >= 0 && hover < n) {
            Point p = getMousePosition() == null ? null : new Point(getMousePosition().x, getMousePosition().y);
            if (p != null) {
                String[] lines2;
                Color[] dots;
                if (two) {
                    lines2 = new String[]{nameA + ":  " + Fmt.moneyWhole(a[hover]), nameB + ":  " + Fmt.moneyWhole(b[hover])};
                    dots = new Color[]{colA, colB};
                } else {
                    lines2 = new String[]{nameA + ":  " + Fmt.moneyWhole(a[hover])};
                    dots = new Color[]{colA};
                }
                drawTooltip(g, p.x, p.y, labels.get(hover), lines2, dots);
            }
        }
        g.dispose();
    }

    private void drawBar(Graphics2D g, double x, int base, double bw, double value, double top, int plotH,
                         Color c, boolean hot) {
        double bh = value / top * plotH * progress;
        if (bh < 0.5) return;
        Shape oldClip = g.getClip();
        g.clipRect(0, 0, getWidth(), base);
        double y = base - bh;
        g.setPaint(new GradientPaint(0, (float) y, hot ? Theme.mix(c, Color.WHITE, 0.15f) : c,
                0, base, Theme.alpha(c, hot ? 150 : 90)));
        g.fill(new RoundRectangle2D.Double(x, y, bw, bh + 12, 12, 12));
        g.setClip(oldClip);
    }

    private static final class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
