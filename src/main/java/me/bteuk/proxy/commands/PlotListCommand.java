package me.bteuk.proxy.commands;

import me.bteuk.proxy.utils.PlotListMessage;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public abstract class PlotListCommand extends AbstractCommand {

    private final String TYPE;
    private final String TITLE;
    private final String QUERY;
    private final String EMPTY;

    public PlotListCommand(String name, String description, String title, String query, String empty) {
        super(name, description);
        TYPE = name;
        TITLE = title;
        QUERY = query;
        EMPTY = empty;
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

        PlotListMessage message = new PlotListMessage(TYPE, TITLE, QUERY, EMPTY, 1);

        ReplyCallbackAction reply = event.replyEmbeds(message.createList());
        message.addButtons(reply);
        reply.queue();

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event, int page) {

        PlotListMessage message = new PlotListMessage(TYPE, TITLE, QUERY, EMPTY, page);

        MessageEditCallbackAction edit = event.editMessageEmbeds();
        message.addButtons(edit);
        edit.queue();

    }
}
