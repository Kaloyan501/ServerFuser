package com.kaloyandonev.serverfuser;

public final class MessageTypes {

    // [int length = 17][byte 0 = HANDSHAKE][8 bytes UUID MSB][8 bytes UUID LSB]
    public static final byte HANDSHAKE = 0;
    //[int length][byte CHAT][long uuidMSB][long uuidLSB][UTF senderName][UTF chatTypeKey][int jsonLen][UTF-8 bytes of the Component as JSON]
    public static final byte CHAT = 1;
    //[long uuidMSB][long uuidLSB][byte[32] sha256][int nbtLen][nbt bytes]
    public static final byte PLAYERDATA = 2;
    //[uuid][byte status]
    public static final byte PLAYERDATAACK = 3;

    //PLAYERDATAACK statuses
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_WRITE_FAILED = 1;
    public static final byte STATUS_CHECKSUM_INVALID = 2;
    public static final byte STATUS_INVALID_PLAYER = 3;

    private MessageTypes() {}
}
