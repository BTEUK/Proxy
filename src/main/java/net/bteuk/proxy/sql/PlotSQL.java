package net.bteuk.proxy.sql;

import net.bteuk.network.lib.enums.PlotDifficulties;
import net.bteuk.network.lib.utils.Reviewing;
import net.bteuk.proxy.Proxy;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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

    public int getAvailablePlots(String uuid, boolean isArchitect, boolean isReviewer) {

        List<PlotDifficulties> difficulties = Reviewing.getAvailablePlotDifficulties(isArchitect, isReviewer, getReviewerReputation(uuid));
        int count = 0;

        for (PlotDifficulties difficulty : difficulties) {
            count += Proxy.getInstance().getPlotSQL().getInt("SELECT COUNT(id) FROM plot_data WHERE status='submitted' AND difficulty=" + difficulty.getValue() + ";");
        }

        return count;
    }
}
