package com.example.enchanter.cmd;

import net.kyori.adventure.text.Component;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class NameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length < 1){ sender.sendMessage("§6[EasyEnchants] §4Usage: /name <name...>"); return true; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()){ p.sendMessage("§6[EasyEnchants] §4Hold an item."); return true; }

        ItemMeta meta = hand.getItemMeta();

        // Keep existing color if present, but kill italics
        String existingLegacy = "";
        if (meta.hasDisplayName()) {
            Component c = meta.displayName();
            if (c != null) existingLegacy = TextUtil.toLegacy(c);
        }
        String lastColors = org.bukkit.ChatColor.getLastColors(ColorUtil.colorize(existingLegacy));
        String nameRaw = ColorUtil.normalizeSpaces(String.join(" ", Arrays.asList(args)));
        String finalLegacy = ColorUtil.colorize((lastColors == null ? "" : lastColors) + nameRaw);

        meta.displayName(TextUtil.legacyNoItalics(finalLegacy));
        hand.setItemMeta(meta);

        p.sendMessage("§6[EasyEnchants] §6name changed");
        return true;
    }
}
