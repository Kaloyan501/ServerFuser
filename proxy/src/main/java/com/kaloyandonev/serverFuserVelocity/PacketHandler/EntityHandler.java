package com.kaloyandonev.serverFuserVelocity.PacketHandler;

import com.kaloyandonev.serverFuserVelocity.Interconnect.ShardConnection;
import com.kaloyandonev.serverFuserVelocity.ServerFuserVelocity;
import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.EntityData;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import com.kaloyandonev.serverfuser.Messages.TransferAck;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;

import static com.kaloyandonev.serverFuserVelocity.TransferRouter.forward;

public class EntityHandler {
    public static void handle(EntityData e, ShardConnection conn, byte[] body) throws IOException {

        ServerFuserVelocity.logger.info("send: nbtLen={} sha={}", e.nbt().length, HexFormat.of().formatHex(e.sha()));

        try {
            byte[] calcSha = MessageDigest.getInstance("SHA-256").digest(e.nbt());

            if (!MessageDigest.isEqual(calcSha, e.sha())) {
                conn.send(Packets.encode(new TransferAck(conn.shardId(), e.txID(), MessageTypes.STATUS_CHECKSUM_INVALID)));
                return;
            }

        } catch (Exception o) {
            ServerFuserVelocity.logger.error(o.toString());
            return;
        }

        ByteBuffer.wrap(body)
                .putLong(1, conn.shardId().getMostSignificantBits())
                .putLong(9, conn.shardId().getLeastSignificantBits());

        forward(conn, e.recieverUUID(), e.txID(), body);

        ServerFuserVelocity.logger.info("Got mob data for entityUUID: {} with length {} and sha {}\n", e.entityUUID().toString(), e.nbtLen(), HexFormat.of().formatHex(e.sha()));
        ServerFuserVelocity.logger.info(HexFormat.of().formatHex(e.nbt()));
    }
}
