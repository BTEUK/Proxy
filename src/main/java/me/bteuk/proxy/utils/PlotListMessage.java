package me.bteuk.proxy.utils;

import me.bteuk.proxy.Proxy;
import me.bteuk.proxy.sql.GlobalSQL;
import me.bteuk.proxy.sql.PlotSQL;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.util.ArrayList;

public class PlotListMessage {

    /**
     * Static method that creates an embed for a list of plots with the given query.
     *
     * @param title Title of the MessageEmbed
     * @param query SQL query that will get the list of plots
     * @param emptyMessage Message to send if no plots are found
     * @param page Page
     *
     * @return {@link MessageEmbed} with a list of plots, if any, and potential buttons for the next or previous page.
     */
    private static MessageEmbed createList(String title, String query, String emptyMessage, int page) {

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);

        //Get the list of plots.
        ArrayList<Integer> plots = Proxy.getInstance().getPlotSQL().getIntList(query);

        //Check if the list is empty.
        if (plots.isEmpty()) {

            eb.appendDescription(emptyMessage);
            return eb.build();

        }

        //Calculate maximum number of pages.
        int maxPages = (int) Math.ceil(page / 10d);

        //If the list has less plots than the current page number.
        //For example if there are 10 plots per page, and there are 17 plots then any page above 2 would be too much.
        //In this case decrease the page count to the highest.
        if (plots.size() < (page - 1) * 10) {
            page = maxPages;
        }

        //If there are more plots than fit on this page, add a next page button.
        if (plots.size() > page * 10) {

        }

        //If the page is greater than 1, add a previous page button.
        if (page > 1) {

        }



        StringBuilder plot_message = new StringBuilder();

        if (plots.size() == 0) {
            plot_message = new StringBuilder("There are 0 plots waiting to be reviewed!");
        } else {

            //Add up to 5 plots to the list.
            int counter = 0;
            for (int plot : plots) {

                //If plot status is 'reviewing', then add additional info that the plot is currently under review.
                if (Proxy.getInstance().plotSQL.hasRow("SELECT id FROM plot_data WHERE id=" + plot + " AND status='reviewing';")) {
                    plot_message.append("• Plot ").append(plot).append(" submitted by ").append(Proxy.getInstance().globalSQL.getString("SELECT name FROM player_data WHERE uuid='" +
                            Proxy.getInstance().plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + plot + " AND is_owner=1;") + "';")).append(" (under review)");
                } else {
                    plot_message.append("• Plot ").append(plot).append(" submitted by ").append(Proxy.getInstance().globalSQL.getString("SELECT name FROM player_data WHERE uuid='" +
                            Proxy.getInstance().plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + plot + " AND is_owner=1;") + "';"));
                }

                counter++;

                if (plots.size() > counter) {
                    plot_message.append("\n");
                }

                if (counter > 4) {
                    break;
                }
            }

            if (plots.size() > 5) {
                plot_message.append("*and ").append(plots.size() - 5).append(" more...*");
            }
        }

        eb.setFooter("Page " + page + "/" + maxPages);
        //eb.addField(navigation);
        //eb.setDescription("**" + chatMessage + "**");
        eb.setColor(Color.CYAN);
        return eb.build();

    }

}
