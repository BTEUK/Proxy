package me.bteuk.proxy.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Playerlist extends ListenerAdapter {

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if (event.getName().equalsIgnoreCase("playerlist")) {
            event.deferReply().queue();
            event.getHook().sendMessage("Testing the command!").queue();
        }
    }
}
