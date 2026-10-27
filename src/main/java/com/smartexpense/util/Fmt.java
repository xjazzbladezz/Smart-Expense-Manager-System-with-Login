package com.smartexpense.util;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatting helpers (Indian numbering, dates). */
public final class Fmt {

    private static final Locale IN = new Locale.Builder().setLanguage("en").setRegion("IN").build();

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter MONTH_SHORT = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    public static final DateTimeFormatter MONTH_YEAR_SHORT = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);

    private Fmt() {
    }

    /** ₹1,23,456.00 */
    public static String money(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(IN);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return (v < 0 ? "-" : "") + "₹" + nf.format(Math.abs(v));
    }

    /** ₹1,23,456 (no decimals) */
    public static String moneyWhole(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(IN);
        nf.setMaximumFractionDigits(0);
        return (v < 0 ? "-" : "") + "₹" + nf.format(Math.abs(v));
    }

    /** ₹1.2L, ₹45K, ₹1.4Cr */
    public static String moneyShort(double v) {
        double a = Math.abs(v);
        String sign = v < 0 ? "-" : "";
        if (a >= 1e7) return sign + "₹" + trim(a / 1e7) + "Cr";
        if (a >= 1e5) return sign + "₹" + trim(a / 1e5) + "L";
        if (a >= 1e3) return sign + "₹" + trim(a / 1e3) + "K";
        return sign + "₹" + (long) Math.round(a);
    }

    private static String trim(double d) {
        String s = String.format(Locale.ENGLISH, "%.1f", d);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    public static String pct(double ratio) {
        return String.format(Locale.ENGLISH, "%.0f%%", ratio * 100);
    }

    public static String pct1(double ratio) {
        return String.format(Locale.ENGLISH, "%.1f%%", ratio * 100);
    }
}
