package com.kaloyandonev.serverfuser.Messages;

import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.Helpers.Wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record Chat(UUID senderUUID, String senderName, String chatTypeKey, String chatComponentJSON) implements Packet {

    /**
     * Implementation of the Chat ServerFuser packet type. See Javadoc on Packets for more info about how to read/write a packet.
     * @param senderUUID UUID of the player that sent the message
     * @param senderName The name of the player that sent the message
     * @param chatTypeKey Registry key of the message's ChatType (e.g "minecraft:chat", "minecraft:say_command")
     * @param chatComponentJSON The Component object for the message serialized down to JSON
     */
    public Chat{

    }

    public byte id() { return MessageTypes.CHAT; }

    public void write(DataOutputStream o) throws IOException {
        Wire.writeUuid(o, senderUUID);
        o.writeUTF(senderName);
        o.writeUTF(chatTypeKey);
        Wire.writeBlob(o, chatComponentJSON.getBytes(StandardCharsets.UTF_8));
    }

    public static Chat read(DataInputStream d) throws IOException {
        UUID senderUUID = Wire.readUuid(d);
        String senderName = d.readUTF();
        String chatTypeKey = d.readUTF();
        String json = new String(Wire.readBlob(d, 1_000_000), StandardCharsets.UTF_8);
        return new Chat(senderUUID, senderName, chatTypeKey, json);
    }
}
