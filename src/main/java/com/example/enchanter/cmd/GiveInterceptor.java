package com.example.enchanter.cmd;

import com.example.enchanter.EnchGivePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Arrays;
import java.util.List;

public class GiveInterceptor implements Listener {

    private final EnchGivePlugin plugin;
    private final GiveCommand delegate;

    public GiveInterceptor(EnchGivePlugin plugin, GiveCommand delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
    }

    // Grab /give before vanilla/other plugins see it
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPreprocess(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().trim();
        if (!msg.startsWith("/")) return;

        String lower = msg.toLowerCase();
        if (lower.startsWith("/give ") || lower.equals("/give")) {
            String[] split = msg.substring(1).split("\\s+"); // remove leading '/'
            // split[0] is "give", the rest are args
            String[] args = Arrays.copyOfRange(split, 1, split.length);

            if (!e.getPlayer().hasPermission("enchanter.give")) {
                e.getPlayer().sendMessage("§cYou lack permission: enchanter.give");
                e.setCancelled(true);
                return;
            }

            // Execute our give command logic
            delegate.onCommand(e.getPlayer(), findBukkitGiveCommand(), "give", args);
            e.setCancelled(true); // block vanilla/other plugins
        }
    }

    // Provide completions for "/give " (vanilla path)
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onTab(TabCompleteEvent e) {
        String buffer = e.getBuffer();
        if (buffer == null) return;
        String lb = buffer.toLowerCase();
        if (lb.startsWith("/give ")) {
            String noSlash = buffer.substring(1);
            String[] parts = noSlash.split("\\s+");
            String[] args = parts.length <= 1 ? new String[0] : Arrays.copyOfRange(parts, 1, parts.length);
            List<String> res = delegate.onTabComplete(e.getSender(), findBukkitGiveCommand(), "give", args);
            if (res != null) e.setCompletions(res);
        }
    }

    private Command findBukkitGiveCommand() {
        // This Command object is not used for routing, only to satisfy the signature.
        // Bukkit will ignore it because we already intercepted the raw text.
        return Bukkit.getPluginCommand("give");
    }
}
