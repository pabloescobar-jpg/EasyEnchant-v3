package com.example.enchanter.cmd;

import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class GlowToggleCommand implements CommandExecutor {
    private final NamespacedKey markKey; // marks if WE added the fake enchant (1) or just toggled hide (2)
    public GlowToggleCommand(NamespacedKey key){ this.markKey = key; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()){ p.sendMessage("§6[EasyEnchant] §4Hold an item."); return true; }

        ItemMeta meta = hand.getItemMeta();
        byte state = meta.getPersistentDataContainer().getOrDefault(markKey, PersistentDataType.BYTE, (byte)0);
        boolean turningOn = (state == 0); // off -> on, otherwise on -> off

        if (turningOn){
            // Ensure at least one enchant to make it glow.
            boolean addedByUs = false;
            if (!meta.hasEnchants()) {
                hand.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
                addedByUs = true;
            }
            // Remember what we did so we can undo only our changes.
            meta = hand.getItemMeta();
            meta.getPersistentDataContainer().set(markKey, PersistentDataType.BYTE, (byte)(addedByUs ? 1 : 2));
            hand.setItemMeta(meta);

            p.updateInventory();
            p.sendMessage("§6[EasyEnchant] §6glow §4ON");
        } else {
            // Turn off: if we added a fake enchant earlier (state==1), remove it.
            if (state == 1) {
                hand.removeEnchantment(Enchantment.UNBREAKING);
            }
            meta.getPersistentDataContainer().remove(markKey);
            hand.setItemMeta(meta);

            p.updateInventory();
            p.sendMessage("§6[EasyEnchant] §6glow §4OFF");
        }
        return true;
    }
}
