package com.kaloyandonev.serverfuser.Messages;

import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.Helpers.Wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;


public record Handshake(UUID shardUUID, int protocolVer, int dataVer, byte[] modHashSet) implements Packet {

    /**
     * Implementation of the Handshake ServerFuser packet type. See Javadoc on Packets for more info about how to read/write a packet.
     * @param shardUUID UUID of the shard trying to handshake with the interconnect
     * @param protocolVer ServerFuser protocol version the shard is using
     * @param dataVer The Minecraft data version the shard is using
     * @param modHashSet A combined hash of all the mods installed on the server. WARNING: Mods loaded via classPath are not considered and may cause weird issues.
     */
    public Handshake {
        if (modHashSet.length != 32) throw new IllegalArgumentException("modHashSet must be 32 bytes");
    }

    public byte id() { return MessageTypes.HANDSHAKE; }

    public void write(DataOutputStream o) throws IOException {
        Wire.writeUuid(o, shardUUID);
        o.writeInt(protocolVer);
        o.writeInt(dataVer);
        o.write(modHashSet);
    }

    public static Handshake read(DataInputStream d) throws IOException {
        UUID id = Wire.readUuid(d);
        int proto = d.readInt();
        int dataVer = d.readInt();
        byte[] hash = new byte[32];
        d.readFully(hash);
        return new Handshake(id, proto, dataVer, hash);
    }
}