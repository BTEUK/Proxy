package me.bteuk.proxy.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SubmittedPlots extends AbstractCommand {

    /**
     * Constructor, saved the name and description of the command.
     * Also registers the command in Discord.
     *
     * @param name        Name of the command
     * @param description Description of the command
     */
    public SubmittedPlots(String name, String description) {
        super(name, description);
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

    }
}
