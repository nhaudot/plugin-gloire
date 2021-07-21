package qowax.gloire;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;

public class Database {

    public String host;
    public int port;
    public String ddb;
    public String username;
    public String password;

    MysqlDataSource dataSource;
    private DataSource dataSrc;

    // Essai connexion
    public Database(String _host, int _port, String _ddb, String _username, String _password) throws SQLException {
        host = _host; port = _port; ddb = _ddb; username = _username; password = _password;

        dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setServerName(host);
        dataSource.setPortNumber(port);
        dataSource.setDatabaseName(ddb);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("[Gloire] Connexion MySQL non-réussie, veuillez vérifier la configuration");
            }
        }

        dataSrc = dataSource;
    }

    // Requête avec/sans réponse (boolean _data)
    public ArrayList query(String _query, boolean _data) {
        // Si l'on exige un retour de données
        if (_data == true) {
            ResultSet rs;
            ArrayList<String> sqlArray = null;

            try (Connection conn = dataSrc.getConnection(); PreparedStatement stmt = conn.prepareStatement(_query)) {
                rs = stmt.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                sqlArray = new ArrayList<>(columnCount);

                while (rs.next()) {
                    int i = 1;
                    while (i <= columnCount) {
                        sqlArray.add(rs.getString(i++));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return sqlArray;
        // Si l'on exige pas de retour de données
        } else {
            try (Connection conn = dataSrc.getConnection(); PreparedStatement stmt = conn.prepareStatement(_query)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            ArrayList<String> empty = null;

            return empty;
        }
    }
}
