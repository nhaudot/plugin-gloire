package qowax.gloire;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import qowax.gloire.Listeners.EventListener;
import qowax.gloire.Listeners.MobsListener;

import java.sql.SQLException;

public final class Gloire extends JavaPlugin {

    public static final String BUILD_MAJ = "1";
    public static final String BUILD_MIN = "0";
    public static final String BUILD_NUMBER = "41";

    public static Database bdd;
    public static Plugin plugin;

    @Override
    public void onEnable() {
        // Setup plugin
        saveDefaultConfig();
        plugin = this;
        getLogger().info("Chargement de Gloire...");
        getLogger().info("Version " + BUILD_MAJ + "." + BUILD_MIN + "." + BUILD_NUMBER);

        // Setup connexion à la base de données
        bdd = new Database(
                getConfig().getString("database.host"),
                Integer.parseInt(getConfig().getString("database.port")),
                getConfig().getString("database.database"),
                getConfig().getString("database.username"),
                getConfig().getString("database.password")
        );

        // Connexion à la BDD
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    bdd.connect();
                } catch (SQLException throwables) {
                    getLogger().warning("Erreur MySQL : " + throwables.getMessage());
                    getLogger().warning ("Impossible de se connecter à la base de données, veuillez vérifier les informations dans le fichier config.yml");
                    Bukkit.getPluginManager().disablePlugin(plugin);
                }
            }
        });

        // Déclaration commandes
        this.getCommand("gloire").setExecutor(new CommandGloire(bdd, plugin));

        // Listener
        getServer().getPluginManager().registerEvents(new EventListener(), plugin);
        getServer().getPluginManager().registerEvents(new MobsListener(), plugin);
    }

    @Override
    public void onDisable() {

    }
}
