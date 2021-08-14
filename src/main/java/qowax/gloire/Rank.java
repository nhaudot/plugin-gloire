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
    // Podium
    PREFIX_TRANSCENDANT(Gloire.plugin.getConfig().getString("levels.transcendant.prefix"), 90),
    PREFIX_DIVIN(Gloire.plugin.getConfig().getString("levels.divin.prefix"), 80),
    PREFIX_LEGENDAIRE(Gloire.plugin.getConfig().getString("levels.legendaire.prefix"), 70),
    PREFIX_OVERLORD(Gloire.plugin.getConfig().getString("levels.overlord.prefix"), 60),
    // Standard
    PREFIX_GRAND(Gloire.plugin.getConfig().getString("levels.grand.prefix"), 50),
    PREFIX_IMPITOYABLE(Gloire.plugin.getConfig().getString("levels.impitoyable.prefix"), 40),
    PREFIX_TRIOMPHANT(Gloire.plugin.getConfig().getString("levels.triomphant.prefix"), 30),
    PREFIX_TEMERAIRE(Gloire.plugin.getConfig().getString("levels.temeraire.prefix"), 20),
    NONE(Gloire.plugin.getConfig().getString("levels.aucun.prefix"), 10);

    private final String prefix;
    public final int poids;

    // Map des joueurs
    public static Map<Player, Rank> ranks = new HashMap<>();

    Rank(String _prefix, int _poids) {
        this.prefix = _prefix;
        this.poids = _poids;
    }

    // Avoir le préfix
    public static String getPrefix(Player player) {
        return ranks.get(player).prefix;
    }

    // Avoir le poids
    public static int getPoids(Player player) {
        return ranks.get(player).poids;
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

                    Rank playerRank = null;

                    if (nombreGloire >= Gloire.LEVEL_PODIUM) {
                        // PODIUM
                        ArrayList<String> resultPodium = bdd.query("SELECT `uuid`, `gloire` FROM `statistiques` WHERE `gloire` >= 100000 ORDER BY `gloire` DESC", true);
                        for(int i = 0 ; i < resultPodium.size(); i+=2) {
                            if (i == 0 && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                                playerRank = Rank.PREFIX_TRANSCENDANT;
                                break;
                            } else if ((i == 2 || i == 4 ) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                                playerRank = Rank.PREFIX_DIVIN;
                                break;
                            } else if ((i == 6 || i == 8 || i == 10 || i == 12 || i == 14) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                                playerRank = Rank.PREFIX_LEGENDAIRE;
                                break;
                            } else if ((i == 16 || i == 18 || i == 20 || i == 22 || i == 24 || i == 26 || i == 28) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                                playerRank = Rank.PREFIX_OVERLORD;
                                break;
                            } else {
                                playerRank = Rank.PREFIX_GRAND;
                            }
                        }
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
            ArrayList<String> result = bdd.query("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + player.getUniqueId().toString() + "'", true);

            // Calcul du rang
            int nombreGloire = Integer.valueOf(result.get(0));

            if (nombreGloire >= Gloire.LEVEL_PODIUM) {
                // PODIUM
                ArrayList<String> resultPodium = bdd.query("SELECT `uuid`, `gloire` FROM `statistiques` WHERE `gloire` >= 100000 ORDER BY `gloire` DESC", true);
                for(int i = 0 ; i < resultPodium.size(); i+=2) {
                    if (i == 0 && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerRank = Rank.PREFIX_TRANSCENDANT;
                        break;
                    } else if ((i == 2 || i == 4 ) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerRank = Rank.PREFIX_DIVIN;
                        break;
                    } else if ((i == 6 || i == 8 || i == 10 || i == 12 || i == 14) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerRank = Rank.PREFIX_LEGENDAIRE;
                        break;
                    } else if ((i == 16 || i == 18 || i == 20 || i == 22 || i == 24 || i == 26 || i == 28) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerRank = Rank.PREFIX_OVERLORD;
                        break;
                    } else {
                        playerRank = Rank.PREFIX_GRAND;
                    }
                }
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

    // Charger le poids ## OFFLINE PLAYER ##
    public static int getOfflinePlayerPoids(OfflinePlayer player) {

        int playerPoids = 0;

        try {
            Database bdd = new Database(
                    Gloire.plugin.getConfig().getString("database.host"),
                    Integer.parseInt(Gloire.plugin.getConfig().getString("database.port")),
                    Gloire.plugin.getConfig().getString("database.database"),
                    Gloire.plugin.getConfig().getString("database.username"),
                    Gloire.plugin.getConfig().getString("database.password"));

            Bukkit.getServer().getLogger().info(player.getUniqueId().toString());
            ArrayList<String> result = bdd.query("SELECT `gloire` FROM `statistiques` WHERE `uuid` = '" + player.getUniqueId().toString() + "'", true);

            // Calcul du rang
            int nombreGloire = Integer.valueOf(result.get(0));

            if (nombreGloire >= Gloire.LEVEL_PODIUM) {
                // PODIUM
                ArrayList<String> resultPodium = bdd.query("SELECT `uuid`, `gloire` FROM `statistiques` WHERE `gloire` >= 100000 ORDER BY `gloire` DESC", true);
                for(int i = 0 ; i < resultPodium.size(); i+=2) {
                    if (i == 0 && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerPoids = Rank.PREFIX_TRANSCENDANT.poids;
                        break;
                    } else if ((i == 2 || i == 4 ) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerPoids = Rank.PREFIX_DIVIN.poids;
                        break;
                    } else if ((i == 6 || i == 8 || i == 10 || i == 12 || i == 14) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerPoids = Rank.PREFIX_LEGENDAIRE.poids;
                        break;
                    } else if ((i == 16 || i == 18 || i == 20 || i == 22 || i == 24 || i == 26 || i == 28) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerPoids = Rank.PREFIX_OVERLORD.poids;
                        break;
                    } else {
                        playerPoids = Rank.PREFIX_GRAND.poids;
                    }
                }
            } else if (nombreGloire < Gloire.LEVEL_PODIUM && nombreGloire >= Gloire.LEVEL_GRAND) {
                // GRAND
                playerPoids = Rank.PREFIX_GRAND.poids;
            } else if (nombreGloire < Gloire.LEVEL_GRAND && nombreGloire >= Gloire.LEVEL_IMPITOYABLE) {
                // IMPITOYABLE
                playerPoids = Rank.PREFIX_IMPITOYABLE.poids;
            } else if (nombreGloire < Gloire.LEVEL_IMPITOYABLE && nombreGloire >= Gloire.LEVEL_TRIOMPHANT) {
                // TRIOMPHANT
                playerPoids = Rank.PREFIX_TRIOMPHANT.poids;
            } else if (nombreGloire < Gloire.LEVEL_TRIOMPHANT && nombreGloire >= Gloire.LEVEL_TEMERAIRE) {
                // TEMERAIRE
                playerPoids = Rank.PREFIX_TEMERAIRE.poids;
            } else {
                playerPoids = Rank.NONE.poids;
            }
        } catch (SQLException throwables) {
            Bukkit.getServer().getLogger().warning("Erreur MySQL : " + throwables.getMessage());
            Bukkit.getServer().getLogger().warning("Impossible de se connecter à la base de données, veuillez vérifier les informations dans le fichier config.yml");
            Bukkit.getPluginManager().disablePlugin(Gloire.plugin);
        }

        return playerPoids;
    }

    // Charger le rang ## OFFLINE PLAYER ##
    public static Rank getOfflinePlayerRank(OfflinePlayer player) {

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
                ArrayList<String> resultPodium = bdd.query("SELECT `uuid`, `gloire` FROM `statistiques` WHERE `gloire` >= 100000 ORDER BY `gloire` DESC", true);
                for(int i = 0 ; i < resultPodium.size(); i+=2) {
                    if (i == 0 && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerRank = Rank.PREFIX_TRANSCENDANT;
                        break;
                    } else if ((i == 2 || i == 4 ) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerRank = Rank.PREFIX_DIVIN;
                        break;
                    } else if ((i == 6 || i == 8 || i == 10 || i == 12 || i == 14) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerRank = Rank.PREFIX_LEGENDAIRE;
                        break;
                    } else if ((i == 16 || i == 18 || i == 20 || i == 22 || i == 24 || i == 26 || i == 28) && (player.getUniqueId().toString().equals(resultPodium.get(i)))) {
                        playerRank = Rank.PREFIX_OVERLORD;
                        break;
                    } else {
                        playerRank = Rank.PREFIX_GRAND;
                    }
                }
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

        return playerRank;
    }

    // Retire le joueur de la Hashmap
    public static void saveRank(Player player) {
        ranks.remove(player);
    }
}
