package net.bteuk.proxy;

import lombok.Getter;
import lombok.Setter;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DiscordDirectMessage;
import net.bteuk.network.lib.dto.DiscordEmbed;
import net.bteuk.network.lib.dto.DiscordLinking;
import net.bteuk.network.lib.dto.DiscordRole;
import net.bteuk.proxy.chat.ChatHandler;
import net.bteuk.proxy.commands.CommandManager;
import net.bteuk.proxy.eventing.jda.BotChatListener;
import net.bteuk.proxy.eventing.jda.DiscordChatListener;
import net.bteuk.proxy.log4j.JdaFilter;
import net.bteuk.proxy.utils.Avatar;
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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Discord {

    @Setter
    @Getter
    private JDA jda;

    private TextChannel chat;
    private TextChannel staff;
    private TextChannel supportInfo;
    private TextChannel supportChat;
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
            Proxy.getInstance().getLogger().error("An error occurred while loading discord!");
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

        // Send the message to the relevent channel.
        switch (message.getChannel()) {

            case "global" -> chat.sendMessage(text).queue();
            case "staff" -> staff.sendMessage(staffMessage(text)).queue();

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
        long userId = getRoleID(role.getRole());
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
        Proxy.getInstance().getGlobalSQL().update("DELETE FROM discord WHERE uuid='" + uuid + "';");
        Proxy.getInstance().getLogger().info(String.format("Unlinked user with uuid %s", uuid));
    }

    public static void unlinkUser(long userId) {
        //Remove the user from the discord link table.
        Proxy.getInstance().getGlobalSQL().update("DELETE FROM discord WHERE discord_id=" + userId + ";");
        Proxy.getInstance().getLogger().info(("Removed discord link for " + userId + ", they are no longer in the discord server."));

        // Send an unlink message to the servers to make sure it's also unlinked there.
        DiscordLinking discordLinking = new DiscordLinking();
        discordLinking.setDiscordId(userId);
        discordLinking.setUnlink(true);
        try {
            ChatHandler.handle(discordLinking);
        } catch (IOException e) {
            // Ignored
        }

    }

    private void sendDirectMessage(long userId, String message) {
        jda.retrieveUserById(userId).queue(user -> {
            //Open a private channel with the user and send the message.
            user.openPrivateChannel().queue(channel -> channel.sendMessage(messageLimit(message)).queue());
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

    /**
     * Sends an embed for a player join/leaving the server
     * @param message the message to format
     * @param name the name of the player
     * @param uuid the uuid of the player
     * @param playerSkin the player skin
     * @param consumer to run after success
     */
    public void sendConnectEmbed(String message, String name, String uuid, String playerSkin, Color colour, Consumer<Message> consumer) {
        MessageEmbed embed = createAuthorEmbed(message.replace("%player%", name), null, Avatar.getAvatarUrl(uuid, playerSkin), colour);
        sendEmbed(embed, consumer);
    }

    private void sendEmbed(MessageEmbed embed, Consumer<Message> consumer) {
        chat.sendMessageEmbeds(embed).queue(consumer);
    }

    /**
     * Create an embed with an author as message with an icon.
     *
     * @param author author
     * @param iconUrl icon url
     * @return the {@link MessageEmbed}
     */
    public MessageEmbed createAuthorEmbed(String author, String url, String iconUrl, Color colour) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(author, url, iconUrl);
        builder.setColor(colour);
        return builder.build();
    }

    public TextChannel getSupportInfoChannel() {
        return supportInfo;
    }

    public TextChannel getSupportChatChannel() {
        return supportChat;
    }

    public String getReviewerRoleID() {
        return String.valueOf(getRoleID("reviewer"));
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

    /**
     * Remove the [Staff] prefix from staff-messages.
     *
     * @param message the message with prefix
     * @return the message without prefix
     */
    private static String staffMessage(String message) {
        if (message.startsWith("\\[Staff\\]")) {
            message = message.substring(9);
        }
        return message;
    }

    public static String escapeDiscordFormatting(String message) {
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
        if (message.length() > 2000) {
            message = message.substring(0, 1997) + "...";
        }
        return message;
    }

    private long getRoleID(String role) {
        return Proxy.getInstance().getConfig().getLong("discord_roles." + role);
    }
}
