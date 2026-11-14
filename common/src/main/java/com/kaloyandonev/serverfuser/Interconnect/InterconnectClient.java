package com.kaloyandonev.serverfuser.Interconnect;

import com.kaloyandonev.serverfuser.Constants;
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

    private byte[] buildHandshake(byte[] modHashSet) {
        return ByteBuffer.allocate(1 + 16 + 4 + 4 + 32)
                .put(MessageTypes.HANDSHAKE)
                .putLong(shardId.getMostSignificantBits())
                .putLong(shardId.getLeastSignificantBits())
                .putInt(MessageTypes.PROTOCOL_VER)
                .putInt(SharedConstants.getCurrentVersion().getDataVersion().getVersion())
                .put(modHashSet)
                .array();
    }

    public void start(byte[] modHashSet) {
        System.out.println("[ServerFuser #" + id + "] client started: " + System.identityHashCode(this));
        thread = new Thread(() -> runLoop(modHashSet), "ServerFuser-Interconnect");
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

    private void runLoop(byte[] modHashSet) {
        long backoff = 1000;
        while (running) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                backoff = 1000; // reset after a successful connect



                var out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                var in  = new DataInputStream(socket.getInputStream());

                byte[] hs = buildHandshake(modHashSet);
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
                int len = in.readInt();
                if (len < 1 || len > 1_048_576) throw new IOException("bad frame length " + len);
                byte[] body = new byte[len];
                in.readFully(body);

                var d = new DataInputStream(new ByteArrayInputStream(body));
                byte id = d.readByte();
                onReadPacket(id, d);
            }
        } catch (Throwable t) {
            System.out.println("[ServerFuser] reader ended: " + t);
        }
    }

    private void onReadPacket(byte id, DataInputStream d) throws IOException {
        switch (id) {
            case MessageTypes.HANDSHAKE_RESPONCE -> {
                String status = d.readUTF();
                if (status.equals(MessageTypes.HS_OK)) {
                    accepted = true;
                    System.out.println("[ServerFuser] Handshake accepted");
                    Constants.LOG.error("[ServerFuser] Handshake accepted");
                } else {
                    System.out.println("[ServerFuser] Rejected by proxy: " + status);
                    Constants.LOG.error("[ServerFuser] Rejected by proxy: " + status);
                    running = false;       // stop retrying; the config has to be fixed
                    onFatal.run();
                }
            }
            default -> System.out.println("[ServerFuser] Unknown packet id " + id);
        }
    }

    private void onMessage(String msg) {
        // Do something.
    }
}