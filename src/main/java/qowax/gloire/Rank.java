package qowax.gloire;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public enum Rank {
    // PREFIXS
    NONE(Gloire.plugin.getConfig().getString("levels.aucun.prefix")),
    PREFIX_PODIUM(Gloire.plugin.getConfig().getString("levels.podium.prefix")),
    PREFIX_GRAND(Gloire.plugin.getConfig().getString("levels.grand.prefix")),
    PREFIX_IMPITOYABLE(Gloire.plugin.getConfig().getString("levels.impitoyable.prefix")),
    PREFIX_TRIOMPHANT(Gloire.plugin.getConfig().getString("levels.triomphant.prefix")),
    PREFIX_TEMERAIRE(Gloire.plugin.getConfig().getString("levels.temeraire.prefix"));

    private final String prefix;

    // Map des joueurs
    public static Map<Player, Rank> ranks = new HashMap<>();

    Rank(String prefix) {
        this.prefix = prefix;
    }

    // Avoir le rang
    public static Rank getRank(Player player) {
        return ranks.get(player);
    }

    // Avoir le préfix
    public static String getPrefix(Player player) {
        return ranks.get(player).prefix;
    }

    // Charger le rang (et ajoute le joueur dans la Hashmap)
    public static void loadRank(Player player) {
        // Vérifie de quel rang est le joueur

        // On demande le nombre de Gloire au serv MySQL
        Bukkit.getScheduler().runTaskAsynchronously(Gloire.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    Database bdd = new Database(
                            Gloire.plugin.getConfig().getString("database.host"),
                            Integer.parseInt(Gloire.plugin.getConfig().getString("database.port")),
                            Gloire.plugin.getConfig().getString("database.database"),
                            Gloire.plugin.getConfig().getString("database.username"),
                            Gloire.plugin.getConfig().getString("database.password"));
                    ArrayList<String> result = bdd.query("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + player.getUniqueId() + "'", true);

                    // Calcul du rang
                    int nombreGloire = Integer.valueOf(result.get(0));

                    Rank playerRank;

                    if (nombreGloire >= Gloire.LEVEL_PODIUM) {
                        // PODIUM
                        playerRank = Rank.PREFIX_PODIUM;
                    } else if (nombreGloire < Gloire.LEVEL_PODIUM && nombreGloire >= Gloire.LEVEL_GRAND) {
                        // GRAND
                        playerRank = Rank.PREFIX_GRAND;
                    } else if (nombreGloire < Gloire.LEVEL_GRAND && nombreGloire >= Gloire.LEVEL_IMPITOYABLE) {
                        // IMPITOYABLE
                        playerRank = Rank.PREFIX_IMPITOYABLE;
                    } else if (nombreGloire < Gloire.LEVEL_IMPITOYABLE && nombreGloire >= Gloire.LEVEL_TRIOMPHANT) {
                        // TRIOMPHANT
                        playerRank = Rank.PREFIX_TRIOMPHANT;
                    } else if (nombreGloire < Gloire.LEVEL_TRIOMPHANT && nombreGloire >= Gloire.LEVEL_TEMERAIRE) {
                        // TEMERAIRE
                        playerRank = Rank.PREFIX_TEMERAIRE;
                    } else {
                        playerRank = Rank.NONE;
                    }

                    ranks.put(player, playerRank);
                } catch (SQLException throwables) {
                    Bukkit.getServer().getLogger().warning("Erreur MySQL : " + throwables.getMessage());
                    Bukkit.getServer().getLogger().warning("Impossible de se connecter à la base de données, veuillez vérifier les informations dans le fichier config.yml");
                    Bukkit.getPluginManager().disablePlugin(Gloire.plugin);
                }
            }
        });
    }

    // Charger le préfix ## OFFLINE PLAYER ##
    public static String getOfflinePlayerPrefix(OfflinePlayer player) {

        Rank playerRank = null;

        try {
            Database bdd = new Database(
                    Gloire.plugin.getConfig().getString("database.host"),
                    Integer.parseInt(Gloire.plugin.getConfig().getString("database.port")),
                    Gloire.plugin.getConfig().getString("database.database"),
                    Gloire.plugin.getConfig().getString("database.username"),
                    Gloire.plugin.getConfig().getString("database.password"));
            ArrayList<String> result = bdd.query("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + player.getUniqueId() + "'", true);

            // Calcul du rang
            int nombreGloire = Integer.valueOf(result.get(0));

            if (nombreGloire >= Gloire.LEVEL_PODIUM) {
                // PODIUM
                playerRank = Rank.PREFIX_PODIUM;
            } else if (nombreGloire < Gloire.LEVEL_PODIUM && nombreGloire >= Gloire.LEVEL_GRAND) {
                // GRAND
                playerRank = Rank.PREFIX_GRAND;
            } else if (nombreGloire < Gloire.LEVEL_GRAND && nombreGloire >= Gloire.LEVEL_IMPITOYABLE) {
                // IMPITOYABLE
                playerRank = Rank.PREFIX_IMPITOYABLE;
            } else if (nombreGloire < Gloire.LEVEL_IMPITOYABLE && nombreGloire >= Gloire.LEVEL_TRIOMPHANT) {
                // TRIOMPHANT
                playerRank = Rank.PREFIX_TRIOMPHANT;
            } else if (nombreGloire < Gloire.LEVEL_TRIOMPHANT && nombreGloire >= Gloire.LEVEL_TEMERAIRE) {
                // TEMERAIRE
                playerRank = Rank.PREFIX_TEMERAIRE;
            } else {
                playerRank = Rank.NONE;
            }
        } catch (SQLException throwables) {
            Bukkit.getServer().getLogger().warning("Erreur MySQL : " + throwables.getMessage());
            Bukkit.getServer().getLogger().warning("Impossible de se connecter à la base de données, veuillez vérifier les informations dans le fichier config.yml");
            Bukkit.getPluginManager().disablePlugin(Gloire.plugin);
        }

        return playerRank.prefix;
    }

    // Retire le joueur de la Hashmap
    public static void saveRank(Player player) {
        ranks.remove(player);
    }
}
