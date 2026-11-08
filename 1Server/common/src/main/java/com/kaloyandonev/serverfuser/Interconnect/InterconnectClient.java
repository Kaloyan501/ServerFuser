package com.kaloyandonev.serverfuser.Interconnect;

import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.config.Config;
import net.minecraft.SharedConstants;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.*;

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

    public InterconnectClient(String host, int port) {
        this.host = host;
        this.port = port;
        System.out.println("[ServerFuser #" + id + "] created");
        try {
            this.shardId = Config.loadOrCreateShardId(Path.of("config", "serverfuser-shard-id.txt"));
        } catch (IOException e) {
            throw new RuntimeException("Could not load or create the shard ID", e);
        }
    }

    private byte[] buildHandshake() {
        return ByteBuffer.allocate(1 + 16 + 4 + 4)
                .put(MessageTypes.HANDSHAKE)
                .putLong(shardId.getMostSignificantBits())
                .putLong(shardId.getLeastSignificantBits())
                .putInt(MessageTypes.PROTOCOL_VER)
                .putInt(SharedConstants.getCurrentVersion().getDataVersion().getVersion())
                .array();
    }

    public void start() {
        System.out.println("[ServerFuser #" + id + "] client started: " + System.identityHashCode(this));
        thread = new Thread(this::runLoop, "ServerFuser-Interconnect");
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

    private void runLoop() {
        long backoff = 1000;
        while (running) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                backoff = 1000; // reset after a successful connect



                var out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                var in  = new DataInputStream(socket.getInputStream());

                byte[] hs = buildHandshake();
                out.writeInt(hs.length);
                out.write(hs);
                out.flush();

                // Reader runs on its own thread so writes don't block on reads
                Thread reader = new Thread(() -> readLoop(in), "ServerFuser-Interconnect-Reader");
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

    private void readLoop(DataInputStream in) {
        try {
            while (running) {
                String msg = in.readUTF();
                onMessage(msg);
            }
        } catch (IOException ignored) { /* EOF or reset: main loop reconnects */ }
    }


    private void onMessage(String msg) {
        // Do something.
    }
}