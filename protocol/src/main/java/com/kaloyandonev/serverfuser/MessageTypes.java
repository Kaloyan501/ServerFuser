package com.kaloyandonev.serverfuser;

public final class MessageTypes {

    public static final int PROTOCOL_VER = 1;

    // [int length = 17][byte 0 = HANDSHAKE][8 bytes UUID MSB][8 bytes UUID LSB][int protocolVer][int dataVer][byte[32] modHashSet]
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

    //[long uuidMSB][long uuidLSB][byte[32] sha256][int nbtLen][nbt bytes]
    public static final byte ENTITYDATA = 4;

    //[UTF-8 status]
    public static final byte HANDSHAKE_RESPONCE = 5;
    public static final String HS_OK = "OK";
    public static final String HS_PROTOCOLVER_MISMATCH = "PROTOCOLVER_MISMATCH";
    public static final String HS_DATAVER_MISMATCH = "DATAVER_MISMATCH";
    public static final String HS_MODHASHSET_MISMATCH = "MODHASHSET_MISMATCH";

    private MessageTypes() {}
}