package com.example.enchanter.cmd;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ColorCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { Msg.tell(sender, Msg.fill("Players only.")); return true; }
        if (args.length < 1){ Msg.usage(sender, "/color <&code|#RRGGBB>"); return true; }

        String token = args[0].trim();
        if (!ColorUtil.isColorToken(token)){ Msg.tell(p, Msg.fill("Invalid color.")); return true; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()){ Msg.tell(p, Msg.fill("Hold an item.")); return true; }

        ItemMeta meta = hand.getItemMeta();

        // Use the current plain name text if present, otherwise a pretty material name
        String basePlain;
        if (meta.hasDisplayName() && meta.displayName() != null) {
            String currentLegacy = TextUtil.toLegacy(meta.displayName());
            basePlain = ChatColor.stripColor(ColorUtil.colorize(currentLegacy));
        } else {
            String pretty = hand.getType().name().toLowerCase().replace('_',' ');
            basePlain = Character.toUpperCase(pretty.charAt(0)) + pretty.substring(1);
        }

        String legacy = ColorUtil.stripSpaceAfterLeadingColor(token + basePlain); // fixes "&2 Name" gap
        legacy = ColorUtil.colorize(ColorUtil.normalizeSpaces(legacy));
        meta.displayName(TextUtil.legacyNoItalics(legacy));
        hand.setItemMeta(meta);

        Msg.tell(p, "color changed");
        return true;
    }
}
