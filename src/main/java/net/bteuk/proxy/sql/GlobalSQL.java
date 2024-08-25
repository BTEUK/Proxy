package net.bteuk.proxy.sql;

import net.bteuk.network.lib.dto.UserConnectRequest;
import net.bteuk.proxy.utils.Time;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
