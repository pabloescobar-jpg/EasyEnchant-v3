package com.example.enchanter.cmd;

import org.bukkit.ChatColor;
import java.util.regex.*;

public final class ColorUtil {
    private ColorUtil(){}
    private static final Pattern HEX = Pattern.compile("(?i)(?:&?#)([0-9a-f]{6})");

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

    public static boolean isColorToken(String t){
        if (t == null) return false;
        t = t.trim();
        return t.matches("(?i)&[0-9A-FK-OR]") || t.matches("(?i)&?#[0-9A-F]{6}");
    }

    public static String normalizeSpaces(String s){
        if (s == null) return null;
        return s.replaceAll("\\s+", " ").trim();
    }
}
