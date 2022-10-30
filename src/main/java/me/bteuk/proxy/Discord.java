package me.bteuk.proxy;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.bteuk.proxy.events.DiscordChatListener;
import me.bteuk.proxy.log4j.JdaFilter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;

public class Discord {

    private JDA jda;

    private String chat_channel;

    private JdaFilter jdaFilter;

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
        } catch (LoginException e) {
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public JDA getJda() {
        return jda;
    }
}
