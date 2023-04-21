package me.bteuk.proxy;

//This class manages the embedded message in the support-info channel, this will tell reviewers the status of submitted plots, and other reviewing related information.

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.awt.*;
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
        eb.setTitle("Reviewing Status");

        int plot_count = Proxy.getInstance().plotSQL.getInt("SELECT count(id) FROM plot_data WHERE status='submitted';");
        String plot_message;

        if (plot_count == 1) {
            plot_message = "There is 1 plot waiting to be reviewed!";
        } else {
            plot_message = "There are " + plot_count + " plots waiting to be reviewed!";
        }

        MessageEmbed.Field plots = new MessageEmbed.Field("Plot Submissions", plot_message, false);
        eb.addField(plots);

        //MessageEmbed.Field navigation = new MessageEmbed.Field("Navigation Submissions", "There are currently 0 navigation submission.", false);
        eb.setFooter("Last updated: " + Time.getDateTime(Time.currentTime()));
        //eb.addField(navigation);
        //eb.setDescription("**" + chatMessage + "**");
        eb.setColor(Color.RED);
        return eb.build();

    }
}
