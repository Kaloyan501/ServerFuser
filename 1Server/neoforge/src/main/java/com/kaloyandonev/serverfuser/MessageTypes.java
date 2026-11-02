package com.kaloyandonev.serverfuser;

public final class MessageTypes {

    // HANDSHAKE IS [0][SHARD UUID]
    public static final byte HANDSHAKE = 0;
    public static final byte CHAT = 1;
    public static final byte PLAYERDATA = 2;

    private MessageTypes() {}
}
