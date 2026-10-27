package com.smartexpense.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Tiny vector icon set drawn with Java2D (24x24 design grid). */
public final class Icons {

    private Icons() {
    }

    public static Icon of(String name, int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.aa(g2);
                draw(g2, name, x, y, size, color);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static void draw(Graphics2D g0, String name, int x, int y, int size, Color color) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(x, y);
        double s = size / 24.0;
        g.scale(s, s);
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (name) {
            case "dashboard" -> {
                g.draw(new RoundRectangle2D.Double(3.5, 3.5, 7, 9, 2, 2));
                g.draw(new RoundRectangle2D.Double(13.5, 3.5, 7, 5, 2, 2));
                g.draw(new RoundRectangle2D.Double(13.5, 11.5, 7, 9, 2, 2));
                g.draw(new RoundRectangle2D.Double(3.5, 15.5, 7, 5, 2, 2));
            }
            case "list" -> {
                g.draw(new Line2D.Double(8, 6, 20, 6));
                g.draw(new Line2D.Double(8, 12, 20, 12));
                g.draw(new Line2D.Double(8, 18, 20, 18));
                g.fill(new Ellipse2D.Double(3, 5, 2.4, 2.4));
                g.fill(new Ellipse2D.Double(3, 11, 2.4, 2.4));
                g.fill(new Ellipse2D.Double(3, 17, 2.4, 2.4));
            }
            case "wallet" -> {
                g.draw(new RoundRectangle2D.Double(3, 6, 18, 13, 4, 4));
                g.draw(new Line2D.Double(3, 10, 21, 10));
                g.fill(new Ellipse2D.Double(15.3, 13.2, 2.6, 2.6));
            }
            case "chart" -> {
                g.draw(new Line2D.Double(6, 20, 6, 12));
                g.draw(new Line2D.Double(12, 20, 12, 5));
                g.draw(new Line2D.Double(18, 20, 18, 9));
            }
            case "data" -> {
                g.draw(new Line2D.Double(8, 20, 8, 5));
                Path2D up = new Path2D.Double();
                up.moveTo(4.5, 8.5);
                up.lineTo(8, 5);
                up.lineTo(11.5, 8.5);
                g.draw(up);
                g.draw(new Line2D.Double(16, 4, 16, 19));
                Path2D down = new Path2D.Double();
                down.moveTo(12.5, 15.5);
                down.lineTo(16, 19);
                down.lineTo(19.5, 15.5);
                g.draw(down);
            }
            case "plus" -> {
                g.draw(new Line2D.Double(12, 5, 12, 19));
                g.draw(new Line2D.Double(5, 12, 19, 12));
            }
            case "undo", "redo" -> {
                if (name.equals("redo")) {
                    g.transform(new AffineTransform(-1, 0, 0, 1, 24, 0));
                }
                Path2D head = new Path2D.Double();
                head.moveTo(8, 5);
                head.lineTo(3.5, 10);
                head.lineTo(8, 15);
                g.draw(head);
                Path2D body = new Path2D.Double();
                body.moveTo(4, 10);
                body.lineTo(14, 10);
                body.curveTo(18, 10, 20.5, 12.5, 20.5, 15);
                body.curveTo(20.5, 18, 18, 20, 14.5, 20);
                body.lineTo(10, 20);
                g.draw(body);
            }
            case "logout" -> {
                Path2D door = new Path2D.Double();
                door.moveTo(10, 4);
                door.lineTo(5, 4);
                door.lineTo(5, 20);
                door.lineTo(10, 20);
                g.draw(door);
                g.draw(new Line2D.Double(10, 12, 20, 12));
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(16, 8);
                arrow.lineTo(20, 12);
                arrow.lineTo(16, 16);
                g.draw(arrow);
            }
            case "search" -> {
                g.draw(new Ellipse2D.Double(4, 4, 12, 12));
                g.draw(new Line2D.Double(14.5, 14.5, 20, 20));
            }
            case "trash" -> {
                g.draw(new Line2D.Double(4, 7, 20, 7));
                Path2D body = new Path2D.Double();
                body.moveTo(6, 7);
                body.lineTo(7, 20);
                body.lineTo(17, 20);
                body.lineTo(18, 7);
                g.draw(body);
                Path2D handle = new Path2D.Double();
                handle.moveTo(9, 7);
                handle.lineTo(9, 4);
                handle.lineTo(15, 4);
                handle.lineTo(15, 7);
                g.draw(handle);
            }
            case "edit" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(4, 20);
                p.lineTo(5, 15);
                p.lineTo(16, 4);
                p.lineTo(20, 8);
                p.lineTo(9, 19);
                p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(14, 6, 18, 10));
            }
            case "download" -> {
                g.draw(new Line2D.Double(12, 4, 12, 15));
                Path2D h = new Path2D.Double();
                h.moveTo(7, 10.5);
                h.lineTo(12, 15.5);
                h.lineTo(17, 10.5);
                g.draw(h);
                g.draw(new Line2D.Double(4.5, 20, 19.5, 20));
            }
            case "upload" -> {
                g.draw(new Line2D.Double(12, 16, 12, 5));
                Path2D h = new Path2D.Double();
                h.moveTo(7, 9.5);
                h.lineTo(12, 4.5);
                h.lineTo(17, 9.5);
                g.draw(h);
                g.draw(new Line2D.Double(4.5, 20, 19.5, 20));
            }
            case "bolt" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(13, 2);
                p.lineTo(5, 13);
                p.lineTo(11, 13);
                p.lineTo(10, 22);
                p.lineTo(19, 10);
                p.lineTo(13, 10);
                p.closePath();
                g.fill(p);
            }
            case "trend" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(3, 17);
                p.lineTo(9, 11);
                p.lineTo(13, 15);
                p.lineTo(21, 6);
                g.draw(p);
                Path2D a = new Path2D.Double();
                a.moveTo(15.5, 6);
                a.lineTo(21, 6);
                a.lineTo(21, 11.5);
                g.draw(a);
            }
            case "calendar" -> {
                g.draw(new RoundRectangle2D.Double(4, 5, 16, 15, 3, 3));
                g.draw(new Line2D.Double(4, 10, 20, 10));
                g.draw(new Line2D.Double(8, 3, 8, 7));
                g.draw(new Line2D.Double(16, 3, 16, 7));
            }
            case "good" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(5, 12.5);
                p.lineTo(10, 17.5);
                p.lineTo(19, 7);
                g.draw(p);
            }
            case "warn" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 3.5);
                p.lineTo(21.5, 20);
                p.lineTo(2.5, 20);
                p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(12, 9.5, 12, 14.5));
                g.fill(new Ellipse2D.Double(11, 16.3, 2, 2));
            }
            case "bad" -> {
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g.draw(new Line2D.Double(9, 9, 15, 15));
                g.draw(new Line2D.Double(15, 9, 9, 15));
            }
            case "info" -> {
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g.draw(new Line2D.Double(12, 11, 12, 17));
                g.fill(new Ellipse2D.Double(11, 6.6, 2, 2));
            }
            case "rupee" -> {
                g.draw(new Line2D.Double(7, 5, 17, 5));
                g.draw(new Line2D.Double(7, 9.5, 17, 9.5));
                Path2D p = new Path2D.Double();
                p.moveTo(7, 5);
                p.curveTo(15, 5, 15, 14, 7, 14);
                p.lineTo(16, 20);
                g.draw(p);
            }
            case "trash-all" -> {
                g.draw(new Line2D.Double(5, 5, 19, 19));
                g.draw(new Line2D.Double(19, 5, 5, 19));
            }
            case "file" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(6, 3);
                p.lineTo(14, 3);
                p.lineTo(19, 8);
                p.lineTo(19, 21);
                p.lineTo(6, 21);
                p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(14, 3, 14, 8));
                g.draw(new Line2D.Double(14, 8, 19, 8));
            }
            case "spark" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 2.5);
                p.curveTo(12.8, 8, 16, 11.2, 21.5, 12);
                p.curveTo(16, 12.8, 12.8, 16, 12, 21.5);
                p.curveTo(11.2, 16, 8, 12.8, 2.5, 12);
                p.curveTo(8, 11.2, 11.2, 8, 12, 2.5);
                p.closePath();
                g.fill(p);
            }
            default -> g.draw(new Ellipse2D.Double(5, 5, 14, 14));
        }
        g.dispose();
    }
}
