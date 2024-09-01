package net.bteuk.proxy.commands;

import net.bteuk.proxy.Proxy;
import net.bteuk.proxy.sql.GlobalSQL;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.ArrayList;
import java.util.Comparator;

public class Playerlist extends AbstractCommand {

    /**
     * Constructor, saved the name and description of the command.
     * Also registers the command in Discord.
     * @param name Name of the command
     * @param description Description of the command
     */
    public Playerlist(String name, String description) {
        super(name, description);
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

        String playerListMessage;
        GlobalSQL globalSQL = Proxy.getInstance().getGlobalSQL();

        //Create list of players.
        ArrayList<String> players = globalSQL.getStringList("SELECT uuid FROM online_users;");
        ArrayList<String> playerFormat = new ArrayList<>();

        //Set initial line and default message for empty server.
        if (players.isEmpty()) {
            playerListMessage = "**There are currently no players online!**";
        } else {
            playerListMessage = "**Online players on UKnet (" + players.size() + ")**";

            for (String uuid : players) {
                //Get primary role and name.
                playerFormat.add("[" + globalSQL.getString("SELECT primary_role FROM online_users WHERE uuid='" + uuid + "';") + "] " +
                        globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + uuid + "';"));
            }

            playerListMessage += "\n```\n";

            //Sort the list of players by alphabetical order, since roles are first it'll be by role.
            playerFormat.sort(Comparator.naturalOrder());

            //Add the players separated by comma.
            playerListMessage += String.join(", ", playerFormat);

            //If message exceeds discord character limit cut it short.
            if (playerListMessage.length() > 2000) {
                playerListMessage = playerListMessage.substring(0, 1993) + "...";
            }

            playerListMessage += "\n```";

        }

        ReplyCallbackAction reply = event.reply(playerListMessage);
        reply = reply.setEphemeral(true);
        reply.queue();

    }
}
