package com.kaloyandonev.serverFuserVelocity;

import com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants;
import com.kaloyandonev.serverFuserVelocity.Interconnect.ShardConnection;
import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import com.kaloyandonev.serverfuser.Messages.TransferAck;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.CONNECTIONS;
import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.PENDING;

public class TransferRouter {
    public static void forward(ShardConnection from, UUID receiver, long txId, byte[] body) throws IOException {
        ShardConnection target = CONNECTIONS.get(receiver);
        if (target == null || receiver.equals(from.shardId())) {
            from.send(Packets.encode(new TransferAck(from.shardId(), txId, MessageTypes.STATUS_TARGET_OFFLINE)));
            return;
        }
        ByteBuffer.wrap(body)                       // overwrite the sender slot with the real sender
                .putLong(1, from.shardId().getMostSignificantBits())
                .putLong(9, from.shardId().getLeastSignificantBits());

        var key = new InterconnectRuntimeConstants.TxKey(from.shardId(), txId);
        PENDING.put(key, new InterconnectRuntimeConstants.Pending(receiver, System.currentTimeMillis() + 30_000));
        try {
            target.send(body);
        } catch (IOException e) {
            PENDING.remove(key);
            from.send(Packets.encode(new TransferAck(from.shardId(), txId, MessageTypes.STATUS_TARGET_OFFLINE)));
        }
    }

    static void startSweeper() {
        var ses = Executors.newSingleThreadScheduledExecutor(r -> { var t = new Thread(r, "tx-sweeper"); t.setDaemon(true); return t; });
        ses.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            PENDING.forEach((k, v) -> {
                if (v.expiresAtMs() < now && PENDING.remove(k, v)) failToSender(k, MessageTypes.STATUS_TIMEOUT);
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static void failToSender(InterconnectRuntimeConstants.TxKey k, byte status) {
        ShardConnection s = CONNECTIONS.get(k.sender());
        if (s != null) try { s.send(Packets.encode(new TransferAck(k.sender(), k.txId(), status))); } catch (IOException ignored) {}
    }
}
