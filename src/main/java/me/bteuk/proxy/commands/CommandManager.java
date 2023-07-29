package me.bteuk.proxy.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all Discord commands.
 * Each command is stored in a map so when a command interaction is run it can be routed accordingly.
 */
public class CommandManager extends ListenerAdapter {

    private final List<Command> commands;


    public CommandManager() {

        commands = new ArrayList<>();

    }

    /**
     * Iterates through all the commands to see if any match. If true run the onCommand method for that command.
     * @param event The command event.
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        for (Command command : commands) {
            if (event.getName().equals(command.getName())) {
                command.onCommand(event);
                break;
            }
        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {

        registerCommands(event.getGuild());

    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {

        registerCommands(event.getGuild());

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        //See if the component fits our format.
        String[] args = event.getComponentId().split(",");

        if (args.length == 2) {

            //Search for the command.
            for (Command command : commands) {
                if (args[0].equals(command.getName())) {
                    command.onButtonInteraction(event, Integer.parseInt(args[1]));
                    break;
                }
            }

        }
    }

    /**
     * Registers all the Discord commands. Is called when either the guild is ready, or the bot has joined the guild.
     */
    private void registerCommands(Guild guild) {

        //Create commands.
        commands.add(new Playerlist("playerlist", "List all online players on the Minecraft server."));

        List<CommandData> commandData = new ArrayList<>();

        //Add the command data for each command.
        for (Command command: commands) {
            commandData.add(Commands.slash(command.getName(), command.getDescription()));
        }

        //Add the commands to the guild.
        guild.updateCommands().addCommands(commandData).queue();

    }
}
