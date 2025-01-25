package net.bteuk.proxy.sql;

import net.bteuk.proxy.sql.migration.AcceptData;
import net.bteuk.proxy.sql.migration.DenyData;
import org.slf4j.Logger;

import java.util.List;

public class DatabaseUpdates {

    private final Logger logger;

    private final GlobalSQL globalSQL;

    private final PlotSQL plotSQL;

    private final RegionSQL regionSQL;

    public DatabaseUpdates(Logger logger, GlobalSQL globalSQL, PlotSQL plotSQL, RegionSQL regionSQL) {
        this.logger = logger;
        this.globalSQL = globalSQL;
        this.plotSQL = plotSQL;
        this.regionSQL = regionSQL;
    }

    //Update database if the config was outdated, this implies the database is also outdated.
    public void updateDatabase() {

        //Get the database version from the database.
        String version = "1.0.0";
        if (globalSQL.hasRow("SELECT data_value FROM unique_data WHERE data_key='version';")) {
            version = globalSQL.getString("SELECT data_value FROM unique_data WHERE data_key='version';");
        } else {
            //Insert the current database version as version.
            globalSQL.update("INSERT INTO unique_data(data_key, data_value) VALUES('version','1.7.2')");
        }

        //Check for specific table columns that could be missing,
        //All changes have to be tested from 1.0.0.
        //We update 1 version at a time.

        //Convert config version to integer, so we can easily use them.
        int oldVersionInt = getVersionInt(version);

        //Update sequentially.

        //1.0.0 -> 1.1.0
        if (oldVersionInt <= 1) {
            update1_2();
        }

        //1.1.0 -> 1.2.0
        if (oldVersionInt <= 2) {
            update2_3();
        }

        //1.2.0 -> 1.3.0
        if (oldVersionInt <= 3) {
            update3_4();
        }

        // 1.3.0 -> 1.4.4
        if (oldVersionInt <= 4) {
            update4_5();
        }

        // 1.4.4 -> 1.5.0
        if (oldVersionInt <= 5) {
            update5_6();
        }

        // 1.5.0 -> 1.6.0
        if (oldVersionInt <= 6) {
            update6_7();
        }

        // 1.6.0 -> 1.7.0
        if (oldVersionInt <= 7) {
            update7_8();
        }

        // 1.7.0 -> 1.7.1
        if (oldVersionInt <= 8) {
            update8_9();
        }

        // 1.7.1 -> 1.7.2
        if (oldVersionInt <= 9) {
            update9_10();
        }
    }

    private int getVersionInt(String version) {

        switch(version) {

            // 1.7.2 = 10
            case "1.7.2" -> {
                return 10;
            }

            // 1.7.1 = 9
            case "1.7.1" -> {
                return 9;
            }

            // 1.7.0 = 8
            case "1.7.0" -> {
                return 8;
            }

            // 1.6.0 = 7
            case "1.6.0" -> {
                return 7;
            }

            // 1.5.0 = 6
            case "1.5.0" -> {
                return 6;
            }

            // 1.4.4 = 5
            case "1.4.4" ->  {
                return 5;
            }

            // 1.3.0 = 4
            case "1.3.0" -> {
                return 4;
            }

            // 1.2.0 = 3
            case "1.2.0" -> {
                return 3;
            }

            // 1.1.0 = 2
            case "1.1.0" -> {
                return 2;
            }

            // Default is 1.0.0 = 1;
            default -> {
                return 1;
            }

        }

    }

