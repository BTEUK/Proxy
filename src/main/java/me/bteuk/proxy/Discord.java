package me.bteuk.proxy;

import me.bteuk.proxy.commands.Playerlist;
import me.bteuk.proxy.events.BotChatListener;
import me.bteuk.proxy.events.DiscordChatListener;
import me.bteuk.proxy.log4j.JdaFilter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Discord {

    private JDA jda;

    private JdaFilter jdaFilter;

    private TextChannel chat;
    private TextChannel staff;
    private TextChannel supportInfo;
    private TextChannel supportChat;

    private final String reviewer;

    public Discord() {

        // add log4j filter for JDA messages
        try {
            Class<?> jdaFilterClass = Class.forName("me.bteuk.proxy.log4j.JdaFilter");
            jdaFilter = (JdaFilter) jdaFilterClass.newInstance();
            ((org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger()).addFilter(jdaFilter);
            Proxy.getInstance().getLogger().debug("JdaFilter applied");
        } catch (Exception e) {
            Proxy.getInstance().getLogger().error("Failed to attach JDA message filter to root logger", e);
        }

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

        builder.setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER));
        builder.setChunkingFilter(ChunkingFilter.NONE);
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.setLargeThreshold(50);

        builder.setAutoReconnect(true);

        builder.setActivity(Activity.playing("BTE UK"));

        builder.addEventListeners(new DiscordChatListener(chat_channel, support_chat, staff_channel));
        builder.addEventListeners(new BotChatListener());
        builder.addEventListeners(new Playerlist());

        try {
            jda = builder.build();
            jda.awaitReady();

            chat = jda.getTextChannelById(chat_channel);
            supportInfo = jda.getTextChannelById(support_info);
            supportChat = jda.getTextChannelById(support_chat);
            staff = jda.getTextChannelById(staff_channel);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendConnectDisconnectMessage(String message, boolean connect) {

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

        chat.sendMessageEmbeds(eb.build()).queue((reply) -> {
            users.decrementAndGet();
        });

    }


    public void updateReviewerChannel() {

        //When a message is sent in the reviewer channel update the channel topic to the number of submitted plots.
        int plot_count = Proxy.getInstance().plotSQL.getInt("SELECT count(id) FROM plot_data WHERE status='submitted';");
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

    public void addRole(long user_id, long role_id) {
        try {
            //Get role.
            chat.getGuild().addRoleToMember(UserSnowflake.fromId(user_id), chat.getGuild().getRoleById(role_id)).queue();
        } catch (Exception e) {
            //An error occurred, the user or role is null, this is not necessarily a problem, but is being caught to prevent console spam.
        }
    }

    public void removeRole(long user_id, long role_id) {
        try {
            chat.getGuild().removeRoleFromMember(UserSnowflake.fromId(user_id), chat.getGuild().getRoleById(role_id)).queue();
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
}
