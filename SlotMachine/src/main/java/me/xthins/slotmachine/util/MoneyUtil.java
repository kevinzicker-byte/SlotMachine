package me.xthins.slotmachine.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtil {

    private static final NumberFormat COMMA_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private MoneyUtil() {
    }

    public static String formatCommas(double value) {
        return COMMA_FORMAT.format(Math.round(value));
    }

    public static String formatCommas(long value) {
        return COMMA_FORMAT.format(value);
    }

    public static String formatShort(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000_000.0) return trim(value / 1_000_000_000_000.0) + "T";
        if (abs >= 1_000_000_000.0) return trim(value / 1_000_000_000.0) + "B";
        if (abs >= 1_000_000.0) return trim(value / 1_000_000.0) + "M";
        if (abs >= 1_000.0) return trim(value / 1_000.0) + "K";
        return String.valueOf(Math.round(value));
    }

    public static String formatMoneyCommas(String symbol, double value) {
        return symbol + formatCommas(value);
    }

    public static String formatMoneyShort(String symbol, double value) {
        return symbol + formatShort(value);
    }

    private static String trim(double value) {
        String formatted = String.format(Locale.US, "%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }
}
