package com.example.enchanter;

import com.example.enchanter.cmd.*;
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

    // Persist who is allowed to use /give (non-OPs)
    private final Set<UUID> giveAllowed = new HashSet<>();
    private final Map<UUID, PermissionAttachment> giveAttachments = new HashMap<>();

    @Override
    public void onEnable() {
        // Config holds giveAllowed UUID list
        saveDefaultConfig();
        loadAllowed();

        // Register /give + /egive
        GiveCommand give = new GiveCommand();
        if (getCommand("give") != null) {
            getCommand("give").setExecutor(give);
            getCommand("give").setTabCompleter(give);
        }
        if (getCommand("egive") != null) {
            getCommand("egive").setExecutor(give);
            getCommand("egive").setTabCompleter(give);
        }

        // Intercept raw "/give" so ours wins over Essentials/vanilla
        getServer().getPluginManager().registerEvents(new GiveInterceptor(this, give), this);

        // /permissiongive (OP-only)
        PermissionGiveCommand pGive = new PermissionGiveCommand(this);
        if (getCommand("permissiongive") != null) {
            getCommand("permissiongive").setExecutor(pGive);
            getCommand("permissiongive").setTabCompleter(pGive);
        }

        // /glow, /color, /name
        if (getCommand("glow") != null)
            getCommand("glow").setExecutor(new GlowToggleCommand(new org.bukkit.NamespacedKey(this, "easy_glow")));
        if (getCommand("color") != null)
            getCommand("color").setExecutor(new ColorCommand());
        if (getCommand("name") != null)
            getCommand("name").setExecutor(new NameCommand());

        // Listener for applying saved perms and cleanup
        getServer().getPluginManager().registerEvents(this, this);

        // Re-apply attachment on reload
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (giveAllowed.contains(p.getUniqueId())) attachGive(p);
        }

        getLogger().info("EasyEnchantments enabled.");
    }

    @Override
    public void onDisable() {
        // Remove attachments cleanly
        for (PermissionAttachment att : giveAttachments.values()) {
            try {
                att.getPermissible().removeAttachment(att);
            } catch (Exception ignored) {}
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

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        if (giveAllowed.contains(e.getPlayer().getUniqueId())) attachGive(e.getPlayer());
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) {
        detachGive(e.getPlayer());
    }
}
