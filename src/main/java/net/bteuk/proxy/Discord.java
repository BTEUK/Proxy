package net.bteuk.proxy;

import com.google.protobuf.Field;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import lombok.Setter;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DiscordDirectMessage;
import net.bteuk.network.lib.dto.DiscordEmbed;
import net.bteuk.network.lib.dto.DiscordLinking;
import net.bteuk.network.lib.dto.DiscordRole;
import net.bteuk.proxy.commands.CommandManager;
import net.bteuk.proxy.events.BotChatListener;
import net.bteuk.proxy.events.DiscordChatListener;
import net.bteuk.proxy.log4j.JdaFilter;
import net.bteuk.proxy.sql.PlotSQL;
import net.bteuk.proxy.utils.Linked;
import net.bteuk.proxy.utils.UnknownUserErrorHandler;
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
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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

    @Setter
    @Getter
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

    /**
     * Handle a {@link ChatMessage}.
     *
     * @param message the message to handle
     */
    public void handle(ChatMessage message) {

        // Format the message according to the formatting rules.
        String text = format(message.getComponent());
        message.getComponent().style();

        // Send the message to the relevent channel.
        switch (message.getChannel()) {

            case "global" -> chat.sendMessage(text).queue();
            case "staff" -> staff.sendMessage(text).queue();

            // Ignore chat message in all other channels, they are not intended to be posted on discord.
        }
    }

    /**
     * Handle a {@link DiscordEmbed}
     *
     * @param embed the embed to handle
     */
    public void handle(DiscordEmbed embed) {

        // Create the embed from the transfer object.
        chat.sendMessageEmbeds(createEmbed(embed)).queue();

    }

    /**
     * Handle a {@link DiscordRole}
     *
     * @param role the role to add/remove
     */
    public void handle(DiscordRole role) {

        if (role.getUuid() == null || role.getRole() == null) {
            return;
        }

        // Get the user, cancel if not exists.
        long userId = Proxy.getInstance().getGlobalSQL().getLong("SELECT discord_id FROM discord WHERE uuid='" + role.getUuid() + "';");
        if (userId == 0) {
            return;
        }

        // Get the role, cancel if not exists.
        long roleId = Proxy.getInstance().getConfig().getLong("discord_roles." + role.getRole());
        if (roleId == 0) {
            return;
        }

        if (role.isAddRole()) {
            addRole(userId, roleId, true);
        } else {
            removeRole(userId, roleId, true);
        }
    }

    /**
     * Handle a {@link DiscordDirectMessage}
     *
     * @param directMessage the direct message
     */
    public void handle(DiscordDirectMessage directMessage) {

        if (directMessage.getRecipient() == null || directMessage.getMessage() == null) {
            return;
        }

        // Get the user, cancel if not exists.
        long userId = Proxy.getInstance().getGlobalSQL().getLong("SELECT discord_id FROM discord WHERE uuid='" + directMessage.getRecipient() + "';");
        if (userId == 0) {
            return;
        }

        // Send direct message to the user.
        sendDirectMessage(userId, directMessage.getMessage());

    }

    /**
     * Handle a {@link DiscordLinking}
     *
     * @param discordLinking the discord linking event to handle.
     */
    public void handle(DiscordLinking discordLinking) {

        if (discordLinking.getUuid() == null) {
            return;
        }

        if (discordLinking.isUnlink()) {
            unlinkUser(discordLinking.getUuid());
        } else {
            if (discordLinking.getToken() == null) {
                return;
            }

            // Add object for linking, with a time to remove.
            // If there is already an instance, replace it.
            Linked linked = null;
            for (Linked l : Proxy.getInstance().getLinking()) {
                if (l.uuid.equalsIgnoreCase(discordLinking.getUuid())) {
                    linked = l;
                }
            }

            //If there was already a task for this player, close it first.
            if (linked != null) {
                linked.close();
                Proxy.getInstance().getLinking().remove(linked);
            }

            //Create new link.
            Proxy.getInstance().getLinking().add(new Linked(discordLinking.getUuid(), discordLinking.getToken()));
        }

    }

    private MessageEmbed createEmbed(DiscordEmbed embed) {

        EmbedBuilder builder = new EmbedBuilder();

        // Add all non-null fields from the transfer object to the embed.

        if (embed.getTitle() != null) {
            builder.setTitle(embed.getTitle());
        }

        if (embed.getAuthor() != null || embed.getIcon() != null) {
            builder.setAuthor(embed.getAuthor(), null, embed.getIcon());
        }

        if (embed.getDescription() != null) {
            builder.setDescription(embed.getDescription());
        }

        if (embed.getFields() != null) {
            embed.getFields().forEach(field ->
                    builder.addField(new MessageEmbed.Field(field.getName(), field.getValue(), field.isInline())));
        }

        if (embed.getFooter() != null) {
            builder.setFooter(embed.getFooter());
        }

        // -1 is the default colour.
        if (embed.getColour() != -1) {
            builder.setColor(embed.getColour());
        }

        return builder.build();
    }

    public void unlinkUser(String uuid) {
        //Remove the user from the discord link table.
        Proxy.getInstance().getGlobalSQL().update("DELETE FROM discord WHERE uuid=" + uuid + ";");
    }

    public static void unlinkUser(long userID) {
        //Remove the user from the discord link table.
        if (Proxy.getInstance().getGlobalSQL().hasRow("SELECT discord_id FROM discord WHERE discord_id='" + userID + "';")) {
            Proxy.getInstance().getGlobalSQL().update("DELETE FROM discord WHERE discord_id=" + userID + ";");
            Proxy.getInstance().getLogger().info(("Removed discord link for " + userID + ", they are no longer in the discord server."));
        }
    }

    private void sendDirectMessage(long userId, String message) {
        jda.retrieveUserById(userId).queue(user -> {
            //Open a private channel with the user and send the message.
            user.openPrivateChannel().queue(channel -> channel.sendMessage(messageLimit(message)).queue());
        });
    }

    /**
     * Sends an embed and decrement the integer.
     *
     * @param embed the embed to send
     * @param users the integer to decrease
     */
    public void sendBlockingEmbed(MessageEmbed embed, AtomicInteger users) {

        chat.sendMessageEmbeds(embed).queue((reply) -> users.decrementAndGet());

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
            book_id = plotSQL.getInt("SELECT book_id FROM deny_data WHERE id=" + Integer.parseInt(params[2]) + " AND uuid='" + params[0] + "' ORDER BY attempt DESC;");
        }

        if (book_id != 0) {
            //Add feedback to the message.
            ArrayList<String> pages = plotSQL.getStringList("SELECT contents FROM book_data WHERE id=" + book_id + " ORDER BY page ASC;");

            builder.append("\n").append("Feedback: ").append(String.join(" ", pages));
        }

        String message = escapeDiscordFormatting(builder.toString());

        //Cut the message off at 2000 characters.
        message = messageLimit(message);

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

    public TextChannel getSupportInfoChannel() {
        return supportInfo;
    }

    public TextChannel getSupportChatChannel() {
        return supportChat;
    }

    public String getReviewerRoleID() {
        return reviewer;
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

    private static String format(Component component) {

        // Format each section of the component individually.
        StringBuilder builder = new StringBuilder();

        if (component instanceof TextComponent textComponent) {
            builder.append(format(textComponent));
            textComponent.children().forEach(
                    child -> builder.append(format(child))
            );
        }

        return messageLimit(builder.toString());
    }

    private static String format(TextComponent component) {

        String text = PlainTextComponentSerializer.plainText().serialize(component);

        // Escape all potential discord markdown of plaintext.
        text = escapeDiscordFormatting(text);

        // Apply bold, italic and underline from the component.
        if (component.hasDecoration(TextDecoration.ITALIC)) {
            text = italic(text);
        }
        if (component.hasDecoration(TextDecoration.BOLD)) {
            text = bold(text);
        }
        if (component.hasDecoration(TextDecoration.UNDERLINED)) {
            text = underline(text);
        }

        return text;
    }

    private static String escapeDiscordFormatting(String message) {
        return message.replace("@", "@\u200B")
                .replaceAll("[*_#\\[\\]()\\-`>]", "\\\\$0");
    }

    private static String italic(String message) {
        return String.format("*%s*", message);
    }

    private static String bold(String message) {
        return String.format("**%s**", message);
    }

    private static String underline(String message) {
        return String.format("__%s__", message);
    }

    private static String messageLimit(String message) {
        return message.substring(0, 1997) + "...";
    }
}
