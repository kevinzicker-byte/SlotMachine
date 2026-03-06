package me.xthins.slotmachine.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class ColorUtil {
    private ColorUtil() {}

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public static List<String> color(List<String> input) {
        List<String> out = new ArrayList<>();
        for (String line : input) out.add(color(line));
        return out;
    }
}
