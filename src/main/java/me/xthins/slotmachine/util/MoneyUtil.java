package me.xthins.slotmachine.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtil {
    private static final NumberFormat COMMA_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private MoneyUtil() {}

    public static String formatCommas(double value) {
        return COMMA_FORMAT.format(Math.round(value));
    }

    public static String formatShort(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000_000.0) return trim(value / 1_000_000_000_000.0) + "T";
        if (abs >= 1_000_000_000.0) return trim(value / 1_000_000_000.0) + "B";
        if (abs >= 1_000_000.0) return trim(value / 1_000_000.0) + "M";
        if (abs >= 1_000.0) return trim(value / 1_000.0) + "K";
        return String.valueOf(Math.round(value));
    }

    public static String moneyCommas(double value) { return "$" + formatCommas(value); }
    public static String moneyShort(double value) { return "$" + formatShort(value); }

    private static String trim(double value) {
        String s = String.format(Locale.US, "%.1f", value);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
