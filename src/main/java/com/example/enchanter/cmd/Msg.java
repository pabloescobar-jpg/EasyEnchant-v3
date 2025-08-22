package com.example.enchanter.cmd;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class Msg {
    private Msg(){}

    public static final String BASE = "§6";   // orange
    public static final String FILL = "§4";   // dark red
    public static final String PREFIX = BASE + "[EasyEnchant] " + BASE;

    public static String fill(Object o) { return FILL + String.valueOf(o) + BASE; }

    public static void tell(CommandSender to, String text) { to.sendMessage(PREFIX + text); }

    public static void usage(CommandSender to, String usage) { tell(to, "Usage: " + fill(usage)); }

    public static void broadcastGive(int amount, String material, String playerName) {
        Bukkit.getServer().broadcastMessage(
            BASE + "Gave " + fill(amount) + " " + BASE + "of " + fill(material) + " " + BASE + "to " + fill(playerName)
        );
    }
}
