package com.example.enchanter.cmd;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class NameCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { Msg.tell(sender, Msg.fill("Players only.")); return true; }
        if (args.length < 1){ Msg.usage(sender, "/name <name...>"); return true; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()){ Msg.tell(p, Msg.fill("Hold an item.")); return true; }

        ItemMeta meta = hand.getItemMeta();

        // Preserve existing color (if any), but kill italics on the new name
        String existingLegacy = "";
        if (meta.hasDisplayName()) {
            Component c = meta.displayName();
            if (c != null) existingLegacy = TextUtil.toLegacy(c);
        }

        String lastColors = ChatColor.getLastColors(ColorUtil.colorize(existingLegacy));
        String nameRaw    = ColorUtil.normalizeSpaces(String.join(" ", Arrays.asList(args)));

        String combined   = (lastColors == null ? "" : lastColors) + nameRaw;
        combined          = ColorUtil.stripSpaceAfterLeadingColor(combined); // fix "&a Name" leading space
        String finalLegacy= ColorUtil.colorize(ColorUtil.normalizeSpaces(combined));

        meta.displayName(TextUtil.legacyNoItalics(finalLegacy));
        hand.setItemMeta(meta);

        Msg.tell(p, "name changed");
        return true;
    }
}

