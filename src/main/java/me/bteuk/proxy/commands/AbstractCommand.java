package me.bteuk.proxy.commands;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public abstract class AbstractCommand implements Command {

    private final String name;
    private final String description;

    /**
     * Constructor, saved the name and description of the command.
     * Also registers the command in Discord.
     * @param name Name of the command
     * @param description Description of the command
     */
    public AbstractCommand(String name, String description) {

        this.name = name;
        this.description = description;

    }
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void onButtonInteraction(ButtonInteractionEvent event, int page) {}
}
