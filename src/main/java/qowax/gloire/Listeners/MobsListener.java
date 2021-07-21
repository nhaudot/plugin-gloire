package qowax.gloire.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import qowax.gloire.Database;
import qowax.gloire.Gloire;
import qowax.gloire.Rank;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class MobsListener implements Listener {

    public static Database bddFightTable = null;

    // Quand un joueur perd de la vie à cause d'un autre joueur
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        // S'il s'agit d'un joueur qui frappe un autre joueur
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            //e.getDamager().sendMessage("Dommages infligés : " + e.getDamage());
            //e.getDamager().sendMessage("Entitée damagée : " + e.getEntity());
            bddFightTable.query("INSERT INTO `fightTable`(`ennemy`, `damagedPlayer`, `hearts`) VALUES ('" + e.getDamager().getUniqueId() + "', '" + e.getEntity().getUniqueId() + "', '" + String.format("%.1f", e.getDamage()).replace(",", ".") + "');", false);
        }
    }

    // Quand un joueur tue un mob/joueur
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // On vérifie que le tueur est un joueur
        if (entity.getKiller() instanceof Player) {
            Bukkit.getServer().getLogger().info("HUMAIN TUE HUMAIN");
            // On vérifie si le tueur a tué un joueur ou non
            if (event.getEntity() instanceof Player) {
                // Nombre total de coeurs enlevés (tous joueurs confondus)
                ArrayList<String> totalHeartValue = bddFightTable.query("SELECT SUM(`hearts`) FROM `fightTable` WHERE `damagedPlayer`='" + event.getEntity().getUniqueId().toString() + "'", true);
                // Liste des ennemis
                ArrayList<String> listeEnnemis = bddFightTable.query("SELECT DISTINCT `ennemy` FROM `fightTable` WHERE `damagedPlayer`='" + event.getEntity().getUniqueId().toString() + "'", true);
                // Nombre de Gloire joueur tué
                ArrayList<String> gloireJoueurTue = bddFightTable.query("SELECT `gloire` FROM `statistiques` WHERE `uuid`='" + event.getEntity().getUniqueId().toString() + "'", true);
                // HASHMAP STACKS (joueur, valeurStack)
                HashMap<String, String> coeursEnlevesParEnnemis = new HashMap<>();
                // Attribution des points de Gloire aux gagnants
                for (int i = 0; i < listeEnnemis.size(); i++) {
                    // On remplit la hashmap
                    ArrayList<String> ennemi = bddFightTable.query("SELECT SUM(`hearts`) FROM `fightTable` WHERE `ennemy`='" + listeEnnemis.get(i) + "' AND `damagedPlayer`='" + event.getEntity().getUniqueId().toString() + "'", true);
                    coeursEnlevesParEnnemis.put(listeEnnemis.get(i), ennemi.get(0));
                    // On récupère les points de Gloire de ce tueur
                    ArrayList<String> pointsGloireTueur = bddFightTable.query("SELECT `gloire` FROM `statistiques` WHERE `uuid`='" + listeEnnemis.get(i) + "'", true);
                    // Calcul de la part du tueur
                    double coefficient = Double.valueOf(coeursEnlevesParEnnemis.get(listeEnnemis.get(i))) / Double.valueOf(totalHeartValue.get(0));
                    //entity.getKiller().sendMessage("Coefficient " + listeEnnemis.get(i) + " : " + coefficient + "%");
                    // gloirePourJoueurRatio1 = gloireVictoire (1000 par défaut) + 10% gloireJoueurTue
                    double gloirePourJoueurRatio1 = Double.valueOf(Gloire.plugin.getConfig().getString("config.gloire_joueur")) + (0.1 * Double.valueOf(gloireJoueurTue.get(0)));
                    double pointsGloire = Double.valueOf(pointsGloireTueur.get(0)) + (gloirePourJoueurRatio1 * coefficient);
                    //entity.getKiller().sendMessage("Points de Gloire du tueur ORIGINE: " + pointsGloireTueur.get(0));
                    //entity.getKiller().sendMessage("Points de Gloire joueur GAGNES" + i + " : " + gloirePourJoueurRatio1 * coefficient);
                    //entity.getKiller().sendMessage("NOUVEAUX points de Gloire joueur " + i + " : " + pointsGloire);
                    // On met à jour le nombre de Gloire du joueur
                    bddFightTable.query("UPDATE `statistiques` SET `gloire`='" + (int) pointsGloire + "' WHERE `uuid` = '" + listeEnnemis.get(i) + "'", false);
                    // On supprime les occurences de la table concernant le joueur tué
                    bddFightTable.query("DELETE FROM `fightTable` WHERE `damagedPlayer`='" + event.getEntity().getUniqueId() + "'", false);

                    // On recharge le rang
                    String strPlayerUUID = listeEnnemis.get(i).replace("-", "");
                    UUID playerUUID = new UUID(new BigInteger(strPlayerUUID.substring(0, 16), 16).longValue(), new BigInteger(strPlayerUUID.substring(16), 16).longValue());
                    if (Bukkit.getServer().getPlayer(playerUUID) != null) {
                        // Joueur en ligne
                        Bukkit.getServer().getPlayer(playerUUID).sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.gloire_gagnant").replace("%s", String.valueOf(gloirePourJoueurRatio1)).replace("%t", String.valueOf(100 * coefficient))));
                        Rank.loadRank(Bukkit.getServer().getPlayer(playerUUID));
                    }
                }
                // Suppression de points de Gloire pour le joueur tué
                double gloirePourJoueurTue = (Double.valueOf(Gloire.plugin.getConfig().getString("config.gloire_joueur_perdant"))) + (0.07 * Double.valueOf(gloireJoueurTue.get(0)));
                if (Double.valueOf(gloireJoueurTue.get(0)) - gloirePourJoueurTue <= 0) {
                    bddFightTable.query("UPDATE `statistiques` SET `gloire`='0' WHERE `uuid` = '" + event.getEntity().getUniqueId().toString() + "'", false);
                    // On informe le joueur
                    entity.sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.gloire_perdant").replace("%s", String.valueOf(gloireJoueurTue))));
                } else {
                    bddFightTable.query("UPDATE `statistiques` SET `gloire`='" + (int) (Double.valueOf(gloireJoueurTue.get(0)) - gloirePourJoueurTue) + "' WHERE `uuid` = '" + event.getEntity().getUniqueId().toString() + "'", false);
                    // On informe le joueur
                    entity.sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.gloire_perdant").replace("%s", String.valueOf(gloirePourJoueurTue))));
                }
                // On recharge le rang du joueur tué
                Rank.loadRank((Player) entity);
            }
            else
            {
                // Envoi requête SQL
                Bukkit.getScheduler().runTaskAsynchronously(Gloire.plugin, () -> {
                    try {
                        Database bdd = new Database(
                                Gloire.plugin.getConfig().getString("database.host"),
                                Integer.parseInt(Gloire.plugin.getConfig().getString("database.port")),
                                Gloire.plugin.getConfig().getString("database.database"),
                                Gloire.plugin.getConfig().getString("database.username"),
                                Gloire.plugin.getConfig().getString("database.password"));
                        ArrayList<String> result = bdd.query("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + entity.getKiller().getUniqueId() + "'", true);
                        int nouveauGloire = Integer.parseInt(result.get(0)) + Integer.parseInt(Gloire.plugin.getConfig().getString("config.gloire_" + event.getEntity().toString().toLowerCase().substring(5)));
                        bdd.query("UPDATE `statistiques` SET `gloire`='" + nouveauGloire + "' WHERE `uuid` = '" + entity.getKiller().getUniqueId() + "'", false);
                        // On recharge le rang
                        Rank.loadRank(entity.getKiller());
                        entity.getKiller().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.gagner_points").replace("%s", Gloire.plugin.getConfig().getString("config.gloire_" + event.getEntity().toString().toLowerCase().substring(5)))));
                    } catch (SQLException throwables) {
                        Bukkit.getServer().getLogger().warning("Erreur MySQL : " + throwables.getMessage());
                        Bukkit.getServer().getLogger().warning ("Impossible de se connecter à la base de données, veuillez vérifier les informations dans le fichier config.yml");
                        Bukkit.getPluginManager().disablePlugin(Gloire.plugin);
                    }
                });
            }
        }
    }
}
