package me.bteuk.proxy;

import me.bteuk.proxy.events.DiscordChatListener;
import me.bteuk.proxy.log4j.JdaFilter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
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

    private String chat_channel;

    private JdaFilter jdaFilter;

    private TextChannel chat;

    public Discord() {

        // add log4j filter for JDA messages
        if (jdaFilter == null) {
            try {
                Class<?> jdaFilterClass = Class.forName("me.bteuk.proxy.log4j.JdaFilter");
                jdaFilter = (JdaFilter) jdaFilterClass.newInstance();
                ((org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger()).addFilter((org.apache.logging.log4j.core.Filter) jdaFilter);
                Proxy.getInstance().getLogger().debug("JdaFilter applied");
            } catch (Exception e) {
                Proxy.getInstance().getLogger().error("Failed to attach JDA message filter to root logger", e);
            }
        }

        //Get token from config.
        String token = Proxy.getInstance().getConfig().getString("token");
        chat_channel = Proxy.getInstance().getConfig().getString("chat_channel");

        //Create JDABuilder.
        JDABuilder builder = JDABuilder.createDefault(token);

        builder.setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER));
        builder.setChunkingFilter(ChunkingFilter.NONE);
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.setLargeThreshold(50);

        builder.setAutoReconnect(true);

        builder.setActivity(Activity.playing("BTE UK"));

        builder.addEventListeners(new DiscordChatListener(chat_channel));

        try {
            jda = builder.build();
            jda.awaitReady();

            chat = jda.getTextChannelById(chat_channel);
        } catch (LoginException e) {
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void SendMessage(String channel, byte[] message) throws IOException {

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
            eb.setTitle(sMessage, null);
            eb.setColor(Color.GREEN);
            chat.sendMessage(eb.build()).queue();

        } else if (channel.equalsIgnoreCase("uknet:disconnect")) {
            //If channel is connect send disconnect message using embed.
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(sMessage, null);
            eb.setColor(Color.RED);
            chat.sendMessage(eb.build()).queue();
        }

    }

    public JDA getJda() {
        return jda;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }
}
