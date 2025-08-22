package com.example.enchanter.cmd;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private ColorUtil() {}

    // & #RRGGBB  or  #RRGGBB  → converts to §x§R§R§G§G§B§B
    private static final Pattern HEX = Pattern.compile("(?i)(?:&?#)([0-9a-f]{6})");

    /** Apply hex (&#RRGGBB / #RRGGBB) and legacy (&a etc) color codes. */
    public static String colorize(String s){
        if (s == null || s.isEmpty()) return s;
        Matcher m = HEX.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()){
            String h = m.group(1).toLowerCase();
            String repl = "§x§" + h.charAt(0)+"§"+h.charAt(1)+"§"+h.charAt(2)+"§"+h.charAt(3)+"§"+h.charAt(4)+"§"+h.charAt(5);
            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /** True if token looks like a color (&a…&f, &k…&r, or #RRGGBB / &#RRGGBB). */
    public static boolean isColorToken(String t){
        if (t == null) return false;
        String v = t.trim();
        return v.matches("(?i)&[0-9A-FK-OR]") || v.matches("(?i)&?#[0-9A-F]{6}");
    }

    /** Collapse whitespace and trim ends. */
    public static String normalizeSpaces(String s){
        if (s == null) return null;
        return s.replaceAll("\\s+", " ").trim();
    }

    /** If the string starts with color code(s) then a space (e.g. "&2 Name"), drop that space. */
    public static String stripSpaceAfterLeadingColor(String s){
        if (s == null) return null;
        return s.replaceFirst("(?i)^((?:&#[0-9A-F]{6}|#[0-9A-F]{6}|&[0-9A-FK-OR]|§[0-9A-FK-OR])+)[ \\t]+", "$1");
    }
}
