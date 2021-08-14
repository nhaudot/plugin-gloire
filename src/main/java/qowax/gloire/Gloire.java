package qowax.gloire;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import qowax.gloire.Listeners.EventListener;
import qowax.gloire.Listeners.KillsListener;

import java.math.BigInteger;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Gloire extends JavaPlugin {

    public static final String BUILD_MAJ = "1";
    public static final String BUILD_MIN = "0";
    public static final String BUILD_NUMBER = "345";

    public static Plugin plugin;

    // NIVEAUX
    public static int LEVEL_PODIUM;
    public static int LEVEL_GRAND;
    public static int LEVEL_IMPITOYABLE;
    public static int LEVEL_TRIOMPHANT;
    public static int LEVEL_TEMERAIRE;

    @Override
    public void onEnable() {
        // Setup plugin
        saveDefaultConfig();
        plugin = this;
        getLogger().info("Chargement de Gloire...");
        getLogger().info("Version " + BUILD_MAJ + "." + BUILD_MIN + "." + BUILD_NUMBER);

        LEVEL_PODIUM = Integer.parseInt(getConfig().getString("levels.podium.value"));
        LEVEL_GRAND = Integer.parseInt(getConfig().getString("levels.grand.value"));
        LEVEL_IMPITOYABLE = Integer.parseInt(getConfig().getString("levels.impitoyable.value"));
        LEVEL_TRIOMPHANT = Integer.parseInt(getConfig().getString("levels.triomphant.value"));
        LEVEL_TEMERAIRE = Integer.parseInt(getConfig().getString("levels.temeraire.value"));

        // Connexion à la BDD
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Vérifie que la BDD est initialisée
                Database bdd = new Database(getConfig().getString("database.host"),
                        Integer.parseInt(getConfig().getString("database.port")),
                        getConfig().getString("database.database"),
                        getConfig().getString("database.username"),
                        getConfig().getString("database.password"));

                ArrayList<String> result = bdd.query("SHOW TABLES LIKE 'statistiques'", true);
                if (result.size() == 0) {
                    // On doit initialiser la base de données
                    getLogger().info("Création de la base de données...");
                    bdd.query("CREATE TABLE `statistiques` (`uuid` text NOT NULL, `joueur` text NOT NULL, `gloire` int(11) NOT NULL, `kills` int(11) NOT NULL, `morts` int(11) NOT NULL) DEFAULT CHARSET=utf8;", false);
                    bdd.query("CREATE TABLE `config` (`lastReset` datetime NOT NULL) DEFAULT CHARSET=utf8;", false);
                    bdd.query("INSERT INTO `config` (`lastReset`) VALUES ('2021-07-01 00:00:00');", false);
                    bdd.query("CREATE TABLE `fightTable` (`ennemy` text NOT NULL, `damagedPlayer` text NOT NULL, `hearts` decimal(4,1) NOT NULL) DEFAULT CHARSET=utf8;", false);
                    bdd.query("CREATE TABLE `killTable` (`killer` text NOT NULL, `killedPlayer` text NOT NULL, `lastKillDate` datetime NOT NULL) DEFAULT CHARSET=utf8;", false);
                    bdd.query("CREATE TABLE `offlineBonusTable` (`uuid` text NOT NULL, `gloire` int(11) NOT NULL) DEFAULT CHARSET=utf8;", false);
                    getLogger().info("Base de données ok!");
                }

                // Initialise la connexion pour MobsListener
                KillsListener.bddFightTable = new Database(getConfig().getString("database.host"),
                        Integer.parseInt(getConfig().getString("database.port")),
                        getConfig().getString("database.database"),
                        getConfig().getString("database.username"),
                        getConfig().getString("database.password"));
            } catch (SQLException throwables) {
                getLogger().warning("Erreur MySQL : " + throwables.getMessage());
                getLogger().warning ("Impossible de se connecter à la base de données, veuillez vérifier les informations dans le fichier config.yml");
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        });

        // Déclaration commandes
        this.getCommand("gloire").setExecutor(new CommandGloire(plugin));

        // Listeners
        getServer().getPluginManager().registerEvents(new EventListener(), plugin);
        getServer().getPluginManager().registerEvents(new KillsListener(), plugin);

        // Lancement Timer
        timerLaunch();
    }

    // Fonction check timer
    public static void timerLaunch() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Récupère la date du lastReset
            try {
                Database bdd = new Database(plugin.getConfig().getString("database.host"),
                        Integer.parseInt(plugin.getConfig().getString("database.port")),
                        plugin.getConfig().getString("database.database"),
                        plugin.getConfig().getString("database.username"),
                        plugin.getConfig().getString("database.password"));
                ArrayList<String> result = bdd.query("SELECT `lastReset` FROM `config` WHERE 1", true);

                // Date lastReset
                Date lastReset = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(result.get(0));

                // On y ajoute l'intevralle
                Calendar c = Calendar.getInstance();
                c.setTime(lastReset);
                c.add(Calendar.MONTH, 1);
                lastReset = c.getTime();

                // Date aujourd'hui
                Date today = Calendar.getInstance().getTime();
                // DateFormat
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                // Vérifie si on doit reset le chrono
                int resultDate = today.compareTo(lastReset);
                // C'est l'heure de reset
                if (resultDate >= 0) {
                    // On recrée la date
                    c.setTime(today);
                    c.set(Calendar.HOUR_OF_DAY, 0);
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    Date newDate = c.getTime();
                    // Update la time du reset
                    bdd.query("UPDATE `config` SET `lastReset`='" + dateFormat.format(newDate) + "' WHERE 1", false);

                    // Distribution des récompenses aux joueurs
                    ArrayList<String> playerDatas = bdd.query("SELECT `uuid`, `gloire` FROM `statistiques` WHERE 1", true);

                    for (int i = 0; i < playerDatas.size(); i+=2) {
                        // On récupère le rang du joueur
                        String s = playerDatas.get(i).replace("-", "");
                        UUID playerUUID = new UUID(new BigInteger(s.substring(0, 16), 16).longValue(), new BigInteger(s.substring(16), 16).longValue());
                        Rank rank = Rank.getOfflinePlayerRank(Bukkit.getOfflinePlayer(playerUUID));

                        // On vérifie si le joueur est connecté
                        if (Bukkit.getOfflinePlayer(playerUUID).isOnline() == true) {
                            // le joueur est en ligne
                            try {
                                Bukkit.getOfflinePlayer(playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.fin_saison_award").replace("%s", Rank.getOfflinePlayerPrefix(Bukkit.getOfflinePlayer(playerUUID)))));
                            } catch (NullPointerException e) {
                                // Ne rien faire
                            }
                            sendBonus(playerUUID, rank.poids);
                        } else {
                            // Le joueur n'est pas en ligne, on ajoute son UUID/rang à la BDD, et il recevra ses récompenses à la prochaine connexion
                            bdd.query("INSERT INTO `offlineBonusTable`(`uuid`, `poids`) VALUES ('" + playerUUID.toString() + "'," + rank.poids + ")", false);
                        }
                    }
                    // Reset des points de Gloire
                    bdd.query("UPDATE `statistiques` SET `gloire`=" + plugin.getConfig().getString("config.gloire_base") + " WHERE 1", false);
                    Bukkit.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("config.broadcast_reset")));

                }
            } catch (SQLException | ParseException throwables) {
                throwables.printStackTrace();
            }
        }, 0, Long.parseLong(plugin.getConfig().getString("timer.schedule_check")));
    }

    public static void sendBonus(UUID _playerUUID, int poids) {

        // ##########################################
        // ### LISTE DES RECOMPENSES SE SITUE ICI ###
        // ##########################################

        // Poids <= 10 (AUCUN RANG)
        if (poids >= 10) {
            // Rien
        }

        // Poids <= 20 (TÉMÉRAIRE)
        if (poids >= 20) {
            // Kit hebdomadaire
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kit Guerrier " + Bukkit.getOfflinePlayer(_playerUUID).getName());
        }

        // Poids <= 30 (TRIOMPHANT)
        if (poids >= 30) {
            // Glorium
            Material itemType = Material.matchMaterial("GLORE_LINGOT_GLORIUM");
            ItemStack itemStack = new ItemStack(itemType);
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().getInventory().addItem(itemStack);

            // Récompense journalière boostée
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_boost_journalier").replace("%s", Rank.getOfflinePlayerPrefix(Bukkit.getOfflinePlayer(_playerUUID)))));

            // Récompense familier
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_familier")));
        }

        // Poids <= 40 (IMPITOYABLE)
        if (poids >= 40) {
            // Récompense roulette
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_roulette")));

            // Récompense zone des boss
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_boss")));
        }

        // Poids <= 50 (GRAND)
        if (poids >= 50) {
            // Récompense seed
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_seed")));

            // Récompense quêtes
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_quetes_bonus")));
        }

        // Poids <= 60 (OVERLORD)
        if (poids >= 60) {
            // Récompense césium

            // Récompense 100% boutique
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_100_boutique")));

            // Récompense particules
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_particules")));

            // Récompense Discord
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_discord")));
        }

        // Poids <= 70 (LÉGENDAIRE)
        if (poids >= 70) {
            // Récompense bâton de sorcier
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_baton_sorcier")));

            // Récompense points boutique
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_points_boutique")));
        }

        // Poids <= 80 (DIVIN)
        if (poids >= 80) {
            // Récompense arthémite

            // Récompense livre Lightning
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_livre_lightning")));
        }

        // Poids <= 90 (TRANSCENDANT)
        if (poids >= 90) {
            // Récompense saison pass
            Bukkit.getOfflinePlayer(_playerUUID).getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', Gloire.plugin.getConfig().getString("config.recompense_saison_pass")));
        }
    }

    @Override
    public void onDisable() {

    }
}
