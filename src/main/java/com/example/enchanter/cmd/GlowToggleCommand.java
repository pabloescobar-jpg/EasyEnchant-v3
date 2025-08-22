package com.example.enchanter.cmd;

import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class GlowToggleCommand implements CommandExecutor {
    // 1 = we added fake enchant; 2 = item already had enchants and we only hid the list
    private final NamespacedKey markKey;
    public GlowToggleCommand(NamespacedKey key){ this.markKey = key; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { Msg.tell(sender, Msg.fill("Players only.")); return true; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()){ Msg.tell(p, Msg.fill("Hold an item.")); return true; }

        ItemMeta meta = hand.getItemMeta();
        byte state = meta.getPersistentDataContainer().getOrDefault(markKey, PersistentDataType.BYTE, (byte)0);
        boolean turningOn = (state == 0);

        if (turningOn){
            boolean addedByUs = !meta.hasEnchants();
            if (addedByUs) hand.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            ItemMeta m = hand.getItemMeta();
            m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            m.getPersistentDataContainer().set(markKey, PersistentDataType.BYTE, (byte)(addedByUs ? 1 : 2));
            hand.setItemMeta(m);
            p.updateInventory();
            Msg.tell(p, "glow " + Msg.fill("ON"));
        } else {
            // remove hide flag + our mark
            ItemMeta m = hand.getItemMeta();
            m.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            m.getPersistentDataContainer().remove(markKey);
            hand.setItemMeta(m);
            // if we added fake enchant earlier, remove it now
            if (state == 1) hand.removeEnchantment(Enchantment.UNBREAKING);
            p.updateInventory();
            Msg.tell(p, "glow " + Msg.fill("OFF"));
        }
        return true;
    }
}

