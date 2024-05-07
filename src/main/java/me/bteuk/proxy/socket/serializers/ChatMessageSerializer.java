package me.bteuk.proxy.socket.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import me.bteuk.proxy.socket.ChatMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.units.qual.C;

import java.io.IOException;

public class ChatMessageSerializer extends StdSerializer<ChatMessage> {

    public ChatMessageSerializer() {
        this(null);
    }

    public ChatMessageSerializer(Class<ChatMessage> vc) {
        super(vc);
    }

    @Override
    public void serialize(ChatMessage chatMessage, JsonGenerator generator, SerializerProvider serializerProvider) throws IOException {

        generator.writeStartObject();
        generator.writeStringField("channel", chatMessage.getChannel());
        generator.writeStringField("sender", chatMessage.getSender());
        generator.writeStringField("component", GsonComponentSerializer.gson().serialize(chatMessage.getComponent()));
    }
}
