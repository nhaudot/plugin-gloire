package qowax.gloire;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class CommandGloire implements CommandExecutor {

    private Plugin plugin;
    public Player player;

    public CommandGloire(Plugin _plugin) {
        plugin = _plugin;
    }

    // Commande /gloire
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Commandes joueur
        if (sender instanceof Player) {
            player = (Player) sender;

            // /GLOIRE
            if (args.length == 0)
            {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        Database bdd = new Database(plugin.getConfig().getString("database.host"),
                                Integer.parseInt(plugin.getConfig().getString("database.port")),
                                plugin.getConfig().getString("database.database"),
                                plugin.getConfig().getString("database.username"),
                                plugin.getConfig().getString("database.password"));

                        ArrayList<String> result = bdd.query("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + player.getUniqueId() + "'", true);
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("config.gloire").replace("%s", result.get(0))));
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                });
            }
            // /GLOIRE TOP
            else if (args[0].equalsIgnoreCase("top")) {
                TopGUI monGui = new TopGUI(plugin);

                // Ouverture GUI
                monGui.openInventory((HumanEntity) sender);
            }
            // /GLOIRE ADD/REMOVE <joueur> <montant>
            else if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {

                if (args.length <= 2 || args.length >= 4) {
                    player.sendMessage("Erreur de syntaxe: /gloire <add|remove> <joueur> <montant>");
                }
                else
                {
                    if (args[2].matches("[0-9]+")) {
                        // Recherche joueur dans bdd
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            try {
                                Database bdd = new Database(plugin.getConfig().getString("database.host"),
                                        Integer.parseInt(plugin.getConfig().getString("database.port")),
                                        plugin.getConfig().getString("database.database"),
                                        plugin.getConfig().getString("database.username"),
                                        plugin.getConfig().getString("database.password"));
                                ArrayList<String> result = bdd.query("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + Bukkit.getOfflinePlayer(args[1]).getUniqueId() + "'", true);
                                if (result.size() != 0) {
                                    int nouveauGloire;
                                    if (args[0].equalsIgnoreCase("add")) {
                                        nouveauGloire = Integer.parseInt(result.get(0)) + Integer.parseInt(args[2]);
                                        bdd.query("UPDATE `statistiques` SET `gloire`='" + String.valueOf(nouveauGloire) + "' WHERE `uuid` = '" + Bukkit.getOfflinePlayer(args[1]).getUniqueId() + "'", false);
                                        // On recharge le rang
                                        Player concernedPlayer = Bukkit.getPlayerExact(args[1]);
                                        if(concernedPlayer != null)
                                        {
                                            Rank.loadRank(Bukkit.getPlayer(args[1]));
                                        }
                                        player.sendMessage(args[2] + " Gloire ont été ajoutés à " + args[1]);
                                    } else if (args[0].equalsIgnoreCase("remove")) {
                                        nouveauGloire = Integer.parseInt(result.get(0)) - Integer.parseInt(args[2]);
                                        bdd.query("UPDATE `statistiques` SET `gloire`='" + String.valueOf(nouveauGloire) + "' WHERE `uuid` = '" + Bukkit.getOfflinePlayer(args[1]).getUniqueId() + "'", false);
                                        // On recharge le rang
                                        Player concernedPlayer = Bukkit.getPlayerExact(args[1]);
                                        if(concernedPlayer != null)
                                        {
                                            Rank.loadRank(Bukkit.getPlayer(args[1]));
                                        }
                                        player.sendMessage(args[2] + " Gloire ont été enlevés à " + args[1]);
                                    }
                                } else {
                                    player.sendMessage("Impossible de trouver le joueur");
                                }
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                            }
                        });
                    } else {
                        player.sendMessage("Erreur de syntaxe: /gloire <add|remove> <joueur> <montant>");
                    }
                }
            }
            // /GLOIRE TIMER
            else if (args[0].equalsIgnoreCase("setday")) {

                if (args.length <= 1 || args.length >= 3) {
                    player.sendMessage("Erreur de syntaxe: /gloire setday <00-31>");
                }
                else
                {
                    // On set le jour du mois pour le timer
                    if (args[1].matches("0[1-9]|[12]\\d|3[01]")) {
                        Date today = Calendar.getInstance().getTime();
                        Calendar c = Calendar.getInstance();
                        c.setTime(today);
                        c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(args[1]));
                        c.set(Calendar.HOUR_OF_DAY, 0);
                        c.set(Calendar.HOUR_OF_DAY, 0);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.SECOND, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        Calendar cal = Calendar.getInstance();
                        int dayNumber = cal.get(Calendar.DAY_OF_MONTH);
                        if (dayNumber < Integer.parseInt(args[1])) {
                            c.add(Calendar.MONTH, -1);
                        }

                        Date newDate = c.getTime();
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            Database bdd = new Database(plugin.getConfig().getString("database.host"),
                                    Integer.parseInt(plugin.getConfig().getString("database.port")),
                                    plugin.getConfig().getString("database.database"),
                                    plugin.getConfig().getString("database.username"),
                                    plugin.getConfig().getString("database.password"));

                            bdd.query("UPDATE `config` SET `lastReset`='" + dateFormat.format(newDate) + "' WHERE 1", false);
                            c.add(Calendar.MONTH, 1);
                            newDate = c.getTime();
                            player.sendMessage("Prochain reset : " + dateFormat.format(newDate));
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                    else
                    {
                        player.sendMessage("Erreur de syntaxe: /gloire setday <01-31>");
                    }
                }
            }
            else if (args[0].equalsIgnoreCase("help")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e### AIDE PLUGIN GLOIRE ###"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/gloire&f: Affiche le nombre de Gloire du joueur"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/gloire top&f: Affiche le top joueurs du serveur"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/gloire <add|remove> <joueur> <montant>&f: Ajoute ou enlève des Gloire à un joueur"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/gloire setday <00-31>&f: Définit le jour du mois auquel le nombre de Gloire sera remis à zéro"));
            }
            else {
                player.sendMessage("Commande introuvable");
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Seul les joueurs peuvent utiliser cette commande ");
        }

        return true;
    }
}