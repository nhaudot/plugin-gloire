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
    public boolean status; // Status de la connexion
    private DataSource dataSrc;

    public Database(String _host, int _port, String _ddb, String _username, String _password) {
        host = _host; port = _port; ddb = _ddb; username = _username; password = _password;

        dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setServerName(host);
        dataSource.setPortNumber(port);
        dataSource.setDatabaseName(ddb);
        dataSource.setUser(username);
        dataSource.setPassword(password);
    }

    // Initialise la connexion à la base de données
    public void connect() throws SQLException
    {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("[Gloire] Connexion MySQL non-réussie, veuillez vérifier la configuration");
            }
        }
        dataSrc = dataSource;
    }

    public ArrayList sendRequest(String _request) throws SQLException
    {
        ResultSet rs = null;
        ArrayList<String> sqlArray = null;

        try (Connection conn = dataSrc.getConnection(); PreparedStatement stmt = conn.prepareStatement(_request)) {
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
    }

    public void executeQuery(String _query) throws SQLException {
        try (Connection conn = dataSrc.getConnection(); PreparedStatement stmt = conn.prepareStatement(_query)) {
            int rs = stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

