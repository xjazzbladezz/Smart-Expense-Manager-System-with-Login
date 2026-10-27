package com.smartexpense.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;

/** Design tokens (colours, fonts) and Look-and-Feel setup. */
public final class Theme {

    public static final Color BG = new Color(0x0B0D14);
    public static final Color SIDEBAR = new Color(0x0F121B);
    public static final Color CARD = new Color(0x151926);
    public static final Color CARD_ALT = new Color(0x1B2031);
    public static final Color BORDER = new Color(0x262C41);
    public static final Color TEXT = new Color(0xE9ECF5);
    public static final Color MUTED = new Color(0x8B93AB);
    public static final Color ACCENT = new Color(0x7C5CFF);
    public static final Color ACCENT_2 = new Color(0x22D3EE);
    public static final Color GREEN = new Color(0x34D399);
    public static final Color RED = new Color(0xF87171);
    public static final Color AMBER = new Color(0xFBBF24);
    public static final Color PINK = new Color(0xF472B6);

    private static final String FAMILY = pickFamily();

    private Theme() {
    }

    private static String pickFamily() {
        String[] wanted = {"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Roboto", "Ubuntu"};
        java.util.Set<String> have = new java.util.HashSet<>(java.util.Arrays.asList(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String w : wanted) if (have.contains(w)) return w;
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, 1).deriveFont(style, size);
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static Color mix(Color a, Color b, float t) {
        float u = 1 - t;
        return new Color(
                Math.round(a.getRed() * u + b.getRed() * t),
                Math.round(a.getGreen() * u + b.getGreen() * t),
                Math.round(a.getBlue() * u + b.getBlue() * t));
    }

    public static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static void install() {
        System.setProperty("sun.java2d.opengl", "false");
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@accentColor", "#7C5CFF",
                "@background", "#0B0D14",
                "@foreground", "#E9ECF5"));
        FlatDarkLaf.setup();

        UIManager.put("defaultFont", font(Font.PLAIN, 13));
        UIManager.put("Panel.background", BG);
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("TextField.background", CARD_ALT);
        UIManager.put("PasswordField.background", CARD_ALT);
        UIManager.put("FormattedTextField.background", CARD_ALT);
        UIManager.put("Spinner.background", CARD_ALT);
        UIManager.put("ComboBox.background", CARD_ALT);
        UIManager.put("ComboBox.buttonBackground", CARD_ALT);
        UIManager.put("PopupMenu.background", CARD);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("Dialog.background", CARD);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.track", BG);
        UIManager.put("ScrollBar.thumb", new Color(0x2C3350));
        UIManager.put("ScrollBar.hoverThumbColor", new Color(0x3A4366));
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("Table.background", CARD);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", alpha(ACCENT, 70));
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", alpha(BORDER, 140));
        UIManager.put("TableHeader.background", CARD_ALT);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER);
        UIManager.put("TableHeader.separatorColor", CARD_ALT);
        UIManager.put("TableHeader.height", 42);
        UIManager.put("ToolTip.background", CARD_ALT);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("Label.foreground", TEXT);
    }
}
