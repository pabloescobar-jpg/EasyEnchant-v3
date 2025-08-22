package com.example.enchanter.cmd;

import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ColorCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length < 1){ sender.sendMessage("§eUsage: /color <&code|#RRGGBB>"); return true; }

        String token = args[0];
        if (!ColorUtil.isColorToken(token)){ p.sendMessage("§cInvalid color. Use &0–&f, &k–&o, &r or #RRGGBB."); return true; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()){ p.sendMessage("§cHold an item."); return true; }

        ItemMeta meta = hand.getItemMeta();
        String base = (meta.hasDisplayName() ? meta.getDisplayName() : pretty(hand.getType()));
        // strip existing color, then re-apply chosen color
        String plain = org.bukkit.ChatColor.stripColor(ColorUtil.colorize(base));
        meta.setDisplayName(ColorUtil.colorize(token + plain));
        hand.setItemMeta(meta);
        p.sendMessage("§a[EasyEnchant] §fcolor changed");
        return true;
    }

    private static String pretty(Material m){
        String s = m.name().toLowerCase().replace('_',' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
