package com.kaloyandonev.serverfuser.Messages.Helpers;

import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * To use a Packet, you need a DataOutputStream connected to the shard you would like to send that data to.
 * To create a sendable Packet payload, you can do something like this:
 * {@snippet :
byte[] hs = Packets.encode(new Handshake(shardId, MessageTypes.PROTOCOL_VER, SharedConstants.getCurrentVersion().getDataVersion().getVersion(), modHashSet));
 * }
 * Packets.encode will turn the provided packet type and its parameters into a sendable payload.
 * Then to actually send the packet down the DataOutputStream, do the following:
 * {@snippet :
 *  out.writeInt(hs.length);
 *  out.write(hs);
 *  out.flush();
 * }
 * That will append the length of the packet at the start of it (the standard of the protocol), then append the actual payload and send it.
 * <p>
 * To read the Packet, do
 * {@snippet :
 * Handshake hs = Handshake.read(d);
 * }
 * Now that you have the packet, you can read the actual fields with
 * {@snippet :
 * hs.fieldName();
 * }
 */
public final class Packets {
    private Packets() {}

    public static byte[] encode(Packet p) throws IOException {
        var buf = new ByteArrayOutputStream();
        var o = new DataOutputStream(buf);
        o.writeByte(p.id());
        p.write(o);
        return buf.toByteArray();
    }

    public static Packet read(DataInputStream d) throws IOException {
        byte id = d.readByte();
        return switch (id) {
            case MessageTypes.HANDSHAKE -> Handshake.read(d);
            case MessageTypes.CHAT -> Chat.read(d);
            case MessageTypes.PLAYERDATA -> PlayerData.read(d);
            case MessageTypes.TRANSFERACK -> TransferAck.read(d);
            case MessageTypes.ENTITYDATA -> EntityData.read(d);
            case MessageTypes.HANDSHAKE_RESPONCE -> HandshakeResponce.read(d);
            default -> throw new IOException("unknown packet id " + id);
        };
    }
}
