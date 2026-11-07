package com.kaloyandonev.serverFuserVelocity.Interconnect;

public final class MessageTypes {

    // [int length = 17][byte 0 = HANDSHAKE][8 bytes UUID MSB][8 bytes UUID LSB]
    public static final byte HANDSHAKE = 0;
    //[int length][byte CHAT][long uuidMSB][long uuidLSB][UTF senderName][UTF chatTypeKey][int jsonLen][UTF-8 bytes of the Component as JSON]
    public static final byte CHAT = 1;
    //[long uuidMSB][long uuidLSB][byte[32] sha256][int nbtLen][nbt bytes]
    public static final byte PLAYERDATA = 2;
    //[uuid][byte status]
    /*
        Byte status -
            00000000 - OK
            00000001 - Write Failed
            00000002 - Checksum Invalid
            00000003 - Invalid Player
     */
    public static final byte PLAYERDATAACK = 3;

    private MessageTypes() {}
}