    private void update9_10() {

        logger.info("Updating database from 1.7.1 to 1.7.2");

        plotSQL.update("ALTER TABLE plot_data MODIFY status ENUM('unclaimed','claimed','submitted','completed','deleted')  NOT NULL");

        plotSQL.update("RENAME TABLE plot_submissions TO plot_submission;");
        plotSQL.update("ALTER TABLE plot_submission DROP PRIMARY KEY;");
        plotSQL.update("ALTER TABLE plot_submission RENAME COLUMN id TO plot_id;");
        plotSQL.update("ALTER TABLE plot_submission ADD COLUMN status ENUM('submitted','under review','awaiting verification','under verification') NOT NULL;");
        plotSQL.update("ALTER TABLE plot_submission ADD PRIMARY KEY (plot_id);");
        plotSQL.update("ALTER TABLE plot_submission ADD CONSTRAINT fk_plot_submission_1 FOREIGN KEY (plot_id) REFERENCES plot_data(id);");

        // Migrate existing data from accept_data and deny_data to new plot_review table.
        List<DenyData> denyData = plotSQL.getDenyData();
        for (DenyData deny : denyData) {
            plotSQL.update("INSERT INTO plot_review(plot_id,uuid,reviewer,attempt,review_time,accepted,book_id " +
                    "VALUES(" + deny.id() + ",'" + deny.uuid() + "','" + deny.reviewer() + "'," +
                    deny.attempt() + "," + deny.denyTime() + "," + "0" + "," + deny.bookId() + ");");
        }
        List<AcceptData> acceptData = plotSQL.getAcceptData();
        for (AcceptData accept : acceptData) {
            // Get the highest denied attempt for the user, the accept attempt will be that +1.
            int attempt = 1 + plotSQL.getInt("SELECT MAX(attempt) FROM deny_data WHERE id=" + accept.id() + " AND uuid='" + accept.uuid() + "';");
            int id = plotSQL.insertReturnId("INSERT INTO plot_review(plot_id,uuid,reviewer,attempt,review_time,accepted,book_id " +
                    "VALUES(" + accept.id() + ",'" + accept.uuid() + "','" + accept.reviewer() + "'," +
                    attempt + "," + accept.acceptTime() + "," + "1" + "," + accept.bookId() + ");");
            // Insert an accepted plot row for the review.
            plotSQL.update("INSERT INTO accepted_plot(review_id,accuracy,quality) " +
                    "VALUES(" + id + "," + accept.accuracy() + "," + accept.quality() + ");");
        }

        // Rename the accept_data and deny_data tables to indicate they are old.
        plotSQL.update("RENAME TABLE accept_data TO old_accept_data;");
        plotSQL.update("RENAME TABLE deny_data TO old_deny_data;");

        // Version 1.7.2
        globalSQL.update("UPDATE unique_data SET data_value='1.7.2' WHERE data_key='version';");
    }

    private void update8_9() {

        logger.info("Updating database from 1.7.0 to 1.7.1");

        // Add pinned column in region_members.
        plotSQL.update("ALTER TABLE plot_members ADD COLUMN inactivity_notice TINYINT(1) NOT NULL DEFAULT 0;");

        // Version 1.7.1
        globalSQL.update("UPDATE unique_data SET data_value='1.7.1' WHERE data_key='version';");

    }

    private void update7_8() {

        logger.info("Updating database from 1.6.0 to 1.7.0");

        // Add pinned column in region_members.
        regionSQL.update("ALTER TABLE region_members ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0;");

        // Version 1.7.0
        globalSQL.update("UPDATE unique_data SET data_value='1.7.0' WHERE data_key='version';");

    }

    private void update6_7() {

        logger.info("Updating database from 1.5.0 to 1.6.0");

        // Remove online users table.
        globalSQL.update("DROP TABLE online_users;");

        // Convert messages message column from varchar(256) to clob type.
        // Add id column and use that for the primary key.
        globalSQL.update("ALTER TABLE messages DROP CONSTRAINT fk_messages_1;");
        globalSQL.update("ALTER TABLE messages DROP PRIMARY KEY;");
        globalSQL.update("ALTER TABLE messages ADD id INT NOT NULL AUTO_INCREMENT PRIMARY KEY;");
        globalSQL.update("ALTER TABLE messages MODIFY message TEXT NOT NULL;");
        globalSQL.update("ALTER TABLE messages ADD CONSTRAINT fk_messages_1 FOREIGN KEY (recipient) REFERENCES player_data(uuid);");

        // Remove staff_chat column in player_data.
        globalSQL.update("ALTER TABLE player_data DROP COLUMN staff_chat;");

        // Add chat_channel column in player_data.
        globalSQL.update("ALTER TABLE player_data ADD COLUMN chat_channel VARCHAR(64) NOT NULL DEFAULT 'global';");

        // Version 1.6.0
        globalSQL.update("UPDATE unique_data SET data_value='1.6.0' WHERE data_key='version';");

    }

    private void update5_6() {

        logger.info("Updating database from 1.4.4 to 1.5.0");

        // Update column in plot_data to add a coordinate_id with foreign key.
        plotSQL.update("ALTER TABLE plot_data ADD COLUMN coordinate_id INT NOT NULL DEFAULT 0;");

        // Version 1.5.0
        globalSQL.update("UPDATE unique_data SET data_value='1.5.0' WHERE data_key='version';");
    }

    private void update4_5() {

        logger.info("Updating database from 1.3.0 to 1.4.4");

        // Version 1.4.4
        globalSQL.update("UPDATE unique_data SET data_value='1.4.4' WHERE data_key='version';");

        // Update column in location_data for the new subcategory id as int.
        globalSQL.update("UPDATE location_data SET subcategory=NULL;");
        globalSQL.update("ALTER TABLE location_data MODIFY subcategory INT NULL DEFAULT NULL;");

        // Update column in location_requests for the new subcategory id as int.
        globalSQL.update("ALTER TABLE location_requests MODIFY subcategory INT NULL DEFAULT NULL;");

        // Add foreign key to location_data referencing the new location_subcategory table.
        globalSQL.update("ALTER TABLE location_data ADD CONSTRAINT fk_location_data_2 FOREIGN KEY (subcategory) REFERENCES location_subcategory(id);");

        // Add foreign key to location_requests referencing the new location_subcategory table.
        globalSQL.update("ALTER TABLE location_requests ADD CONSTRAINT fk_location_requests_2 FOREIGN KEY (subcategory) REFERENCES location_subcategory(id);");
    }

