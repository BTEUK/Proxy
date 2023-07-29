package me.bteuk.proxy.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface Command {

    String getName();

    String getDescription();

    void onCommand(SlashCommandInteractionEvent event);

    void onButtonInteraction(ButtonInteractionEvent event, int page);

}
