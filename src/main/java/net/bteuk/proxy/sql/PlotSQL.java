package net.bteuk.proxy.sql;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlotSQL extends AbstractSQL {

    public PlotSQL(BasicDataSource datasource) {
        super(datasource);
    }

    public Double getReviewerReputation(String uuid) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "SELECT reputation FROM reviewers WHERE uuid=?;"
                )
        ) {
            statement.setString(1, uuid);
            ResultSet results = statement.executeQuery();

            if (results.next()) {
                return results.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
