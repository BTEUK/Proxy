package me.bteuk.proxy.utils;

import me.bteuk.proxy.Proxy;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public class PlotListMessage {

    private final String type;
    private final String title;
    private final String emptyMessage;
    private final int page;
    private final int maxPages;

    private final ArrayList<Integer> plots;

    /**
     * @param type         Type of plot list.
     * @param title        Title of the MessageEmbed
     * @param query        SQL query that will get the list of plots
     * @param emptyMessage Message to send if no plots are found
     * @param page         Page
     */
    public PlotListMessage(String type, String title, String query, String emptyMessage, int page) {
        this.type = type;
        this.title = title;
        this.emptyMessage = emptyMessage;

        plots = Proxy.getInstance().getPlotSQL().getIntList(query);

        //Calculate maximum number of pages.
        maxPages = (int) Math.ceil(plots.size() / 10d);

        //If the list has less plots than the current page number.
        //For example if there are 10 plots per page, and there are 17 plots then any page above 2 would be too much.
        //In this case decrease the page count to the highest.
        if (plots.size() < (page - 1) * 10) {
            this.page = maxPages;
        } else {
            this.page = page;
        }
    }

    /**
     * Adds buttons to the event reply.
     */
    public void addButtons(MessageRequest<?> message) {

        //Clear the components of the message.
        message.setComponents();

        Collection<Button> components = new ArrayList<>();

        //If the page is greater than 1, add a previous page button.
        if (page > 1) {

            //Set the id to the slash command.
            components.add(Button.primary(type + "," + (page - 1), "◀"));

        }

        //If there are more plots than fit on this page, add a next page button.
        if (plots.size() > page * 10) {

            //Set the id to the slash command.
            components.add(Button.primary(type + "," + (page + 1), "▶"));

        }

        //Add the buttons to the reply.
        if (!components.isEmpty()) {
            message.setActionRow(components);
        }

    }

    /**
     * @return {@link MessageEmbed} with a list of plots, if any.
     */
    public MessageEmbed createList() {

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        eb.setColor(Color.CYAN);

        //Check if the list is empty.
        if (plots.isEmpty()) {

            eb.setDescription(emptyMessage);
            return eb.build();

        }

        StringBuilder plot_message = new StringBuilder();

        //If the page is greater than 1, set the starting index accordingly.
        int index = (page - 1) * 10;

        //Iterate until the index is divisible by 10 again.
        do {

            //Add message for the plot.
            plot_message.append("• Plot ").append(plots.get(index));

            //If the plot has an owner, add their name.
            if (Proxy.getInstance().getPlotSQL().hasRow("SELECT id FROM plot_members WHERE id=" + plots.get(index) + " AND is_owner=1;")) {
                plot_message.append(" - claimed by: ").append(Proxy.getInstance().getGlobalSQL().getString("SELECT name FROM player_data WHERE uuid='" +
                        Proxy.getInstance().getPlotSQL().getString("SELECT uuid FROM plot_members WHERE id=" + plots.get(index) + " AND is_owner=1;") + "';"));
            }

            //If the plot is completed, add the builder.
            if (Proxy.getInstance().getPlotSQL().hasRow("SELECT id FROM plot_data WHERE id=" + plots.get(index) + " AND status='completed'")) {
                plot_message.append(" - completed by: ").append(Proxy.getInstance().getGlobalSQL().getString("SELECT name FROM player_data WHERE uuid='" +
                        Proxy.getInstance().getPlotSQL().getString("SELECT uuid FROM accept_data WHERE id=" + plots.get(index) + ";") + "';"));
            }

            //If the plot is currently being reviewed add that.
            if (Proxy.getInstance().getPlotSQL().hasRow("SELECT id FROM plot_data WHERE id=" + plots.get(index) + " AND status='reviewing';")) {
                plot_message.append(" (under review)");
            }

            //Increment index.
            index++;

            //If there are more plots and this isn't the last of the page add a new line.
            if (plots.size() > index && index % 10 != 0) {
                plot_message.append("\n");
            }

        } while (index % 10 != 0 && plots.size() > index);

        eb.setDescription(plot_message.toString());

        eb.setFooter("Page " + page + "/" + maxPages);
        return eb.build();

    }
}
