package net.bteuk.proxy.sql;

import net.bteuk.network.lib.enums.PlotDifficulties;
import net.bteuk.network.lib.utils.Reviewing;
import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.sql.migration.AcceptData;
import net.bteuk.proxy.sql.migration.DenyData;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PlotSQL extends AbstractSQL {

    public PlotSQL(BasicDataSource datasource) {
        super(datasource);
    }

    public int insertReturnId(String sql) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {

            statement.executeUpdate();
            ResultSet result = statement.getGeneratedKeys();
            if (result.next()) {
                return result.getInt(1);
            }

        } catch (SQLException e) {
            Proxy.getInstance().getLogger().error("An error occurred while inserting a row in the database", e);
        }

        return 0;
    }

    public double getReviewerReputation(String uuid) {
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
        return 0;
    }

    public List<Integer> getReviewablePlots(String uuid, boolean isArchitect, boolean isReviewer) {
        addReviewerIfNotExists(uuid, isArchitect, isReviewer); // Add an entry for relevant users.
        List<PlotDifficulties> difficulties = Reviewing.getAvailablePlotDifficulties(isArchitect, isReviewer, getReviewerReputation(uuid));

        List<Integer> submitted_plots = new ArrayList<>();

        for (PlotDifficulties difficulty : difficulties) {
            submitted_plots.addAll(getIntList("SELECT pd.id FROM plot_data AS pd INNER JOIN plot_submission AS ps ON pd.id=ps.id WHERE pd.status='submitted' AND pd.difficulty=" + difficulty.getValue() + ";"));
        }

        // Get all plots that the user is the owner or a member of, don't use those in the count.
        List<Integer> member_plots = getIntList("SELECT id FROM plot_members WHERE uuid='" + uuid + "';");

        submitted_plots.removeAll(member_plots);

        return submitted_plots;
    }

    public int getReviewablePlotCount(String uuid, boolean isArchitect, boolean isReviewer) {
        return getReviewablePlots(uuid, isArchitect, isReviewer).size();
    }

    public List<AcceptData> getAcceptData() {
        List<AcceptData> acceptData = new ArrayList<>();

        try (Connection conn = conn();
             PreparedStatement statement = conn.prepareStatement("SELECT * FROM accept_data;");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                acceptData.add(
                        new AcceptData(
                                results.getInt("id"), results.getString("uuid"),
                                results.getString("reviewer"), results.getInt("book_id"),
                                results.getInt("accuracy"), results.getInt("quality"),
                                results.getLong("accept_time")
                        )
                );
            }
        } catch (SQLException e) {
            Proxy.getInstance().getLogger().error("An error occurred while fetching accept_data", e);
        }
        return acceptData;
    }

    public List<DenyData> getDenyData() {
        List<DenyData> denyData = new ArrayList<>();

        try (Connection conn = conn();
             PreparedStatement statement = conn.prepareStatement("SELECT * FROM deny_data;");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                denyData.add(
                        new DenyData(
                                results.getInt("id"), results.getString("uuid"),
                                results.getString("reviewer"), results.getInt("book_id"),
                                results.getInt("attempt"), results.getLong("accept_time")
                        )
                );
            }
        } catch (SQLException e) {
            Proxy.getInstance().getLogger().error("An error occurred while fetching deny_data", e);
        }
        return denyData;
    }

    public void addReviewerIfNotExists(String uuid, boolean isArchitect, boolean isReviewer) {
        if ((!isArchitect && !isReviewer) || hasRow("SELECT uuid FROM reviewers FROM uuid'" + uuid + "';")) {
            return;
        }

        double initialValue = isReviewer ? 5 : 0;
        update("INSERT INTO reviewers(uuid,reputation) VALUES('" + uuid + "'," + initialValue + ");");
    }
}
