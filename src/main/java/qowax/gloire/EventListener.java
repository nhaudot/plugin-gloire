package qowax.gloire;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;

public class EventListener implements Listener {

    public static Database bdd = null;

    // Empêche le joueur de prendre les items du GUI "TOP - Gloire"
    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (e.getWhoClicked() instanceof Player){
            Player p = (Player) e.getWhoClicked();
            ItemStack is = e.getCurrentItem();
            if (is == null ||is.getType() == Material.AIR || is.getType() == null)
                return;
            if(p.getOpenInventory().getTitle().equalsIgnoreCase("TOP - Gloire")) {
                if(e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                    e.setCancelled(true);
                    return;
                }
                e.setCancelled(true);
            }
        }
    }

    // Quand un joueur tue un mob
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // getType() is inherited from Entity
        if(entity.getType() == EntityType.CREEPER) {
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

            Bukkit.getScheduler().runTaskAsynchronously(Gloire.plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        bdd.connect();
                        ArrayList<String> result = bdd.sendRequest("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + entity.getKiller().getUniqueId() + "'");
                        int gloire = Integer.parseInt(result.get(0)) + Integer.parseInt(Gloire.plugin.getConfig().getString("config.gloire_creeper"));
                        bdd.executeQuery("UPDATE `statistiques` SET `gloire`='" + String.valueOf(gloire) + "' WHERE `uuid` = '" + entity.getKiller().getUniqueId() + "'");
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

    // Quand un joueur rejoint le serveur
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(!player.hasPlayedBefore()) {
            // Fetch DDB pour retrouver si le joueur existe
            if (bdd == null) {
                bdd = new Database(
                        Gloire.plugin.getConfig().getString("database.host"),
                        Integer.parseInt(Gloire.plugin.getConfig().getString("database.port")),
                        Gloire.plugin.getConfig().getString("database.database"),
                        Gloire.plugin.getConfig().getString("database.username"),
                        Gloire.plugin.getConfig().getString("database.password")
                );
            }

            Bukkit.getScheduler().runTaskAsynchronously(Gloire.plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        bdd.connect();
                        ArrayList<String> result = bdd.sendRequest("SELECT `joueur` FROM `statistiques` WHERE `uuid` = '" + event.getPlayer().getUniqueId() + "'");

                        if (result.size() == 0) {
                            bdd.executeQuery("INSERT INTO `statistiques`(`uuid`, `joueur`, `gloire`) VALUES ('" + event.getPlayer().getUniqueId().toString() + "','" + event.getPlayer().getName() + "','" + Gloire.plugin.getConfig().getString("config.gloire_base") + "')");
                        }
                    } catch (SQLException throwables) {
                        Bukkit.getServer().getLogger().warning("Erreur MySQL : " + throwables.getMessage());
                        Bukkit.getServer().getLogger().warning ("Impossible de se connecter à la base de données, veuillez vérifier les informations dans le fichier config.yml");
                        Bukkit.getPluginManager().disablePlugin(Gloire.plugin);
                    }
                }
            });
        }

    }

}
