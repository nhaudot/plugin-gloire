package qowax.gloire.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import qowax.gloire.Database;
import qowax.gloire.Gloire;
import qowax.gloire.Rank;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class EventListener implements Listener {

    // Empêche le joueur de prendre les items du GUI "TOP - Gloire"
    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (e.getWhoClicked() instanceof Player){
            Player p = (Player) e.getWhoClicked();
            ItemStack is = e.getCurrentItem();
            if (is == null ||is.getType() == Material.AIR || is.getType() == null)
                return;
            if(p.getOpenInventory().getTitle().equalsIgnoreCase("TOP - Gloire")) {
                // Si le joueur appuie sur le livre ou la boussole
                if (is.getType() == Material.COMPASS) {
                    // On calcule le temps restant avant le reset
                    Database bdd;

                    try {
                        bdd = new Database(Gloire.plugin.getConfig().getString("database.host"),
                                Integer.parseInt(Gloire.plugin.getConfig().getString("database.port")),
                                Gloire.plugin.getConfig().getString("database.database"),
                                Gloire.plugin.getConfig().getString("database.username"),
                                Gloire.plugin.getConfig().getString("database.password"));
                        ArrayList<String> result = bdd.query("SELECT `lastReset` FROM `config` WHERE 1", true);

                        Calendar c = Calendar.getInstance();
                        c.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(result.get(0)));
                        c.add(Calendar.MONTH, 1);
                        long estDateInLong = c.getTime().getTime();
                        long currentTimeinLong = Calendar.getInstance().getTimeInMillis();
                        long diff = (estDateInLong - currentTimeinLong);
                        long diffDay = diff / (24 * 60 * 60 * 1000);
                        diff = diff - (diffDay * 24 * 60 * 60 * 1000);
                        long diffHours = diff / (60 * 60 * 1000);
                        diff = diff - (diffHours * 60 * 60 * 1000);
                        long diffMinutes = diff / (60 * 1000);
                        diff = diff - (diffMinutes * 60 * 1000);
                        long diffSeconds = diff / 1000;

                        // Envoie le temps restant au joueur
                        e.getWhoClicked().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.timer")) + diffDay + " jours, " + String.format("%02d", diffHours) + "h" + String.format("%02d", diffMinutes) + "m" + String.format("%02d", diffSeconds) + "s");
                    } catch (SQLException | ParseException throwables) {
                        throwables.printStackTrace();
                    }
                    p.closeInventory();
                    e.setCancelled(true);
                } else if (is.getType() == Material.BOOK_AND_QUILL) {
                    e.getWhoClicked().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.gagner_gloires")));
                    p.closeInventory();
                    e.setCancelled(true);
                } else  {
                    if(e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                        e.setCancelled(true);
                        return;
                    }
                    e.setCancelled(true);
                }
            }
        }
    }

    // Quand un joueur rejoint le serveur
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(Gloire.plugin, () -> {
            try {
                Database bdd = new Database(
                        Gloire.plugin.getConfig().getString("database.host"),
                        Integer.parseInt(Gloire.plugin.getConfig().getString("database.port")),
                        Gloire.plugin.getConfig().getString("database.database"),
                        Gloire.plugin.getConfig().getString("database.username"),
                        Gloire.plugin.getConfig().getString("database.password"));
                ArrayList<String> result = bdd.query("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + event.getPlayer().getUniqueId() + "'", true);

                // Si joueur pas présent dans la BDD
                if (result.size() == 0) {
                    bdd.query("INSERT INTO `statistiques`(`uuid`, `joueur`, `gloire`, `kills`, `morts`) VALUES ('" + event.getPlayer().getUniqueId().toString() + "','" + event.getPlayer().getName() + "','" + Gloire.plugin.getConfig().getString("config.gloire_base") + "', '0', '0')", false);
                }

                // On charge le rang du joueur
                Rank.loadRank(player);

                // On vérifie qu'il n'a pas des bonus de fin de saison
                ArrayList<String> resultBonus = bdd.query("SELECT `uuid`, `poids` FROM `offlineBonusTable` WHERE 1", true);

                // On parcourt la liste
                for (int i = 0; i < resultBonus.size(); i+=2) {
                    // Le joueur a des bonus à recevoir
                    if (resultBonus.get(i).equalsIgnoreCase(player.getUniqueId().toString())) {
                        Gloire.sendBonus(player.getUniqueId(), Integer.valueOf(resultBonus.get(i+1)));
                        // On supprime l'occurence dans la BDD
                        bdd.query("DELETE FROM `offlineBonusTable` WHERE `uuid` = '" + player.getUniqueId().toString() + "'", false);
                    }
                }
            } catch (SQLException throwables) {
                Bukkit.getServer().getLogger().warning("Erreur MySQL : " + throwables.getMessage());
                Bukkit.getServer().getLogger().warning ("Impossible de se connecter à la base de données, veuillez vérifier les informations dans le fichier config.yml");
                Bukkit.getPluginManager().disablePlugin(Gloire.plugin);
            }
        });
    }

    // Quand un joueur parle dans le chat
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        e.setFormat(e.getFormat().replace("{gloire_rang}", Rank.getPrefix(e.getPlayer())));
    }

    // Quand un joueur quitte le serveur
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Rank.saveRank(event.getPlayer());
    }
}
