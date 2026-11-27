package com.kaloyandonev.serverfuser.Messages;

import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.Helpers.Wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public record TransferAck(UUID senderUUID, long txId, byte status) implements Packet {

    /**
     * Implementation for the TransferAck ServerFuser packet type. Used to ack a transfer of a player/entity.
     * See Javadoc on Packets for more info about how to read/write a packet.
     * @param senderUUID UUID of the sender of the original message being acknowledged.
     * @param txId Unique ID for each transmission by a shard
     * @param status Status of the transfer
     */
    public TransferAck{

    }

    public byte id() { return MessageTypes.TRANSFERACK; }

    public void write(DataOutputStream o) throws IOException {
        Wire.writeUuid(o, senderUUID);
        o.writeLong(txId);
        o.write(status);
    }

    public static TransferAck read(DataInputStream d) throws IOException {
        UUID senderUUID = Wire.readUuid(d);
        long txId = d.readLong();
        byte status = d.readByte();

        return new TransferAck(senderUUID, txId, status);
    }
}
