package com.kaloyandonev.serverfuser.Interconnect;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public final class InterconnectClient {
    private final String host;
    private final int port;
    private static final BlockingQueue<byte[]> outbox = new LinkedBlockingQueue<>(1024); // bounded
    private volatile boolean running = true;
    private Thread thread;

    public InterconnectClient(String host, int port) { this.host = host; this.port = port; }

    public void start() {
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
            } catch (InterruptedException e) {
                return;
            } catch (IOException e) {
                // connection failed or dropped: fall through to backoff
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