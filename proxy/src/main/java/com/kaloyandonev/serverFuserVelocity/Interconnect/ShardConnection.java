package com.kaloyandonev.serverFuserVelocity.Interconnect;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public final class ShardConnection {
    private final UUID shardId;
    private final Socket socket;
    private final DataOutputStream out;

    public ShardConnection(UUID shardId, Socket socket) throws IOException {
        this.shardId = shardId;
        this.socket = socket;
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public UUID shardId() { return shardId; }

    /** Frames and sends one packet body. Safe to call from any thread. */
    public synchronized void send(byte[] body) throws IOException {
        out.writeInt(body.length);
        out.write(body);
        out.flush();
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) { }
    }
}
