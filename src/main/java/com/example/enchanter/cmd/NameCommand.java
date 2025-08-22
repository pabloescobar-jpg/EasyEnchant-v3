package com.example.enchanter.cmd;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class NameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length < 1){ sender.sendMessage("§eUsage: /name <name...>"); return true; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()){ p.sendMessage("§cHold an item."); return true; }

        String name = String.join(" ", Arrays.asList(args));
        ItemMeta meta = hand.getItemMeta();

        // keep existing leading color if present
        String prefix = org.bukkit.ChatColor.getLastColors(meta.hasDisplayName() ? meta.getDisplayName() : "");
        meta.setDisplayName(ColorUtil.colorize(prefix + name));
        hand.setItemMeta(meta);

        p.sendMessage("§a[EasyEnchants] §fname changed");
        return true;
    }
}
