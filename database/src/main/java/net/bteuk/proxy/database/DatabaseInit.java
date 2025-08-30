package net.bteuk.proxy.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.java.Log;
import net.bteuk.proxy.database.sql.GlobalSQL;
import net.bteuk.proxy.database.sql.PlotSQL;
import net.bteuk.proxy.database.sql.RegionSQL;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

@Log
public class DatabaseInit {

    // Creates the mysql connection.
    public DataSource mysqlSetup(String database, String host, int port, String username, String password) throws SQLException {

        HikariConfig cfg = new HikariConfig();

        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?rewriteBatchedStatements=true" + "&allowPublicKeyRetrieval=true" + "&useSSL=false" + "&cachePrepStmts=true" + "&prepStmtCacheSize=256" + "&prepStmtCacheSqlLimit=2048" + "&connectTimeout=10000" + "&socketTimeout=30000" + "&connectionTimeZone=UTC");

        cfg.setUsername(username);
        cfg.setPassword(password);

        cfg.setMaximumPoolSize(20);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(15000);
        cfg.setIdleTimeout(300000);
        cfg.setMaxLifetime(1800000);
        cfg.setKeepaliveTime(300000);
        cfg.setPoolName("ProxyHikariPool");

        Properties dsProps = new Properties();
        dsProps.setProperty("useUnicode", "true");
        dsProps.setProperty("characterEncoding", "utf8");
        cfg.setDataSourceProperties(dsProps);

        return new HikariDataSource(cfg);
    }

    public void initializeSchemas(DataSource globalDatasource, DataSource plotDatasource, DataSource regionDatasource) {
        // Setup MySQL
        initDb("/dbsetup_global.sql", globalDatasource);
        initDb("/dbsetup_plots.sql", plotDatasource);
        initDb("/dbsetup_regions.sql", regionDatasource);

        GlobalSQL globalSQL = new GlobalSQL(globalDatasource);
        PlotSQL plotSQL = new PlotSQL(plotDatasource);
        RegionSQL regionSQL = new RegionSQL(regionDatasource);

        DatabaseUpdates databaseUpdates = new DatabaseUpdates(globalSQL, plotSQL, regionSQL);
        databaseUpdates.updateDatabase();
    }

    // Set up the tables for the database.
    private void initDb(String fileName, DataSource dataSource) {
        // Read the database setup files.
        // This file contains statements to create our initial tables.
        // It is located in the resources.
        String setup;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(fileName))))) {
            setup = in.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.severe("Could not read db setup file: " + fileName);
            return;
        }
        // Mariadb can only handle a single query per statement. We need to split at ';'.
        String[] queries = setup.split(";");
        // execute each query to the database.
        for (String query : queries) {
            if (query.trim().isEmpty()) continue;
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.execute();
            } catch (SQLException sqlException) {
                log.severe("Error in sql syntax: " + sqlException.getLocalizedMessage());
            }
        }
        log.info("Database setup complete for: " + fileName);
    }
}
