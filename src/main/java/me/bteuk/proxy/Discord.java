package me.bteuk.proxy;

import me.bteuk.proxy.commands.CommandManager;
import me.bteuk.proxy.events.BotChatListener;
import me.bteuk.proxy.events.DiscordChatListener;
import me.bteuk.proxy.log4j.JdaFilter;
import me.bteuk.proxy.sql.PlotSQL;
import me.bteuk.proxy.utils.ChatFormatter;
import me.bteuk.proxy.utils.UnknownUserErrorHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Discord {

    private JDA jda;

    private TextChannel chat;
    private TextChannel staff;
    private TextChannel supportInfo;
    private TextChannel supportChat;

    private final String reviewer;

    public Discord() {

        // add log4j filter for JDA messages
        JdaFilter jdaFilter = new JdaFilter();
        ((org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger()).addFilter(jdaFilter);
        Proxy.getInstance().getLogger().debug("JdaFilter applied");

        //Get token from config.
        String token = Proxy.getInstance().getConfig().getString("token");
        String chat_channel = Proxy.getInstance().getConfig().getString("chat.global");
        String support_info = Proxy.getInstance().getConfig().getString("chat.support.info");
        String support_chat = Proxy.getInstance().getConfig().getString("chat.support.chat");
        String staff_channel = Proxy.getInstance().getConfig().getString("chat.staff");

        reviewer = Proxy.getInstance().getConfig().getString("role.reviewer");

        //Create JDABuilder.
        JDABuilder builder = JDABuilder.createDefault(token);

        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.enableIntents(GatewayIntent.DIRECT_MESSAGES);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES);

        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setChunkingFilter(ChunkingFilter.NONE);
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.setLargeThreshold(50);

        builder.setAutoReconnect(true);

        builder.setActivity(Activity.playing("bteuk.net"));

        builder.addEventListeners(new DiscordChatListener(chat_channel, support_chat, staff_channel));
        builder.addEventListeners(new BotChatListener());
        builder.addEventListeners(new CommandManager());

        try {
            jda = builder.build();
            jda.awaitReady();

            chat = jda.getTextChannelById(chat_channel);
            supportInfo = jda.getTextChannelById(support_info);
            supportChat = jda.getTextChannelById(support_chat);
            staff = jda.getTextChannelById(staff_channel);

            //Enable role syncing.
            enableRoleSyncing();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendConnectDisconnectMessage(String message, boolean connect) {

        String[] aMessage = message.split(" ");
        String url = aMessage[0];
        String chatMessage = String.join(" ", Arrays.copyOfRange(aMessage, 1, aMessage.length));

        //If channel is connect send connect message using embed.
        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor(chatMessage, null, url);
        //eb.setDescription("**" + chatMessage + "**");
        eb = (connect) ? eb.setColor(Color.GREEN) : eb.setColor(Color.RED);
        chat.sendMessageEmbeds(eb.build()).queue();

    }

    public void sendDisconnectBlockingMessage(String message, AtomicInteger users) {

        String[] aMessage = message.split(" ");
        String url = aMessage[0];
        String chatMessage = String.join(" ", Arrays.copyOfRange(aMessage, 1, aMessage.length));

        //If channel is connect send connect message using embed.
        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor(chatMessage, null, url);
        //eb.setDescription("**" + chatMessage + "**");
        eb.setColor(Color.RED);

        chat.sendMessageEmbeds(eb.build()).queue((reply) -> users.decrementAndGet());

    }

    public void updateReviewerChannel() {

        //When a message is sent in the reviewer channel update the channel topic to the number of submitted plots.
        int plot_count = Proxy.getInstance().getPlotSQL().getInt("SELECT count(id) FROM plot_data WHERE status='submitted';");
        String topic;

        if (plot_count == 1) {
            topic = "There is 1 plot waiting to be reviewed!";
        } else {
            topic = "There are " + plot_count + " plots waiting to be reviewed!";
        }

        chat.getManager().setTopic(topic);

    }

    public void sendMessage(String message) {

        //TODO: could consider applying bold, italic and other formatting to discord messages if used in Minecraft.

        //If chat channel is global send it.
        chat.sendMessage(message).queue();

    }

    public void sendStaffMessage(String message) {

        //Send message to staff channel.
        staff.sendMessage(message).queue();

    }

    /**
     * Send an announcement, the type of announcement is the first of the string.
     *
     * @param message the message to announce
     */
    public void sendAnnouncement(String message) {

        String[] aMessage = message.split(" ");
        String type = aMessage[0];
        String chatMessage = String.join(" ", Arrays.copyOfRange(aMessage, 1, aMessage.length));

        switch (type) {
            case "afk" -> sendItalicMessage(chatMessage);
            case "promotion" -> sendBoldMessage(chatMessage);
            case "connect" -> sendConnectDisconnectMessage(chatMessage, true);
            case "disconnect" -> sendConnectDisconnectMessage(chatMessage, false);
        }
    }

    /**
     * Send a DM to the user telling them their plot was accepted/denied.
     * Additionally send feedback is applicable or the promoted role if applicable.
     *
     * @param userID the user to send the DM to
     * @param params the parameters used to construct the message in the following order, UUID of player, accepted/denied, plot id, (optional) promoted role.
     */
    public void sendReviewingUpdateDM(String userID, String[] params) {

        PlotSQL plotSQL = Proxy.getInstance().getPlotSQL();

        //Construct the message.
        StringBuilder builder = new StringBuilder();
        builder.append("Plot ").append(params[2]).append(" has been ").append(params[1]);

        //If the user was promoted add that.
        if (params.length == 4) {
            builder.append("\n").append("You have been promoted to **").append(params[3]).append("**!");
        }

        //If there is feedback.
        //0 means no feedback.
        int book_id;
        if (params[1].equals("accepted")) {
            book_id = plotSQL.getInt("SELECT book_id FROM accept_data WHERE id=" + Integer.parseInt(params[2]) + ";");
        } else {
            //Find the book id of the latest attempt.
            book_id = plotSQL.getInt("SELECT book_id FROM deny_data WHERE id=" + Integer.parseInt(params[2]) + " ORDER BY attempt DESC;");
        }

        if (book_id != 0) {
            //Add feedback to the message.
            ArrayList<String> pages = plotSQL.getStringList("SELECT contents FROM book_data WHERE id=" + book_id + " ORDER BY page ASC;");

            builder.append("\n").append("Feedback: ").append(String.join(" ", pages));
        }

        String message = ChatFormatter.escapeDiscordFormatting(builder.toString());

        //Cut the message off at 2000 characters.
        if (message.length() > 2000) {
            message = builder.substring(0, 1997) + "...";
        }

        //Get discord user.
        String finalMessage = message;
        jda.retrieveUserById(userID).queue(user -> {
            //Open a private channel with the user and send the message.
            user.openPrivateChannel().queue(channel -> channel.sendMessage(finalMessage).queue());
        });
    }

    public void addRole(long user_id, long role_id) {
        try {
            //Get role.
            chat.getGuild().addRoleToMember(UserSnowflake.fromId(user_id), Objects.requireNonNull(chat.getGuild().getRoleById(role_id))).queue(
                    null, new UnknownUserErrorHandler(user_id)
            );
        } catch (Exception e) {
            //An error occurred, the user or role is null, this is not necessarily a problem, but is being caught to prevent console spam.
        }
    }

    public void removeRole(long user_id, long role_id) {
        try {
            chat.getGuild().removeRoleFromMember(UserSnowflake.fromId(user_id), Objects.requireNonNull(chat.getGuild().getRoleById(role_id))).queue(
                    null, new UnknownUserErrorHandler(user_id)
            );
        } catch (Exception e) {
            //An error occurred, the user or role is null, this is not necessarily a problem, but is being caught to prevent console spam.
        }
    }

    public JDA getJda() {
        return jda;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    public TextChannel getSupportInfoChannel() {
        return supportInfo;
    }

    public TextChannel getSupportChatChannel() {
        return supportChat;
    }

    public String getReviewerRoleID() {
        return reviewer;
    }

    private void sendItalicMessage(String message) {
        sendMessage("*" + ChatFormatter.escapeDiscordFormatting(message) + "*");
    }

    private void sendBoldMessage(String message) {
        sendMessage("**" + ChatFormatter.escapeDiscordFormatting(message) + "**");
    }

    private void enableRoleSyncing() {

        List<Long> hasRoles = Proxy.getInstance().getConfig().getLongArray("role_syncing.has");
        List<Long> giveRoles = Proxy.getInstance().getConfig().getLongArray("role_syncing.give");

        if (hasRoles == null || giveRoles == null) {
            return;
        }

        Proxy.getInstance().getServer().getScheduler().buildTask(Proxy.getInstance(), () -> {
                    // Remove the role from members that shouldn't have it.
                    for (long role_id : giveRoles) {
                        Role role = chat.getGuild().getRoleById(role_id);
                        if (role != null) {
                            chat.getGuild().findMembersWithRoles(role).onSuccess(members -> members.forEach(member -> {
                                if (member.getRoles().stream().noneMatch(memberRole -> hasRoles.contains(memberRole.getIdLong()))) {
                                    removeRole(member.getIdLong(), role_id);
                                }
                            }));
                        }
                    }

                    // Add the roles to all members who should have it.
                    for (long role_id : hasRoles) {
                        Role role = chat.getGuild().getRoleById(role_id);
                        if (role != null) {
                            chat.getGuild().findMembersWithRoles(role).onSuccess(members -> {
                                members.forEach(member -> {
                                    for (long giveRole : giveRoles) {
                                        addRole(member.getIdLong(), giveRole);
                                    }
                                });
                            });
                        }
                    }
                })
                .repeat(5L, TimeUnit.MINUTES)
                .schedule();
    }
}
