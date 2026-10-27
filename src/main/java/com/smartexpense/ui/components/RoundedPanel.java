package com.smartexpense.ui.components;

import com.smartexpense.ui.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

/** Panel with rounded corners, optional gradient fill and a hairline border. */
public class RoundedPanel extends JPanel {

    private int arc = 22;
    private Color fill = Theme.CARD;
    private Color border = Theme.BORDER;
    private Color gradientTo;

    public RoundedPanel() {
        this(null);
    }

    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public RoundedPanel arc(int arc) {
        this.arc = arc;
        return this;
    }

    public RoundedPanel fill(Color c) {
        this.fill = c;
        return this;
    }

    public RoundedPanel border(Color c) {
        this.border = c;
        return this;
    }

    public RoundedPanel gradient(Color from, Color to) {
        this.fill = from;
        this.gradientTo = to;
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, arc, arc);
        if (gradientTo != null) {
            g2.setPaint(new GradientPaint(0, 0, fill, getWidth(), getHeight(), gradientTo));
        } else {
            g2.setColor(fill);
        }
        g2.fill(shape);
        if (border != null) {
            g2.setColor(border);
            g2.draw(shape);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
