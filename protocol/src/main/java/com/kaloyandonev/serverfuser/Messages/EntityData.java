package com.kaloyandonev.serverfuser.Messages;

import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.Helpers.Wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public record EntityData(UUID senderUUID, UUID recieverUUID, long txID, UUID entityUUID, byte[] sha, int nbtLen, byte[] nbt) implements Packet {
    /**
     * Implementation of the EntityData ServerFuser packet type. This packet is used when transferring a mob between servers.
     * See Javadoc on Packets for more info about how to read/write a packet.
     * @param senderUUID UUID of the shard sending the mob
     * @param recieverUUID UUID of the shard receiving the mob
     * @param txID Unique transmit ID for tracking if the mob transfer has completed successfully.
     * @param entityUUID UUID of the entity
     * @param sha SHA256 of the NBT
     * @param nbtLen Length of the NBT
     * @param nbt Byte array of the NBT
     */
    public EntityData {
        if (sha.length != 32) throw new IllegalArgumentException("sha must be 32 bytes");
    }

    public byte id() { return MessageTypes.ENTITYDATA; }
    public void write(DataOutputStream o) throws IOException {
        Wire.writeUuid(o, senderUUID);
        Wire.writeUuid(o, recieverUUID);
        o.writeLong(txID);
        Wire.writeUuid(o, entityUUID);
        o.write(sha);
        o.writeInt(nbtLen);
        o.write(nbt);
    }

    public static EntityData read(DataInputStream d) throws IOException {
        UUID senderUUID = Wire.readUuid(d);
        UUID recieverUUID = Wire.readUuid(d);
        long txID = d.readLong();
        UUID entityUUID = Wire.readUuid(d);
        byte[] sha = new byte[32];
        d.readFully(sha);
        int nbtLen = d.readInt();
        byte[] nbt = new byte[nbtLen];
        d.readFully(nbt);
        return new EntityData(senderUUID, recieverUUID, txID, entityUUID, sha, nbtLen, nbt);
    }
}
