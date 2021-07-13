package qowax.gloire;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class CommandTop implements CommandExecutor {

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Vérification joueur/console
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seul les joueurs peuvent utiliser cette commande");
            return true;
        }



        Server s = Bukkit.getServer();

        s.getLogger().info("Connexion SQL...");


        MysqlDataSource dataSource = new MysqlConnectionPoolDataSource();

        //Database database = config.getDatabase();
        dataSource.setServerName("127.0.0.1");
        dataSource.setPortNumber(3306);
        dataSource.setDatabaseName("basemc");
        dataSource.setUser("minecraft");
        dataSource.setPassword("minecraft");




        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT *  from database")) {

            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }

            ResultSet rs = stmt.executeQuery();
            // do something with the ResultSet
        } catch (SQLException e) {
            e.printStackTrace(); // This should be replaced with a propper logging solution. don't do this.
        }


        return true;
    }

}
