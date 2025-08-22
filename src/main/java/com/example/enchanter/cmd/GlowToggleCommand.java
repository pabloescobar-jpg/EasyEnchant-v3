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
    // PDC mark: 1 = we added fake enchant; 2 = item already had enchants (we only hid them)
    private final NamespacedKey markKey;
    public GlowToggleCommand(NamespacedKey key){ this.markKey = key; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { sender.sendMessage("§6[EasyEnchant] §4Players only."); return true; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) { p.sendMessage("§6[EasyEnchant] §4Hold an item."); return true; }

        // fresh meta each branch to avoid stale writes
        ItemMeta meta = hand.getItemMeta();
        byte state = meta.getPersistentDataContainer().getOrDefault(markKey, PersistentDataType.BYTE, (byte)0);
        boolean turningOn = (state == 0);

        if (turningOn) {
            boolean addedByUs = !meta.hasEnchants();
            if (addedByUs) {
                // add a harmless fake enchant to trigger glint
                hand.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            }
            ItemMeta m = hand.getItemMeta();
            m.addItemFlags(ItemFlag.HIDE_ENCHANTS); // hide the list
            m.getPersistentDataContainer().set(markKey, PersistentDataType.BYTE, (byte)(addedByUs ? 1 : 2));
            hand.setItemMeta(m);

            p.updateInventory();
            p.sendMessage("§6[EasyEnchant] §6glow §4ON");
        } else {
            // remove hide flag first on a fresh meta
            ItemMeta m = hand.getItemMeta();
            m.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            m.getPersistentDataContainer().remove(markKey);
            hand.setItemMeta(m);

            // if we added the fake enchant earlier, remove it now
            if (state == 1) {
                hand.removeEnchantment(Enchantment.UNBREAKING);
            }

            p.updateInventory();
            p.sendMessage("§6[EasyEnchant] §6glow §4OFF");
        }
        return true;
    }
}
