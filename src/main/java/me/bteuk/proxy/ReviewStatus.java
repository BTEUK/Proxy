package me.bteuk.proxy;

//This class manages the embedded message in the support-info channel, this will tell reviewers the status of submitted plots, and other reviewing related information.

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ReviewStatus {

    private String messageID;
    private final TextChannel reviewerChannel;

    //Build embed.
    public ReviewStatus() {

        //Get channel.
        reviewerChannel = Proxy.getInstance().getDiscord().getReviewerChannel();

        //Try to get the message ID from config, if it does not exist, create a new message id.
        messageID = Proxy.getInstance().getConfig().getString("message.reviewer");

        //Create scheduler to update the message frequently.
        //Run a delayed task to remove this from the list.
        //Create new message.
        //Update message.
        Proxy.getInstance().getServer().getScheduler().buildTask(Proxy.getInstance(), () -> {

                    if (messageID == null) {

                        //Create new message.
                        createMessage();

                    } else {

                        //Update message.
                        updateMessage();

                    }

                })
                .repeat(1L, TimeUnit.MINUTES)
                .schedule();

    }

    private void createMessage() {

        reviewerChannel.sendMessageEmbeds(getEmbed()).queue((message) -> {

            //Set message id for next time.
            messageID = message.getId();
        });

    }

    private void updateMessage() {

        reviewerChannel.retrieveMessageById(messageID).queue((message) -> {
            // use the message here, its an async callback
            message.editMessageEmbeds(getEmbed()).queue();
        }, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, (e) -> reviewerChannel.sendMessage("The message with id " + messageID + " does not exist!").queue()));


    }

    private MessageEmbed getEmbed() {

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Support Information");

        //Submitted plots, show up to 5 in a list.
        ArrayList<Integer> plots = Proxy.getInstance().plotSQL.getIntList("SELECT id FROM plot_data WHERE status='submitted';");
        StringBuilder plot_message = new StringBuilder();

        if (plots.size() == 0) {
            plot_message = new StringBuilder("There are 0 plots waiting to be reviewed!");
        } else {

            //Add up to 5 plots to the list.
            int counter = 0;
            for (int plot : plots) {

                plot_message.append("• Plot ").append(plot).append(" submitted by ").append(Proxy.getInstance().globalSQL.getString("SELECT name FROM player_data WHERE uuid='" +
                        Proxy.getInstance().plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + plot + " AND is_owner=1;") + "';"));

                counter++;

                if (plots.size() > counter) {
                    plot_message.append("\n");
                }

                if (counter > 5) {
                    break;
                }
            }

            if (plots.size() > 5) {
                plot_message.append("*and ").append(plots.size() - 5).append(" more...*");
            }
        }

        //Number of available plots in the plotsystem.
        MessageEmbed.Field plot_submissions = new MessageEmbed.Field("Plot Submissions", plot_message.toString(), false);

        //Plot availability, list number of unclaimed plots for each difficulty.
        String plots_per_difficulty = "• Easy: " + Proxy.getInstance().plotSQL.getInt("SELECT COUNT(id) FROM plot_data WHERE status='unclaimed' AND difficulty=1;") +
                "\n• Normal: " + Proxy.getInstance().plotSQL.getInt("SELECT COUNT(id) FROM plot_data WHERE status='unclaimed' AND difficulty=2;") +
                "\n• Hard: " + Proxy.getInstance().plotSQL.getInt("SELECT COUNT(id) FROM plot_data WHERE status='unclaimed' AND difficulty=3;");

        MessageEmbed.Field plot_availability = new MessageEmbed.Field("Plots Available", plots_per_difficulty, false);

        //Number of navigation requests.
        //Submitted plots, show up to 5 in a list.
        ArrayList<String> locations = Proxy.getInstance().globalSQL.getStringList("SELECT location FROM location_requests;");
        StringBuilder navigation_message = new StringBuilder();

        if (locations.size() == 0) {
            navigation_message = new StringBuilder("There are 0 navigation requests waiting to be reviewed!");
        } else {

            //Add up to 5 plots to the list.
            int counter = 0;
            for (String location : locations) {

                navigation_message.append("• Location ").append(location).append(" requested");

                counter++;

                if (locations.size() > counter) {
                    navigation_message.append("\n");
                }

                if (counter > 5) {
                    break;
                }
            }

            if (locations.size() > 5) {
                navigation_message.append("*and ").append(locations.size() - 5).append(" more...*");
            }
        }

        MessageEmbed.Field navigation_requests = new MessageEmbed.Field("Navigation Requests", navigation_message.toString(), false);

        //Number of region requests.
        ArrayList<String[]> regions = Proxy.getInstance().regionSQL.getStringArrayList("SELECT region,uuid FROM region_requests WHERE staff_accept=0;");
        StringBuilder region_message = new StringBuilder();

        if (regions.size() == 0) {
            region_message = new StringBuilder("There are 0 region requests waiting to be reviewed!");
        } else {

            //Add up to 5 plots to the list.
            int counter = 0;
            for (String[] region : regions) {

                region_message.append("• Region ").append(region[0]).append(" requested by ")
                        .append(Proxy.getInstance().globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + region[1] + "';"));

                counter++;

                if (regions.size() > counter) {
                    region_message.append("\n");
                }

                if (counter > 5) {
                    break;
                }
            }

            if (regions.size() > 5) {
                region_message.append("*and ").append(locations.size() - 5).append(" more...*");
            }
        }

        MessageEmbed.Field region_requests = new MessageEmbed.Field("Region Requests", region_message.toString(), false);

        eb.addField(plot_submissions);
        eb.addField(plot_availability);
        eb.addField(navigation_requests);
        eb.addField(region_requests);

        eb.setFooter("Last updated: " + Time.getDateTime(Time.currentTime()));
        //eb.addField(navigation);
        //eb.setDescription("**" + chatMessage + "**");
        eb.setColor(Color.CYAN);
        return eb.build();

    }
}
