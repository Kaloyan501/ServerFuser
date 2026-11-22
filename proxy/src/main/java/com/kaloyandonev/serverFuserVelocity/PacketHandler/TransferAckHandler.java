package com.kaloyandonev.serverFuserVelocity.PacketHandler;

import com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants;
import com.kaloyandonev.serverFuserVelocity.Interconnect.ShardConnection;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import com.kaloyandonev.serverfuser.Messages.TransferAck;

import java.io.IOException;

import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.CONNECTIONS;
import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.PENDING;

public class TransferAckHandler {
    public static void handle(TransferAck a, ShardConnection conn) throws IOException {
        var key = new InterconnectRuntimeConstants.TxKey(a.senderUUID(), a.txId());
        InterconnectRuntimeConstants.Pending pend = PENDING.get(key);
        if (pend == null || !pend.target().equals(conn.shardId())) return;
        if (!PENDING.remove(key, pend)) return;
        ShardConnection origin = CONNECTIONS.get(a.senderUUID());
        if (origin != null) origin.send(Packets.encode(a));
    }
}
