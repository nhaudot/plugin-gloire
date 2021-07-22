package qowax.gloire;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TopGUI implements Listener {

    private Plugin plugin;
    private final Inventory inv;

    public TopGUI(Plugin _plugin, Player _player) {
        plugin = _plugin;

        // Crée l'inventaire
        inv = Bukkit.createInventory(null, 45, "TOP - Gloire");

        // Ajoute les têtes du top dans le GUI
        try {
            initializeItems(_player);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    // Initialise les items di GUI
    public void initializeItems(Player _player) throws SQLException {
        Database bdd = new Database(plugin.getConfig().getString("database.host"),
                Integer.parseInt(plugin.getConfig().getString("database.port")),
                plugin.getConfig().getString("database.database"),
                plugin.getConfig().getString("database.username"),
                plugin.getConfig().getString("database.password"));
        ArrayList<String> result = bdd.query("SELECT `uuid`, `gloire` FROM `statistiques` ORDER BY `gloire` DESC LIMIT 25;", true);

        // Ajout item boussole compte à rebours
        ItemStack timer = new ItemStack(Material.COMPASS, 1);
        timer.addUnsafeEnchantment(Enchantment.LURE, 1);
        final ItemMeta itemMeta = timer.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.setDisplayName("Compte à rebours");
        timer.setItemMeta(itemMeta);
        inv.setItem(8,timer);

        // Ajout item livre gagner de la Gloire
        ItemStack book = new ItemStack(Material.BOOK_AND_QUILL, 1);
        book.addUnsafeEnchantment(Enchantment.LURE, 1);
        final ItemMeta itemMeta2 = book.getItemMeta();
        itemMeta2.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta2.setDisplayName("Gagner de la Gloire");
        book.setItemMeta(itemMeta2);
        inv.setItem(0,book);

        // Fetch base de données & liste joueurs
        inv.setItem(1, null);

        for (int i = 0; i < result.size() - 1; i+=2)
        {
            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            String s = result.get(i).replace("-", "");
            UUID playerUUID = new UUID(new BigInteger(s.substring(0, 16), 16).longValue(), new BigInteger(s.substring(16), 16).longValue());
            meta.setOwner(Bukkit.getOfflinePlayer(playerUUID).getName());
            meta.setDisplayName(Bukkit.getOfflinePlayer(playerUUID).getName());
            List<String> description = new ArrayList<>();

            // On recrée le rang du joueur
            description.add(0, ChatColor.translateAlternateColorCodes('&', Rank.getOfflinePlayerPrefix(Bukkit.getOfflinePlayer(playerUUID))));
            description.add(1, ChatColor.GREEN + result.get(i+1));

            // Ratio morts/kills si admin
            if(_player.hasPermission("gloire.ratio")) {
                ArrayList<String> ratioJoueur = bdd.query("SELECT `kills`, `morts` FROM `statistiques` WHERE `uuid` = '" + playerUUID.toString() + "'", true);
                Double ratio = Double.valueOf(ratioJoueur.get(0)) / Double.valueOf(ratioJoueur.get(1));
                description.add(2, ChatColor.RED + "Ratio K/M: " + String.format("%.2f", ratio));
            }

            meta.setLore(description);
            skull.setItemMeta(meta);

            // Ajoute le joueur au top
            int j = 4;
            switch (i) {
                case 0:
                    inv.setItem(4,skull);
                    break;
                case 2:
                    inv.setItem(12,skull);
                    break;
                case 4:
                    inv.setItem(13,skull);
                    break;
                case 6:
                    inv.setItem(14,skull);
                    break;
                case 8:
                    inv.setItem(20,skull);
                    break;
                case 10:
                    inv.setItem(21,skull);
                    break;
                case 12:
                    inv.setItem(22,skull);
                    break;
                case 14:
                    inv.setItem(23,skull);
                    break;
                case 16:
                    inv.setItem(24,skull);
                    break;
                case 18:
                    inv.setItem(28,skull);
                    break;
                case 20:
                    inv.setItem(29,skull);
                    break;
                case 22:
                    inv.setItem(30,skull);
                    break;
                case 24:
                    inv.setItem(31,skull);
                    break;
                case 26:
                    inv.setItem(32,skull);
                    break;
                case 28:
                    inv.setItem(33,skull);
                    break;
                case 30:
                    inv.setItem(34,skull);
                    break;
                case 32:
                    inv.setItem(36,skull);
                    break;
                case 34:
                    inv.setItem(37,skull);
                    break;
                case 36:
                    inv.setItem(38,skull);
                    break;
                case 38:
                    inv.setItem(39,skull);
                    break;
                case 40:
                    inv.setItem(40,skull);
                    break;
                case 42:
                    inv.setItem(41,skull);
                    break;
                case 44:
                    inv.setItem(42,skull);
                    break;
                case 46:
                    inv.setItem(43,skull);
                    break;
                case 48:
                    inv.setItem(44,skull);
                    break;
                }
        }
    }

    // Ouvrir le GUI
    public void openInventory(final HumanEntity ent) {
        ent.openInventory(inv);
    }
}
