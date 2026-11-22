package com.kaloyandonev.serverfuser.Interconnect;

import com.kaloyandonev.serverfuser.Constants;
import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.*;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import com.kaloyandonev.serverfuser.Transfers;
import com.kaloyandonev.serverfuser.config.Config;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class InterconnectClient {
    private final String host;
    private final int port;
    private static final BlockingQueue<byte[]> outbox = new LinkedBlockingQueue<>(1024); // bounded
    private volatile boolean running = true;
    private Thread thread;

    private static final java.util.concurrent.atomic.AtomicInteger IDS =
            new java.util.concurrent.atomic.AtomicInteger();
    private final int id = IDS.incrementAndGet();
    private final UUID shardId;

    private volatile boolean accepted = false;

    private final Runnable onFatal;

    public InterconnectClient(String host, int port, Runnable onFatal) {
        this.host = host;
        this.port = port;
        this.onFatal = onFatal;
        System.out.println("[ServerFuser #" + id + "] created");
        try {
            this.shardId = Config.loadOrCreateShardId(Path.of("config", "serverfuser-shard-id.txt"));
        } catch (IOException e) {
            throw new RuntimeException("Could not load or create the shard ID", e);
        }
    }

    public void start(byte[] modHashSet, Supplier<MinecraftServer> server) {
        System.out.println("[ServerFuser #" + id + "] client started: " + System.identityHashCode(this));
        thread = new Thread(() -> runLoop(modHashSet, server), "ServerFuser-Interconnect");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    /** Safe to call from any thread. Returns false if the buffer is full. */
    public static boolean send(byte[] message) {
        return outbox.offer(message);
    }

    private void runLoop(byte[] modHashSet, Supplier<MinecraftServer> server) {
        long backoff = 1000;
        while (running) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                backoff = 1000; // reset after a successful connect

                var out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                var in  = new DataInputStream(socket.getInputStream());

                byte[] hs = Packets.encode(new Handshake(shardId, MessageTypes.PROTOCOL_VER, SharedConstants.getCurrentVersion().getDataVersion().getVersion(), modHashSet));

                out.writeInt(hs.length);
                out.write(hs);
                out.flush();

                // Reader runs on its own thread so writes don't block on reads
                Thread reader = new Thread(() -> readLoop(in, server), "ServerFuser-Interconnect-Reader");
                reader.setDaemon(true);
                reader.start();

                while (running && reader.isAlive()) {
                    byte[] msg = outbox.poll(1, TimeUnit.SECONDS);
                    if (msg == null) continue;
                    out.writeInt(msg.length);
                    out.write(msg);
                    out.flush();
                }
                System.out.println("[ServerFuser #" + id + "]  write loop exited: running=" + running
                        + " readerAlive=" + reader.isAlive());
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            } catch (Throwable t) {
                // connection failed or dropped: fall through to backoff
                System.out.println("[ServerFuser #" + id + "] reader ended: " + t);
            }
            finally {
                System.out.println("[ServerFuser #" + id + "]  reader thread exiting");
            }
            try { Thread.sleep(backoff); } catch (InterruptedException e) { return; }
            backoff = Math.min(backoff * 2, 15000);
        }
    }

    private static void sendAck(UUID sender, long txId, byte status) {
        try {
            send(Packets.encode(new TransferAck(sender, txId, status)));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void readLoop(DataInputStream in, Supplier<MinecraftServer> server) {
        try {
            while (running) {
                int len = in.readInt();
                if (len < 1 || len > 1_048_576) throw new IOException("bad frame length " + len);
                byte[] body = new byte[len];
                in.readFully(body);

                var d = new DataInputStream(new ByteArrayInputStream(body));
                Packet p = Packets.read(d);
                onReadPacket(p, server);
            }
        } catch (Throwable t) {
            System.out.println("[ServerFuser] reader ended: " + t);
        }
    }

    private void onReadPacket(Packet p, Supplier<MinecraftServer> server) throws IOException {
        switch (p) {
            case HandshakeResponce r -> {
                if (r.status().equals(MessageTypes.HS_OK)) {
                    accepted = true;
                    System.out.println("[ServerFuser] Handshake accepted");
                    Constants.LOG.error("[ServerFuser] Handshake accepted");
                } else {
                    System.out.println("[ServerFuser] Rejected by proxy: " + r.status());
                    Constants.LOG.error("[ServerFuser] Rejected by proxy: " + r.status());
                    running = false;       // stop retrying; the config has to be fixed
                    onFatal.run();
                }
            }
            case EntityData e -> {
                AfterTransferHandler.spawnEntity(e.nbt(), status -> sendAck(e.senderUUID(), e.txID(), status), server.get());
            }
            case TransferAck a -> Transfers.complete(a.txId(), a.status());
            default -> System.out.println("[ServerFuser] unexpected packet " + p);
        }
    }
    private void onMessage(String msg) {
        // Do something.
    }
}