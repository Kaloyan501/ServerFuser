package com.kaloyandonev.serverFuserVelocity.Interconnect;

import com.kaloyandonev.serverFuserVelocity.PacketHandler.*;
import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.*;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.CONNECTIONS;
import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.PENDING;
import static com.kaloyandonev.serverFuserVelocity.ServerFuserVelocity.proxy;
import static com.kaloyandonev.serverFuserVelocity.TransferRouter.failToSender;

public class TCP {

    static boolean isKnownBackend(InetAddress remote) {
        for (RegisteredServer rs : proxy.getAllServers()) {
            InetSocketAddress addr = rs.getServerInfo().getAddress();
            try {
                for (InetAddress known : InetAddress.getAllByName(addr.getHostString())) {
                    if (known.equals(remote)) return true;
                }
            } catch (UnknownHostException ignored) { }
        }
        return false;
    }

    public static void createInterconnectSocket(int port) throws IOException{
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[ServerFuser Interconnect] Listening on port " + port + " ...");
            while (true) {
                Socket socket = serverSocket.accept();

                InetAddress remote = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress();
                if (!isKnownBackend(remote)) {
                    System.out.println("Rejected interconnect connection from " + remote);
                    socket.close();
                    continue;
                }

                Thread.ofVirtual().start(() -> handle(socket));
            }
        }
    }

    static void handle(Socket socket) {

        SocketAddress remote = socket.getRemoteSocketAddress();
        ShardConnection conn = null;

        try (socket; var input = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            System.out.println("Client connected: " + socket.getRemoteSocketAddress());
            while (true) {
                int len = input.readInt();
                if (len < 1 || len > 1_048_576) throw new IOException("bad frame length " + len);
                byte[] body = new byte[len];
                input.readFully(body);

                var d = new DataInputStream(new ByteArrayInputStream(body));
                Packet p = Packets.read(d);
                if (!(p instanceof Handshake) && conn == null) {
                    System.out.println("Packet " + p.id() + " before handshake from " + remote + ", dropping");
                    return;
                }

                switch (p) {
                    case Handshake h -> {
                        conn = HandshakeHandler.handle(h, socket);
                    }
                    case Chat c -> {
                        ChatHandler.handle(c);
                    }
                    case EntityData e -> {
                        EntityHandler.handle(e, conn, body);
                    }
                    case PlayerData pd -> {
                        PlayerHandler.handle(pd);
                    }
                    case TransferAck a -> {
                        TransferAckHandler.handle(a, conn);
                    }
                    default -> {}
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + remote);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Connection error: " + remote + ": " + e);
            e.printStackTrace();
        } finally {
            if (conn != null && CONNECTIONS.remove(conn.shardId(), conn)) {
                UUID id = conn.shardId();
                System.out.println("Shard " + id + " removed");
                PENDING.forEach((k, v) -> {
                    if (v.target().equals(id) && PENDING.remove(k, v)) {
                        failToSender(k, MessageTypes.STATUS_TARGET_OFFLINE);
                    } else if (k.sender().equals(id)) {
                        PENDING.remove(k);
                    }
                });
            }
        }
    }
}
