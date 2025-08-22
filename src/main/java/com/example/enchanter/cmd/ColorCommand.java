package com.example.enchanter.cmd;

import net.kyori.adventure.text.Component;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ColorCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length < 1){ p.sendMessage("§6[EasyEnchant] §4Usage: /color <&code|#RRGGBB>"); return true; }

        String token = args[0].trim();
        if (!ColorUtil.isColorToken(token)){ p.sendMessage("§6[EasyEnchant] §4Invalid color."); return true; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()){ p.sendMessage("§6[EasyEnchant] §4Hold an item."); return true; }

        ItemMeta meta = hand.getItemMeta();

        String basePlain;
        if (meta.hasDisplayName() && meta.displayName() != null) {
            String currentLegacy = TextUtil.toLegacy(meta.displayName());
            basePlain = org.bukkit.ChatColor.stripColor(ColorUtil.colorize(currentLegacy));
        } else {
            String pretty = hand.getType().name().toLowerCase().replace('_',' ');
            basePlain = Character.toUpperCase(pretty.charAt(0)) + pretty.substring(1);
        }

        String finalLegacy = ColorUtil.colorize(ColorUtil.normalizeSpaces(token + basePlain));
        meta.displayName(TextUtil.legacyNoItalics(finalLegacy));
        hand.setItemMeta(meta);

        p.sendMessage("§6[EasyEnchant] §6color changed");
        return true;
    }
}
