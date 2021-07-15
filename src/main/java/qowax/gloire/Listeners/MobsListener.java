package qowax.gloire.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import qowax.gloire.Database;
import qowax.gloire.Gloire;

import java.sql.SQLException;
import java.util.ArrayList;

public class MobsListener implements Listener {

    private static Database bdd = null;

    // Quand un joueur tue un mob/joueur
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Connexion à la BDD pour vérifier si le joueur est présent
        if (bdd == null) {
            bdd = new Database(
                    Gloire.plugin.getConfig().getString("database.host"),
                    Integer.parseInt(Gloire.plugin.getConfig().getString("database.port")),
                    Gloire.plugin.getConfig().getString("database.database"),
                    Gloire.plugin.getConfig().getString("database.username"),
                    Gloire.plugin.getConfig().getString("database.password")
            );
        }

        // Envoi requête SQL
        Bukkit.getScheduler().runTaskAsynchronously(Gloire.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    bdd.connect();
                    ArrayList<String> result = bdd.sendRequest("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + entity.getKiller().getUniqueId() + "'");
                    int nouveauGloire = Integer.parseInt(result.get(0)) + Integer.parseInt(Gloire.plugin.getConfig().getString("config.gloire_" + event.getEntity().toString().toLowerCase().substring(5)));
                    bdd.executeQuery("UPDATE `statistiques` SET `gloire`='" + String.valueOf(nouveauGloire) + "' WHERE `uuid` = '" + entity.getKiller().getUniqueId() + "'");
                    entity.getKiller().sendMessage("Vous avez gagné " + Gloire.plugin.getConfig().getString("config.gloire_creeper") + " points de Gloire!");
                } catch (SQLException throwables) {
                    Bukkit.getServer().getLogger().warning("Erreur MySQL : " + throwables.getMessage());
                    Bukkit.getServer().getLogger().warning ("Impossible de se connecter à la base de données, veuillez vérifier les informations dans le fichier config.yml");
                    Bukkit.getPluginManager().disablePlugin(Gloire.plugin);
                }
            }
        });
    }
}
