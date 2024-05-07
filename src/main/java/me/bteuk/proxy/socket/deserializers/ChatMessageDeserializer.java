package me.bteuk.proxy.socket.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import me.bteuk.proxy.socket.ChatMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.IOException;

public class ChatMessageDeserializer extends StdDeserializer<ChatMessage> {

    public ChatMessageDeserializer() {
        this(null);
    }

    public ChatMessageDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ChatMessage deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        String channel = node.get("channel").asText();
        String sender = node.get("sender").asText();
        Component component = GsonComponentSerializer.gson().deserialize(node.get("component").asText());

        return new ChatMessage(channel, sender, component);
    }
}
