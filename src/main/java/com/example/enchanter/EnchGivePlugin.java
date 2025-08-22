package com.example.enchanter;

import com.example.enchanter.cmd.GiveCommand;
import com.example.enchanter.cmd.PermissionGiveCommand;
import com.example.enchanter.cmd.GlowToggleCommand;
import com.example.enchanter.cmd.ColorCommand;
import com.example.enchanter.cmd.NameCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class EnchGivePlugin extends JavaPlugin implements Listener {

    // players explicitly granted enchanter.give (persisted)
    private final Set<UUID> giveAllowed = new HashSet<>();
    // live attachments per online player
    private final Map<UUID, PermissionAttachment> giveAttachments = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadAllowed();

        // Commands
        GiveCommand give = new GiveCommand();
        if (getCommand("give") != null) {
            getCommand("give").setExecutor(give);
            getCommand("give").setTabCompleter(give);
        }
        if (getCommand("egive") != null) {
            getCommand("egive").setExecutor(give);
            getCommand("egive").setTabCompleter(give);
        }

        // New: toggle /permissiongive (OP-only)
        PermissionGiveCommand pGive = new PermissionGiveCommand(this);
        if (getCommand("permissiongive") != null) {
            getCommand("permissiongive").setExecutor(pGive);
            getCommand("permissiongive").setTabCompleter(pGive);
        }

        // Your other commands if you added them:
        if (getCommand("glow") != null)
            getCommand("glow").setExecutor(new GlowToggleCommand(new org.bukkit.NamespacedKey(this, "easy_glow")));
        if (getCommand("color") != null)
            getCommand("color").setExecutor(new ColorCommand());
        if (getCommand("name") != null)
            getCommand("name").setExecutor(new NameCommand());

        // Listeners to apply/remove attachments
        getServer().getPluginManager().registerEvents(this, this);

        // Apply to any players already online (server reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (giveAllowed.contains(p.getUniqueId())) attachGive(p);
        }

        getLogger().info("EasyEnchant enabled.");
    }

    @Override
    public void onDisable() {
        // cleanly remove attachments
        for (Map.Entry<UUID, PermissionAttachment> e : giveAttachments.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && e.getValue() != null) p.removeAttachment(e.getValue());
        }
        giveAttachments.clear();
        saveAllowed();
    }

    // ===== Permission storage & application =====
    private void loadAllowed() {
        List<String> list = getConfig().getStringList("giveAllowed");
        giveAllowed.clear();
        for (String s : list) {
            try { giveAllowed.add(UUID.fromString(s)); } catch (Exception ignored) {}
        }
    }

    private void saveAllowed() {
        List<String> list = new ArrayList<>();
        for (UUID id : giveAllowed) list.add(id.toString());
        getConfig().set("giveAllowed", list);
        saveConfig();
    }

    public boolean toggleGive(UUID uuid) {
        boolean now;
        if (giveAllowed.contains(uuid)) {
            giveAllowed.remove(uuid);
            now = false;
        } else {
            giveAllowed.add(uuid);
            now = true;
        }
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            if (now) attachGive(p); else detachGive(p);
            p.recalculatePermissions();
        }
        saveAllowed();
        return now;
    }

    private void attachGive(Player p) {
        PermissionAttachment att = giveAttachments.get(p.getUniqueId());
        if (att == null) {
            att = p.addAttachment(this);
            giveAttachments.put(p.getUniqueId(), att);
        }
        att.setPermission("enchanter.give", true);
    }

    private void detachGive(Player p) {
        PermissionAttachment att = giveAttachments.remove(p.getUniqueId());
        if (att != null) p.removeAttachment(att);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (giveAllowed.contains(e.getPlayer().getUniqueId())) attachGive(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // optional: free attachment when they leave
        detachGive(e.getPlayer());
    }
}
