package me.bteuk.proxy;

import me.bteuk.proxy.events.BotChatListener;
import me.bteuk.proxy.events.DiscordChatListener;
import me.bteuk.proxy.log4j.JdaFilter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Discord {

    private JDA jda;

    private JdaFilter jdaFilter;

    private TextChannel chat;
    private TextChannel staff;
    private TextChannel reviewer;

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
        String reviewer_channel = Proxy.getInstance().getConfig().getString("chat.reviewer");
        String staff_channel = Proxy.getInstance().getConfig().getString("chat.staff");

        //Create JDABuilder.
        JDABuilder builder = JDABuilder.createDefault(token);

        builder.setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER));
        builder.setChunkingFilter(ChunkingFilter.NONE);
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.setLargeThreshold(50);

        builder.setAutoReconnect(true);

        builder.setActivity(Activity.playing("BTE UK"));

        builder.addEventListeners(new DiscordChatListener(chat_channel, reviewer_channel, staff_channel));
        builder.addEventListeners(new BotChatListener());

        try {
            jda = builder.build();
            jda.awaitReady();

            chat = jda.getTextChannelById(chat_channel);
            reviewer = jda.getTextChannelById(reviewer_channel);
            staff = jda.getTextChannelById(staff_channel);

        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String channel, byte[] message) throws IOException {

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
        String sMessage = in.readUTF();

        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}|&[a-fA-F0-9]|&k|&l|&m|&n|&o|&r");

        Matcher matcher = pattern.matcher(sMessage);
        sMessage = matcher.replaceAll("");

        //TODO: could consider applying bold, italic and other formatting to discord messages if used in Minecraft.

        //If chat channel is global send it.
        if (channel.equalsIgnoreCase("uknet:globalchat")) {

            chat.sendMessage(sMessage).queue();

        } else if (channel.equalsIgnoreCase("uknet:connect")) {
            //If channel is connect send connect message using embed.
            EmbedBuilder eb = new EmbedBuilder();
            eb.setDescription("**" + sMessage + "**");
            eb.setColor(Color.GREEN);
            chat.sendMessage(eb.build()).queue();

        } else if (channel.equalsIgnoreCase("uknet:disconnect")) {
            //If channel is connect send disconnect message using embed.
            EmbedBuilder eb = new EmbedBuilder();
            eb.setDescription("**" + sMessage + "**");
            eb.setColor(Color.RED);
            chat.sendMessage(eb.build()).queue();
        } else if (channel.equalsIgnoreCase("uknet:reviewer")) {

            //If plot is submitted, update the channel description.
            if ("plot submitted message".equalsIgnoreCase(sMessage)) {
                chat.getManager().setTopic("Updated channel topic!");
            }


        } else if (channel.equalsIgnoreCase("uknet:staff")) {
            //Send message to staff channel.
            staff.sendMessage(sMessage).queue();
        }

    }

    public void addRole(long user_id, long role_id) {
        try {
            //Get role.
            chat.getGuild().addRoleToMember(user_id, chat.getGuild().getRoleById(role_id));
        } catch (Exception e) {
            //An error occurred, the user or role is null, this is not necessarily a problem, but is being caught to prevent console spam.
        }
    }

    public void removeRole(long user_id, long role_id) {
        try {
            chat.getGuild().removeRoleFromMember(user_id, chat.getGuild().getRoleById(role_id));
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
}
