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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Smooth area/line chart with reveal animation and hover crosshair. */
public class LineChart extends AnimatedPanel {

    private String[] labels = new String[0];
    private double[] values = new double[0];
    private Color color = Theme.ACCENT;
    private String seriesName = "Value";
    private int hover = -1;
    private int mouseX;
    private int mouseY;

    private static final int PAD_L = 54;
    private static final int PAD_R = 14;
    private static final int PAD_T = 14;
    private static final int PAD_B = 28;

    public LineChart() {
        setPreferredSize(new Dimension(520, 260));
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                int n = values.length;
                if (n == 0) {
                    hover = -1;
                } else if (n == 1) {
                    hover = 0;
                } else {
                    double stepX = (getWidth() - PAD_L - PAD_R) / (double) (n - 1);
                    int idx = (int) Math.round((e.getX() - PAD_L) / stepX);
                    hover = (e.getX() < PAD_L - 10 || idx < 0 || idx >= n) ? -1 : idx;
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

    public void setData(String seriesName, String[] labels, double[] values, Color color) {
        this.seriesName = seriesName;
        this.labels = labels;
        this.values = values;
        this.color = color;
        this.hover = -1;
        replay();
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
        int n = values.length;

        double min = 0;
        double max = 0;
        for (double v : values) {
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        double range = max - min;
        double step = niceStep(range / 4.0);
        double top = Math.ceil(max / step) * step;
        double bottom = Math.floor(min / step) * step;
        if (top - bottom < step * 4) top = bottom + step * 4;
        if (top <= bottom) top = bottom + 1;

        g.setFont(Theme.font(Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 5f}, 0f));
        int lines = (int) Math.round((top - bottom) / step);
        for (int i = 0; i <= lines; i++) {
            double val = bottom + step * i;
            int y = (int) (base - (val - bottom) / (top - bottom) * plotH);
            g.setColor(Theme.alpha(Theme.BORDER, i == 0 ? 255 : 150));
            g.drawLine(PAD_L, y, w - PAD_R, y);
            g.setColor(Theme.MUTED);
            String s = Fmt.moneyShort(val);
            g.drawString(s, PAD_L - 8 - fm.stringWidth(s), y + 4);
        }
        g.setStroke(new BasicStroke(1f));

        if (n == 0) {
            g.setColor(Theme.MUTED);
            g.drawString("No data yet", w / 2 - 30, h / 2);
            g.dispose();
            return;
        }

        double[] px = new double[n];
        double[] py = new double[n];
        for (int i = 0; i < n; i++) {
            px[i] = n == 1 ? PAD_L + plotW / 2.0 : PAD_L + plotW * i / (double) (n - 1);
            py[i] = base - (values[i] - bottom) / (top - bottom) * plotH;
        }

        // x labels
        int every = Math.max(1, (int) Math.ceil(n / 8.0));
        g.setColor(Theme.MUTED);
        for (int i = 0; i < n; i += every) {
            String s = labels[i];
            g.drawString(s, (int) (px[i] - fm.stringWidth(s) / 2.0), h - 8);
        }

        // reveal clip
        g.clipRect(0, 0, (int) (PAD_L + (w - PAD_L) * progress) + 1, h);

        if (n >= 2) {
            Path2D line = smooth(px, py, PAD_T, base);
            Path2D area = new Path2D.Double(line);
            area.lineTo(px[n - 1], base);
            area.lineTo(px[0], base);
            area.closePath();
            g.setPaint(new GradientPaint(0, PAD_T, Theme.alpha(color, 90), 0, base, Theme.alpha(color, 0)));
            g.fill(area);
            g.setPaint(new GradientPaint(PAD_L, 0, color, w, 0, Theme.mix(color, Theme.ACCENT_2, 0.5f)));
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(line);
        } else {
            g.setColor(color);
            g.fill(new Ellipse2D.Double(px[0] - 5, py[0] - 5, 10, 10));
        }

        g.setClip(null);

        if (hover >= 0 && hover < n) {
            g.setColor(Theme.alpha(Color.WHITE, 40));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
            g.drawLine((int) px[hover], PAD_T, (int) px[hover], base);
            g.setStroke(new BasicStroke(1f));
            g.setColor(Theme.BG);
            g.fill(new Ellipse2D.Double(px[hover] - 7, py[hover] - 7, 14, 14));
            g.setColor(color);
            g.fill(new Ellipse2D.Double(px[hover] - 5, py[hover] - 5, 10, 10));
            drawTooltip(g, mouseX, mouseY, labels[hover],
                    new String[]{seriesName + ":  " + Fmt.moneyWhole(values[hover])}, new Color[]{color});
        }
        g.dispose();
    }

    /** Catmull-Rom style smoothing converted to cubic Béziers (clamped to the plot area). */
    private static Path2D smooth(double[] x, double[] y, double minY, double maxY) {
        int n = x.length;
        Path2D p = new Path2D.Double();
        p.moveTo(x[0], y[0]);
        for (int i = 0; i < n - 1; i++) {
            double x0 = x[Math.max(i - 1, 0)];
            double y0 = y[Math.max(i - 1, 0)];
            double x1 = x[i];
            double y1 = y[i];
            double x2 = x[i + 1];
            double y2 = y[i + 1];
            double x3 = x[Math.min(i + 2, n - 1)];
            double y3 = y[Math.min(i + 2, n - 1)];
            double c1x = x1 + (x2 - x0) / 6.0;
            double c1y = clamp(y1 + (y2 - y0) / 6.0, minY, maxY);
            double c2x = x2 - (x3 - x1) / 6.0;
            double c2y = clamp(y2 - (y3 - y1) / 6.0, minY, maxY);
            p.curveTo(c1x, c1y, c2x, c2y, x2, y2);
        }
        return p;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
