package me.bteuk.proxy;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Discord {

    private JDA jda;

    private TextChannel chat;
    private TextChannel staff;
    private TextChannel supportInfo;
    private TextChannel supportChat;

    private final String reviewer;

    private List<Long> hasRoles;
    private List<Long> giveRoles;

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

            //Load all members into cache.
            chat.getGuild().loadMembers().onSuccess(members -> {
                Proxy.getInstance().getLogger().info("Loaded all discord members into cache");

                //Enable role syncing.
                enableRoleSyncing();
            });

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void unlinkUser(long userID) {
        //Remove the user from the discord link table.
        if (Proxy.getInstance().getGlobalSQL().hasRow("SELECT discord_id FROM discord WHERE discord_id='" + userID + "';")) {
            Proxy.getInstance().getGlobalSQL().update("DELETE FROM discord WHERE discord_id=" + userID + ";");
            Proxy.getInstance().getLogger().info(("Removed discord link for " + userID + ", they are no longer in the discord server."));
        } else {
            //The link does not exist in the database, make sure it's removed for the Network also.
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            try {
                out.writeUTF(GsonComponentSerializer.gson().serialize(Component.text("unlink " + userID)));
            } catch (IOException ex) {
                Proxy.getInstance().getLogger().info("Unable to send unlink message for " + userID);
            }
            for (RegisteredServer server : Proxy.getInstance().getServer().getAllServers()) {
                server.sendPluginMessage(MinecraftChannelIdentifier.create("uknet", "discord_linking"), stream.toByteArray());
            }
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

    public void addRole(long user_id, long role_id, boolean sync) {
        // Only give the role if they don't have it yet.
        try {
            // Get the member.
            Member member = chat.getGuild().getMember(UserSnowflake.fromId(user_id));
            if (member == null) {
                // Unlink user is linked.
                unlinkUser(user_id);
                return;
            }
            // Get the role.
            Role role = chat.getGuild().getRoleById(role_id);
            if (role == null) {
                return;
            }
            // If the member does not have the role, add it.
            if (!member.getRoles().contains(role)) {
                // If successful, resync if enabled.
                chat.getGuild().addRoleToMember(member, role).queue(
                        (user) -> {
                            if (sync && hasRoles != null && giveRoles != null) {
                                syncRoles();
                            }
                        }, new UnknownUserErrorHandler(user_id)
                );
            }
        } catch (Exception e) {
            //An error occurred, the user or role is null, this is not necessarily a problem, but is being caught to prevent console spam.
        }
    }

    public void removeRole(long user_id, long role_id, boolean sync) {
        // Only remove the role if they don't have it yet.
        try {
            // Get the member.
            Member member = chat.getGuild().getMember(UserSnowflake.fromId(user_id));
            if (member == null) {
                // Unlink user is linked.
                unlinkUser(user_id);
                return;
            }
            // Get the role.
            Role role = chat.getGuild().getRoleById(role_id);
            if (role == null) {
                return;
            }
            // If the member does not have the role, add it.
            if (member.getRoles().contains(role)) {
                chat.getGuild().removeRoleFromMember(member, role).queue(
                        (user) -> {
                            if (sync && hasRoles != null && giveRoles != null) {
                                syncRoles();
                            }
                        }, new UnknownUserErrorHandler(user_id)
                );
            }
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

        hasRoles = Proxy.getInstance().getConfig().getLongArray("role_syncing.has");
        giveRoles = Proxy.getInstance().getConfig().getLongArray("role_syncing.give");

        if (hasRoles == null || giveRoles == null) {
            return;
        }

        Proxy.getInstance().getServer().getScheduler().buildTask(Proxy.getInstance(), this::syncRoles)
                .repeat(5L, TimeUnit.MINUTES)
                .schedule();
    }

    private void syncRoles() {
        // Get lists of all members with all the roles
        Map<Role, List<Member>> hasRolesMap = fillRoleMap(hasRoles);
        Map<Role, List<Member>> giveRolesMap = fillRoleMap(giveRoles);

        // Remove the role from members that shouldn't have it.
        for (Role role : giveRolesMap.keySet()) {
            chat.getGuild().getMembersWithRoles(role).forEach(member -> {
                if (member.getRoles().stream().noneMatch(hasRolesMap::containsKey)) {
                    removeRole(member.getIdLong(), role.getIdLong(), false);
                }
            });
        }

        // Add the roles to all members who should have it.
        for (Role role : hasRolesMap.keySet()) {
            chat.getGuild().getMembersWithRoles(role).forEach(member -> {
                for (Role giveRole : giveRolesMap.keySet()) {
                    // Only give the role if they don't have it yet.
                    if (!member.getRoles().contains(giveRole)) {
                        addRole(member.getIdLong(), giveRole.getIdLong(), false);
                    }
                }
            });
        }
    }

    private Map<Role, List<Member>> fillRoleMap(List<Long> role_ids) {
        Map<Role, List<Member>> roleMap = new HashMap<>();
        for (long role_id : role_ids) {
            // Get the role.
            Role role = chat.getGuild().getRoleById(role_id);
            if (role != null) {
                //Get all members with the role.
                roleMap.put(role, chat.getGuild().getMembersWithRoles(role));
            }
        }
        return roleMap;
    }
}
