package qowax.gloire;

import org.bukkit.plugin.java.JavaPlugin;

public final class Gloire extends JavaPlugin {

    @Override
    public void onEnable() {

        // Fichier de configuration
        saveDefaultConfig();

        // CommandKit
        // Register our command "kit" (set an instance of your command class as executor)
        this.getCommand("gloire").setExecutor(new CommandKit());


        this.getCommand("topp").setExecutor(new CommandTop());


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Stopping...");
    }
}
