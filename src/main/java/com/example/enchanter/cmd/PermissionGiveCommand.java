package com.example.enchanter.cmd;

import com.example.enchanter.EnchGivePlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class PermissionGiveCommand implements CommandExecutor, TabCompleter {

    private final EnchGivePlugin plugin;

    public PermissionGiveCommand(EnchGivePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // OP-only (console allowed)
        if (!(sender instanceof ConsoleCommandSender) && !sender.isOp()) {
            sender.sendMessage("§cYou must be OP to use /" + label + ".");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§eUsage: /" + label + " <player>");
            return true;
        }

        // Prefer online player; fallback to offline by name
        Player online = Bukkit.getPlayerExact(args[0]);
        UUID uuid;
        String display;
        if (online != null) {
            uuid = online.getUniqueId();
            display = online.getName();
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer off = Bukkit.getOfflinePlayer(args[0]);
            if (off == null || (off.getName() == null && !off.hasPlayedBefore())) {
                sender.sendMessage("§cPlayer not found: " + args[0]);
                return true;
            }
            uuid = off.getUniqueId();
            display = off.getName() != null ? off.getName() : args[0];
        }

        boolean grantedNow = plugin.toggleGive(uuid);
        if (grantedNow) {
            sender.sendMessage("§a[EasyEnchant] §f" + display + " §ahas received §fGIVE §apermission");
        } else {
            sender.sendMessage("§a[EasyEnchant] §f" + display + " §ahas lost §fGIVE §apermission");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.isOp()) return Collections.emptyList();
        if (args.length == 1) {
            String pfx = args[0].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(pfx)) names.add(p.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