    private void update3_4() {

        logger.info("Updating database from 1.2.0 to 1.3.0");

        //Version 1.3.0.
        globalSQL.update("UPDATE unique_data SET data_value='1.3.0' WHERE data_key='version';");

        //Add tips_enabled to the player_data table.
        globalSQL.update("ALTER TABLE player_data ADD COLUMN tips_enabled TINYINT(1) NOT NULL DEFAULT 1;");

    }

    private void update2_3() {

        logger.info("Updating database from 1.1.0 to 1.2.0");

        //Version 1.2.0.
        globalSQL.update("UPDATE unique_data SET data_value='1.2.0' WHERE data_key='version';");

        //Add applicant to list of builder roles.
        globalSQL.update("ALTER TABLE player_data MODIFY builder_role ENUM('default','applicant','apprentice','jrbuilder','builder','architect','reviewer') DEFAULT 'default'");

    }

    private void update1_2() {

        logger.info("Updating database from 1.0.0 to 1.1.0");

        //Version 1.1.0.
        globalSQL.getString("UPDATE unique_data SET data_value='1.1.0' WHERE data_key='version';");

        //Add skin texture id column.
        globalSQL.update("ALTER TABLE player_data ADD COLUMN player_skin TEXT NULL DEFAULT NULL;");

        //Add foreign constraints.

        //id to location_data (coordinate), location_requests (coordinate) and home (coordinate_id)
        // since it references an id from the coordinates table.
        globalSQL.update("ALTER TABLE location_data ADD CONSTRAINT fk_location_data_1 FOREIGN KEY (coordinate) REFERENCES coordinates(id);");
        globalSQL.update("ALTER TABLE location_requests ADD CONSTRAINT fk_location_requests_1 FOREIGN KEY (coordinate) REFERENCES coordinates(id);");
        globalSQL.update("ALTER TABLE home ADD CONSTRAINT fk_home_1 FOREIGN KEY (coordinate_id) REFERENCES coordinates(id);");

        //uuid to join_events, server_events, statistics, online_users, server_switch, moderation, coins, discord and home
        // since it references a player that will always be in the player_data table.
        globalSQL.update("ALTER TABLE messages ADD CONSTRAINT fk_messages_1 FOREIGN KEY (recipient) REFERENCES player_data(uuid);");
        globalSQL.update("ALTER TABLE join_events ADD CONSTRAINT fk_join_events_1 FOREIGN KEY (uuid) REFERENCES player_data(uuid);");
        globalSQL.update("ALTER TABLE server_events ADD CONSTRAINT fk_server_events_1 FOREIGN KEY (uuid) REFERENCES player_data(uuid);");
        globalSQL.update("ALTER TABLE statistics ADD CONSTRAINT fk_statistics_1 FOREIGN KEY (uuid) REFERENCES player_data(uuid);");
        globalSQL.update("ALTER TABLE online_users ADD CONSTRAINT fk_online_users_1 FOREIGN KEY (uuid) REFERENCES player_data(uuid);");
        globalSQL.update("ALTER TABLE server_switch ADD CONSTRAINT fk_server_switch_1 FOREIGN KEY (uuid) REFERENCES player_data(uuid);");
        globalSQL.update("ALTER TABLE moderation ADD CONSTRAINT fk_moderation_1 FOREIGN KEY (uuid) REFERENCES player_data(uuid);");
        globalSQL.update("ALTER TABLE coins ADD CONSTRAINT fk_coins_1 FOREIGN KEY (uuid) REFERENCES player_data(uuid);");
        globalSQL.update("ALTER TABLE discord ADD CONSTRAINT fk_discord_1 FOREIGN KEY (uuid) REFERENCES player_data(uuid);");
        globalSQL.update("ALTER TABLE home ADD CONSTRAINT fk_home_2 FOREIGN KEY (uuid) REFERENCES player_data(uuid);");

        //name to online_users (server), server_switch (from_server and to_server), coordinates (server)
        // since it references servers in the server_data table.
        globalSQL.update("ALTER TABLE online_users ADD fk_online_users_2 FOREIGN KEY (server) REFERENCES server_data(name);");
        globalSQL.update("ALTER TABLE server_switch ADD fk_server_switch_2 FOREIGN KEY (from_server) REFERENCES server_data(name);");
        globalSQL.update("ALTER TABLE server_switch ADD fk_server_switch_3 FOREIGN KEY (to_server) REFERENCES server_data(name);");
        globalSQL.update("ALTER TABLE coordinates ADD fk_coordinates_1 FOREIGN KEY (server) REFERENCES server_data(name);");

    }
}
