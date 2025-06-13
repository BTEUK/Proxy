package net.bteuk.proxy.sql;

import net.bteuk.proxy.utils.Time;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GlobalSQL extends AbstractSQL {
    public GlobalSQL(BasicDataSource datasource) {
        super(datasource);
    }

    public boolean createUser(String uuid, String name, String playerSkin) {

        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "INSERT INTO player_data(uuid,name,last_online,last_submit,player_skin) VALUES('" +
                                uuid + "','" +
                                name + "'," +
                                Time.currentTime() + "," +
                                0 + ",'" +
                                playerSkin + "');"
                )
        ) {

            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertMessage(String recipient, String message) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "INSERT INTO messages(recipient,message) VALUES(?,?);"
                )
        ) {
            statement.setString(1, recipient);
            statement.setString(2, message);

            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getOfflineMessages(String uuid) {
        List<String> messages = new ArrayList<>();
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "SELECT message FROM messages WHERE recipient=?;"
                )
        ) {
            statement.setString(1, uuid);
            ResultSet results = statement.executeQuery();

            while (results.next()) {
                messages.add(results.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public boolean checkIfUserExistsByName(String name) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "SELECT uuid FROM player_data WHERE name=?;"
                )
        ) {
            statement.setString(1, name);
            ResultSet results = statement.executeQuery();

            if (results.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

}

/*
uuid       CHAR(36)      NOT NULL,
name      VARCHAR(16)       NOT NULL,
last_online    BIGINT       NOT NULL,
last_submit    BIGINT       NOT NULL DEFAULT 0,
navigator   TINYINT(1)      NOT NULL DEFAULT 1,
teleport_enabled    TINYINT(1)  NOT NULL DEFAULT 1,
nightvision_enabled TINYINT(1)  NOT NULL DEFAULT 0,
chat_channel    VARCHAR(64)  NOT NULL DEFAULT 'global',
previous_coordinate INT     NOT NULL DEFAULT 0,
player_skin     TEXT    NULL DEFAULT NULL,
tips_enabled    TINYINT(1)  NOT NULL DEFAULT 1,
*/
