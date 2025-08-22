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
    private final NamespacedKey markKey;
    public GlowToggleCommand(NamespacedKey key){ this.markKey = key; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()){ p.sendMessage("§cHold an item."); return true; }

        ItemMeta meta = hand.getItemMeta();
        boolean on = meta.getPersistentDataContainer().getOrDefault(markKey, PersistentDataType.BYTE, (byte)0) == 1;

        if (!on){
            if (!meta.hasEnchants()) hand.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(markKey, PersistentDataType.BYTE, (byte)1);
            hand.setItemMeta(meta);
            p.sendMessage("§a[EasyEnchant] §fglow §aON");
        }else{
            // only remove our fake glow if we set it
            hand.removeEnchantment(Enchantment.UNBREAKING);
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().remove(markKey);
            hand.setItemMeta(meta);
            p.sendMessage("§a[EasyEnchant] §fglow §aOFF");
        }
        return true;
    }
}
