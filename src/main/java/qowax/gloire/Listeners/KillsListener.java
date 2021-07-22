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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class KillsListener implements Listener {

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
            // On vérifie si le tueur a tué un joueur ou non
            if (event.getEntity() instanceof Player) {
                // On ajoute +1 au tueur en kill et +1 au tué en mort (pour ratio morts/kills)
                bddFightTable.query("UPDATE `statistiques` SET `kills`=`kills` + 1 WHERE `uuid`='" + event.getEntity().getKiller().getUniqueId().toString() + "'", false);
                bddFightTable.query("UPDATE `statistiques` SET `morts`=`morts` + 1 WHERE `uuid`='" + event.getEntity().getUniqueId().toString() + "'", false);
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
                    // On vérifie que le tueur n'a pas tué le joueur dans les dernières 24h
                    ArrayList<String> dateLastKill = bddFightTable.query("SELECT `lastKillDate` FROM `killTable` WHERE `killer`='" + listeEnnemis.get(i) + "' AND `killedPlayer`= '" + entity.getUniqueId().toString() + "'", true);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    if (dateLastKill.size() > 0) {
                        // On ajoute 1 jour à la date côté BDD pour vérifier que ça fait plus de 24h qu'on a tué le joueur
                        Calendar c = Calendar.getInstance();
                        try {
                            c.setTime(dateFormat.parse(dateLastKill.get(0)));// all done
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        c.add(Calendar.DAY_OF_MONTH, 1);
                        // Ca fait plus de 24h
                        if (c.getTime().compareTo(Calendar.getInstance().getTime()) <= 0) {
                            // On attribue les points de Gloire
                            ArrayList<String> pointsGloireTueur = bddFightTable.query("SELECT `gloire` FROM `statistiques` WHERE `uuid`='" + listeEnnemis.get(i) + "'", true);
                            // Calcul de la part du tueur
                            double coefficient = Double.valueOf(ennemi.get(0)) / Double.valueOf(totalHeartValue.get(0));
                            // gloirePourJoueurRatio1 = gloireVictoire (1000 par défaut) + 10% gloireJoueurTue
                            double gloirePourJoueurRatio1 = Double.valueOf(Gloire.plugin.getConfig().getString("config.gloire_joueur")) + (0.1 * Double.valueOf(gloireJoueurTue.get(0)));
                            double pointsGloire = Double.valueOf(pointsGloireTueur.get(0)) + (gloirePourJoueurRatio1 * coefficient);
                            // On met à jour le nombre de Gloire du joueur
                            bddFightTable.query("UPDATE `statistiques` SET `gloire`='" + (int) pointsGloire + "' WHERE `uuid` = '" + listeEnnemis.get(i) + "'", false);
                            // On recharge le rang
                            String strPlayerUUID = listeEnnemis.get(i).replace("-", "");
                            UUID playerUUID = new UUID(new BigInteger(strPlayerUUID.substring(0, 16), 16).longValue(), new BigInteger(strPlayerUUID.substring(16), 16).longValue());
                            if (Bukkit.getServer().getPlayer(playerUUID) != null) {
                                // Joueur en ligne
                                Bukkit.getServer().getPlayer(playerUUID).sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.gloire_gagnant").replace("%s", String.valueOf((int) (gloirePourJoueurRatio1 * coefficient))).replace("%t", String.valueOf((int) (100 * coefficient)))));
                                Rank.loadRank(Bukkit.getServer().getPlayer(playerUUID));
                            }
                            // On update l'occurence du tueur dans la killTable en remplaçant la date de kill
                            bddFightTable.query("UPDATE `killTable` SET `lastKillDate`='" + dateFormat.format(Calendar.getInstance().getTime()) + "' WHERE `killer`='" + listeEnnemis.get(i) + "' AND `killedPlayer`='" + entity.getUniqueId().toString() + "'", false);
                        }
                        else {
                            // On n'attribue pas les points de Gloire
                            String strPlayerUUID = listeEnnemis.get(i).replace("-", "");
                            UUID playerUUID = new UUID(new BigInteger(strPlayerUUID.substring(0, 16), 16).longValue(), new BigInteger(strPlayerUUID.substring(16), 16).longValue());
                            if (Bukkit.getServer().getPlayer(playerUUID) != null) {
                                // Joueur en ligne
                                Bukkit.getServer().getPlayer(playerUUID).sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.gloire_24h")));
                                Rank.loadRank(Bukkit.getServer().getPlayer(playerUUID));
                            }
                        }
                    } else {
                        // On attribue les points de Gloire
                        ArrayList<String> pointsGloireTueur = bddFightTable.query("SELECT `gloire` FROM `statistiques` WHERE `uuid`='" + listeEnnemis.get(i) + "'", true);
                        // Calcul de la part du tueur
                        double coefficient = Double.valueOf(ennemi.get(0)) / Double.valueOf(totalHeartValue.get(0));
                        // gloirePourJoueurRatio1 = gloireVictoire (1000 par défaut) + 10% gloireJoueurTue
                        double gloirePourJoueurRatio1 = Double.valueOf(Gloire.plugin.getConfig().getString("config.gloire_joueur")) + (0.1 * Double.valueOf(gloireJoueurTue.get(0)));
                        double pointsGloire = Double.valueOf(pointsGloireTueur.get(0)) + (gloirePourJoueurRatio1 * coefficient);
                        // On met à jour le nombre de Gloire du joueur
                        bddFightTable.query("UPDATE `statistiques` SET `gloire`='" + (int) pointsGloire + "' WHERE `uuid` = '" + listeEnnemis.get(i) + "'", false);
                        // On recharge le rang
                        String strPlayerUUID = listeEnnemis.get(i).replace("-", "");
                        UUID playerUUID = new UUID(new BigInteger(strPlayerUUID.substring(0, 16), 16).longValue(), new BigInteger(strPlayerUUID.substring(16), 16).longValue());
                        if (Bukkit.getServer().getPlayer(playerUUID) != null) {
                            // Joueur en ligne
                            Bukkit.getServer().getPlayer(playerUUID).sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.gloire_gagnant").replace("%s", String.valueOf((int) (gloirePourJoueurRatio1 * coefficient))).replace("%t", String.valueOf((int) (100 * coefficient)))));
                            Rank.loadRank(Bukkit.getServer().getPlayer(playerUUID));
                        }
                        // On crée l'occurence du tueur dans la killTable en remplaçant la date de kill
                        bddFightTable.query("INSERT INTO `killTable`(`killer`, `killedPlayer`, `lastKillDate`) VALUES ('" + listeEnnemis.get(i) + "','" + entity.getUniqueId().toString() + "','" + dateFormat.format(Calendar.getInstance().getTime()) + "')", false);
                    }
                }
                // On supprime les occurences de la table concernant le joueur tué
                bddFightTable.query("DELETE FROM `fightTable` WHERE `damagedPlayer`='" + event.getEntity().getUniqueId() + "'", false);
                bddFightTable.query("DELETE FROM `fightTable` WHERE `ennemy`='" + event.getEntity().getUniqueId() + "'", false);
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
            // ### MOBS
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
