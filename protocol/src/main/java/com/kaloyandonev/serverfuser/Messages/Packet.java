package com.kaloyandonev.serverfuser.Messages;

import java.io.DataOutputStream;
import java.io.IOException;


public sealed interface Packet permits Handshake, Chat, EntityData, PlayerData, HandshakeResponce, TransferAck {
    byte id();
    void write(DataOutputStream o) throws IOException;
}
