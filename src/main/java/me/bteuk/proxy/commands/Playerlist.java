package me.bteuk.proxy.commands;

import me.bteuk.proxy.sql.GlobalSQL;
import me.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Playerlist extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("playerlist")) {

            event.deferReply().setEphemeral(true).queue();

            String playerListMessage;
            GlobalSQL globalSQL = Proxy.getInstance().globalSQL;

            //Create list of players.
            ArrayList<String> players = globalSQL.getStringList("SELECT uuid FROM online_users;");
            ArrayList<String> playerFormat = new ArrayList<>();

            //Set initial line and default message for empty server.
            if (players.size() == 0) {
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

            event.getHook().sendMessage(playerListMessage).queue();

        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {

        //Add the commands.
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("playerlist", "List all online players on the Minecraft server."));
        event.getGuild().updateCommands().addCommands(commandData).queue();

    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {

        //Add the commands.
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("playerlist", "List all online players on the Minecraft server."));
        event.getGuild().updateCommands().addCommands(commandData).queue();

    }
}
