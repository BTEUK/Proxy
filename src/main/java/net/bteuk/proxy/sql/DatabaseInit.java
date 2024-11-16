package net.bteuk.proxy.sql;

import net.bteuk.proxy.Proxy;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseInit {

    //Creates the mysql connection.
    public BasicDataSource mysqlSetup(String database) throws SQLException, ClassNotFoundException {

        Class.forName("com.mysql.cj.jdbc.Driver");

        String host = Proxy.getInstance().getConfig().getString("host");
        int port = Proxy.getInstance().getConfig().getInt("port");
        String username = Proxy.getInstance().getConfig().getString("username");
        String password = Proxy.getInstance().getConfig().getString("password");

        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?&allowPublicKeyRetrieval=true&useSSL=false&");
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        testDataSource(dataSource);
        return dataSource;

    }

    private void testDataSource(BasicDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        }
    }
}
