package com.kaloyandonev.serverfuser.Messages;

import com.kaloyandonev.serverfuser.MessageTypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public record HandshakeResponce(String status) implements Packet {
    public HandshakeResponce {

    }
    public byte id() { return MessageTypes.HANDSHAKE_RESPONCE; }
    public void write(DataOutputStream o) throws IOException {
        o.writeUTF(status);
    }

    public static HandshakeResponce read(DataInputStream d) throws IOException {
        String status = d.readUTF();
        return new HandshakeResponce(status);
    }
}
