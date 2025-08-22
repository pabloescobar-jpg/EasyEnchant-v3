package com.example.enchanter;

import com.example.enchanter.cmd.GiveCommand;
import com.example.enchanter.cmd.GiveInterceptor;
import org.bukkit.plugin.java.JavaPlugin;

public final class EnchGivePlugin extends JavaPlugin {

    private GiveCommand giveCommand;

    @Override
    public void onEnable() {
        this.giveCommand = new GiveCommand(this);

        // Register our commands from plugin.yml
        if (getCommand("give") != null) {
            getCommand("give").setExecutor(giveCommand);
            getCommand("give").setTabCompleter(giveCommand);
        }
        if (getCommand("egive") != null) {
            getCommand("egive").setExecutor(giveCommand);
            getCommand("egive").setTabCompleter(giveCommand);
        }

        // Intercept vanilla/other-plugin /give very early
        getServer().getPluginManager().registerEvents(new GiveInterceptor(this, giveCommand), this);

        getLogger().info("EnchantedGive enabled.");
    }
}
