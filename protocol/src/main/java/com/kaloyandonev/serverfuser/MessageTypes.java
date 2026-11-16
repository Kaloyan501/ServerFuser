package com.kaloyandonev.serverfuser;

public final class MessageTypes {

    public static final int PROTOCOL_VER = 1;

    public static final byte HANDSHAKE = 0;
    public static final byte CHAT = 1;
    public static final byte PLAYERDATA = 2;
    //[uuid][long txId][byte status]
    public static final byte TRANSFERACK = 3;

    //PLAYERDATAACK statuses
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_WRITE_FAILED = 1;
    public static final byte STATUS_CHECKSUM_INVALID = 2;
    public static final byte STATUS_INVALID_PLAYER = 3;
    public static final byte STATUS_TARGET_OFFLINE = 4;
    public static final byte STATUS_NO_SERVER = 5;
    public static final byte STATUS_BAD_ENTITY = 6;
    public static final byte STATUS_DUPLICATE_UUID = 7;
    public static final byte STATUS_EXCEPTION = 8;
    public static final byte STATUS_TIMEOUT = 9;

    public static final byte ENTITYDATA = 4;

    //[UTF-8 status]
    public static final byte HANDSHAKE_RESPONCE = 5;
    public static final String HS_OK = "OK";
    public static final String HS_PROTOCOLVER_MISMATCH = "PROTOCOLVER_MISMATCH";
    public static final String HS_DATAVER_MISMATCH = "DATAVER_MISMATCH";
    public static final String HS_MODHASHSET_MISMATCH = "MODHASHSET_MISMATCH";

    private MessageTypes() {}

    public static String statusName(byte s) {
        return switch (s) {
            case STATUS_OK               -> "OK";
            case STATUS_WRITE_FAILED     -> "WRITE_FAILED";
            case STATUS_CHECKSUM_INVALID -> "CHECKSUM_INVALID";
            case STATUS_INVALID_PLAYER   -> "INVALID_PLAYER";
            case STATUS_TARGET_OFFLINE   -> "TARGET_OFFLINE";
            case STATUS_NO_SERVER        -> "NO_SERVER";
            case STATUS_BAD_ENTITY       -> "BAD_ENTITY";
            case STATUS_DUPLICATE_UUID   -> "DUPLICATE_UUID";
            case STATUS_EXCEPTION        -> "EXCEPTION";
            case STATUS_TIMEOUT          -> "TIMEOUT";
            default                      -> "UNKNOWN(" + s + ")";
        };
    }
}