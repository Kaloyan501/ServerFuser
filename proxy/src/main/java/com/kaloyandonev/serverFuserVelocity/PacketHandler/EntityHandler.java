package com.kaloyandonev.serverFuserVelocity.PacketHandler;

import com.kaloyandonev.serverFuserVelocity.Interconnect.ShardConnection;
import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.EntityData;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import com.kaloyandonev.serverfuser.Messages.TransferAck;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;

import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.CONNECTIONS;
import static com.kaloyandonev.serverFuserVelocity.Interconnect.TCP.forward;

public class EntityHandler {
    public static void handle(EntityData e, ShardConnection conn, byte[] body) throws IOException {
        ShardConnection target = CONNECTIONS.get(e.recieverUUID());

        System.out.println("send: nbtLen=" + e.nbt().length + " sha=" + HexFormat.of().formatHex(e.sha()));

        try {
            byte[] calcSha = MessageDigest.getInstance("SHA-256").digest(e.nbt());

            if (!MessageDigest.isEqual(calcSha, e.sha())) {
                conn.send(Packets.encode(new TransferAck(conn.shardId(), e.txID(), MessageTypes.STATUS_CHECKSUM_INVALID)));
                return;
            }

        } catch (Exception o) {
            o.printStackTrace();
            return;
        }

        ByteBuffer.wrap(body)
                .putLong(1, conn.shardId().getMostSignificantBits())
                .putLong(9, conn.shardId().getLeastSignificantBits());

        forward(conn, e.recieverUUID(), e.txID(), body);

        System.out.println(String.format("Got mob data for entityUUID: %s with length %d and sha %s", e.entityUUID().toString(), e.nbtLen(), HexFormat.of().formatHex(e.sha())));
        System.out.println(HexFormat.of().formatHex(e.nbt()));
    }
}
