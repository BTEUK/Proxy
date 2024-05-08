package net.bteuk.proxy.socket;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.bteuk.proxy.socket.deserializers.ChatMessageDeserializer;
import net.bteuk.proxy.socket.serializers.ChatMessageSerializer;
import net.kyori.adventure.text.Component;

@JsonTypeName("CHAT_MESSAGE")
@JsonDeserialize(using = ChatMessageDeserializer.class)
@JsonSerialize(using = ChatMessageSerializer.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessage extends SocketObject {

    private String channel;

    private String sender;

    private Component component;

}