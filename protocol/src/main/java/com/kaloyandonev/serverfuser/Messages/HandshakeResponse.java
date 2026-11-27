package com.kaloyandonev.serverfuser.Messages;

import com.kaloyandonev.serverfuser.MessageTypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public record HandshakeResponse(String status) implements Packet {
    /**
     * Implementation of the HandshakeResponse ServerFuser packet type. See Javadoc on Packets for more info about how to read/write a packet.
     * @param status - status of the handshake
     */
    public HandshakeResponse {

    }
    public byte id() { return MessageTypes.HANDSHAKE_RESPONCE; }
    public void write(DataOutputStream o) throws IOException {
        o.writeUTF(status);
    }

    public static HandshakeResponse read(DataInputStream d) throws IOException {
        String status = d.readUTF();
        return new HandshakeResponse(status);
    }
}
