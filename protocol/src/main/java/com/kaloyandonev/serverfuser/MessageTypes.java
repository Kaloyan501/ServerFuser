package com.kaloyandonev.serverfuser;

public final class MessageTypes {

    public static final int PROTOCOL_VER = 1;

    public static final byte HANDSHAKE = 0;
    public static final byte CHAT = 1;
    public static final byte PLAYERDATA = 2;
    //[uuid][byte status]
    public static final byte PLAYERDATAACK = 3;

    //PLAYERDATAACK statuses
    public static final byte PLAYER_STATUS_OK = 0;
    public static final byte PLAYER_STATUS_WRITE_FAILED = 1;
    public static final byte PLAYER_STATUS_CHECKSUM_INVALID = 2;
    public static final byte PLAYER_STATUS_INVALID_PLAYER = 3;

    public static final byte ENTITYDATA = 4;

    //[UTF-8 status]
    public static final byte HANDSHAKE_RESPONCE = 5;
    public static final String HS_OK = "OK";
    public static final String HS_PROTOCOLVER_MISMATCH = "PROTOCOLVER_MISMATCH";
    public static final String HS_DATAVER_MISMATCH = "DATAVER_MISMATCH";
    public static final String HS_MODHASHSET_MISMATCH = "MODHASHSET_MISMATCH";

    private MessageTypes() {}
}