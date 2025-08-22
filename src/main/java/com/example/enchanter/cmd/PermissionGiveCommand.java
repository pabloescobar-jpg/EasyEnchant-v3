package com.example.enchanter.cmd;

import com.example.enchanter.EnchGivePlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PermissionGiveCommand implements CommandExecutor, TabCompleter {

    private final EnchGivePlugin plugin;

    public PermissionGiveCommand(EnchGivePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.isOp()) {
            Msg.tell(sender, Msg.fill("OP only."));
            return true;
        }
        if (args.length != 1) {
            Msg.usage(sender, "/" + label + " <player>");
            return true;
        }

        String token = args[0];
        UUID uuid;
        String display;

        // Prefer online player exact match
        Player online = Bukkit.getPlayerExact(token);
        if (online != null) {
            uuid = online.getUniqueId();
            display = online.getName();
        } else {
            // Fallback to offline cache; if not cached, create an OfflinePlayer to get a UUID
            OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(token);
            if (off == null) off = Bukkit.getOfflinePlayer(token);
            uuid = off.getUniqueId();
            display = off.getName() != null ? off.getName() : token;
        }

        boolean grantedNow = plugin.toggleGive(uuid);
        if (grantedNow) {
            Msg.tell(sender, Msg.fill(display) + " has received " + Msg.fill("GIVE") + " permission");
        } else {
            Msg.tell(sender, Msg.fill(display) + " has lost " + Msg.fill("GIVE") + " permission");
        }
        return true;
    }

    // simple player name completion
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.isOp()) return Collections.emptyList();
        if (args.length != 1) return Collections.emptyList();

        String pfx = args[0].toLowerCase();
        List<String> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String n = p.getName();
            if (n.toLowerCase().startsWith(pfx)) out.add(n);
        }
        Collections.sort(out);
        return out;
        }
}
