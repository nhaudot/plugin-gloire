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
import java.util.ArrayList;

public class CommandGloire implements CommandExecutor {

    private Database bdd;
    private Plugin plugin;
    public Player player;

    public CommandGloire(Database _bdd, Plugin _plugin) {
        bdd = _bdd;
        plugin = _plugin;
    }

    // Commande /gloire
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            player = (Player) sender;

            // /GLOIRE
            if (args.length == 0)
            {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ArrayList<String> result = bdd.sendRequest("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + player.getUniqueId() + "'");
                            sender.sendMessage("Vous avez " + result.get(0) + " gloires");
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                });
            }
            // /GLOIRE TOP
            else if (args[0].equalsIgnoreCase("top")) {
                TopGUI monGui = new TopGUI(bdd);

                // Ouverture GUI
                monGui.openInventory((HumanEntity) sender);
            }
            // /GLOIRE ADD <joueur< <montant>
            else if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {

                if (args.length <= 2 || args.length >= 4) {
                    player.sendMessage("Erreur de syntaxe: /gloire add <joueur> <montant>");
                }
                else
                {
                    if (args[2].matches("[0-9]+")) {
                        // Recherche joueur dans bdd
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ArrayList<String> result = bdd.sendRequest("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + Bukkit.getOfflinePlayer(args[1]).getUniqueId() + "'");
                                    if (result.size() != 0) {
                                        int nouveauGloire = 0;
                                        if (args[0].equalsIgnoreCase("add")) {
                                            nouveauGloire = Integer.parseInt(result.get(0)) + Integer.parseInt(args[2]);
                                            bdd.executeQuery("UPDATE `statistiques` SET `gloire`='" + String.valueOf(nouveauGloire) + "' WHERE `uuid` = '" + Bukkit.getOfflinePlayer(args[1]).getUniqueId() + "'");
                                            player.sendMessage(args[2] + " Gloire ont été ajoutés à " + args[1]);
                                        } else if (args[0].equalsIgnoreCase("remove")) {
                                            nouveauGloire = Integer.parseInt(result.get(0)) - Integer.parseInt(args[2]);
                                            bdd.executeQuery("UPDATE `statistiques` SET `gloire`='" + String.valueOf(nouveauGloire) + "' WHERE `uuid` = '" + Bukkit.getOfflinePlayer(args[1]).getUniqueId() + "'");
                                            player.sendMessage(args[2] + " Gloire ont été enlevés à " + args[1]);
                                        }
                                    } else {
                                        player.sendMessage("Impossible de trouver le joueur");
                                    }
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                }
                            }
                        });
                    } else {
                        player.sendMessage("Erreur de syntaxe: /gloire add <joueur> <montant>");
                    }
                }
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Seul les joueurs peuvent utiliser cette commande ");
        }

        return true;
    }
}