package me.bteuk.proxy;

//This class manages the embedded message in the support-info channel, this will tell reviewers the status of submitted plots, and other reviewing related information.

import com.velocitypowered.api.scheduler.ScheduledTask;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class ReviewStatus {

    private long messageID;
    private TextChannel reviewerChannel;

    private ScheduledTask task;

    //Build embed.
    public ReviewStatus() {

        //Get channel.
        reviewerChannel = Proxy.getInstance().getDiscord().getReviewerChannel();

        //Try to get the message ID from config, if it does not exist, create a new message id.
        int id = Proxy.getInstance().getConfig().getInt("message.reviewer");

        //Create scheduler to update the message frequently.
        //Run a delayed task to remove this from the list.
        task = Proxy.getInstance().getServer().getScheduler().buildTask(Proxy.getInstance(), () -> {

                    if (id == 0) {

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
            messageID = message.getIdLong();
        });

    }

    private void updateMessage() {

        reviewerChannel.retrieveMessageById(messageID).queue((message) -> {
            // use the message here, its an async callback
            message.editMessageEmbeds(getEmbed()).queue();
        }, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, (e) -> {

            reviewerChannel.sendMessage("The message with id " + messageID + " does not exist!").queue();

        }));


    }

    private MessageEmbed getEmbed() {

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Reviewing Status");
        MessageEmbed.Field plots = new MessageEmbed.Field("Plot Submissions", "There are currently 0 submitted plots.", false);
        MessageEmbed.Field navigation = new MessageEmbed.Field("Navigation Submissions", "There are currently 0 navigation submission.", false);
        eb.setFooter("Last updated: " + Time.getDateTime(Time.currentTime()));
        eb.addField(plots);
        eb.addField(navigation);
        //eb.setDescription("**" + chatMessage + "**");
        eb.setColor(Color.RED);
        return eb.build();

    }
}
