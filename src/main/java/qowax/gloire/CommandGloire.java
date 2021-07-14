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
            else if (args[0].equalsIgnoreCase("top")) {
                TopGUI monGui = new TopGUI(bdd);

                // Ouverture GUI
                monGui.openInventory((HumanEntity) sender);
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Seul les joueurs peuvent utiliser cette commande ");
        }

        return true;
    }
}
