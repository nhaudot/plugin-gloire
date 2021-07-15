package qowax.gloire;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import qowax.gloire.Listeners.EventListener;
import qowax.gloire.Listeners.MobsListener;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public final class Gloire extends JavaPlugin {

    public static final String BUILD_MAJ = "1";
    public static final String BUILD_MIN = "0";
    public static final String BUILD_NUMBER = "63";

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

                    // Vérifie que la BDD est initialisée
                    ArrayList<String> result = bdd.sendRequest("SHOW TABLES LIKE 'statistiques'");
                    if (result.size() == 0) {
                        // On doit initialiser la base de données
                        String sqldump = new Scanner(Gloire.class.getResourceAsStream("/database.sql"), "UTF-8").useDelimiter("\\A").next();
                        getLogger().info("Création de la base de données...");
                        bdd.executeQuery(sqldump);
                        getLogger().info("Base de données ok!");
                    }
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
