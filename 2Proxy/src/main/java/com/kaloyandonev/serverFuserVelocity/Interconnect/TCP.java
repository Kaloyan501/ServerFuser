package com.kaloyandonev.serverFuserVelocity.Interconnect;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static com.kaloyandonev.serverFuserVelocity.ServerFuserVelocity.proxy;

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

    public static void CreateInterconnectSocket(int port) throws IOException{
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
        try (socket; var input = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            System.out.println("Client connected: " + socket.getRemoteSocketAddress());
            while (true) {
                int len = input.readInt();                       // throws EOFException on disconnect
                if (len < 1 || len > 1_048_576) throw new IOException("bad frame length " + len);
                byte[] body = new byte[len];
                input.readFully(body);

                var d = new DataInputStream(new ByteArrayInputStream(body));
                byte id = d.readByte();
                if (id == MessageTypes.HANDSHAKE) {
                    UUID shardId = new UUID(d.readLong(), d.readLong());
                    int protocolVer = d.readInt();
                    int dataVer = d.readInt();
                    System.out.println("Handshake from shard " + shardId + " with protocolVer " + protocolVer + " and dataVer " + dataVer);
                }
                if (id == MessageTypes.CHAT) {
                    UUID senderId = new UUID(d.readLong(), d.readLong());
                    String senderName = d.readUTF();
                    String chatTypeKey = d.readUTF();
                    int jsonLen = d.readInt();
                    byte[] json = new byte[jsonLen];
                    d.readFully(json);
                    String jsonString = new String(json, StandardCharsets.UTF_8);
                    System.out.println(String.format("Got chat message from shard: %s %s %s %s", senderId, senderName, chatTypeKey, jsonString));
                }
                if (id == MessageTypes.PLAYERDATA) {
                    UUID playerUUID = null;
                    byte[] sha = new byte[32];
                    int nbtLen = 0;
                    byte[] nbt = null;
                    playerUUID = new UUID(d.readLong(), d.readLong());
                    d.readFully(sha);
                    nbtLen = d.readInt();
                    if (nbtLen < 0 || nbtLen > d.available()) throw new IOException("bad nbt length " + nbtLen);
                    nbt = new byte[nbtLen];
                    d.readFully(nbt);

                    try {
                        byte[] calcSha = MessageDigest.getInstance("SHA-256").digest(nbt);

                        if (!MessageDigest.isEqual(calcSha, sha)) {
                            throw new IOException("SHA Checksum invalid. Throwing data out.");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }


                    System.out.println(String.format("Got player data for playerUUID: %s with length %d and sha %s", playerUUID.toString(), nbtLen, HexFormat.of().formatHex(sha)));
                    System.out.println(HexFormat.of().formatHex(nbt));
                }
                if (id == MessageTypes.ENTITYDATA) {
                    UUID entityUUID = null;
                    byte[] sha = new byte[32];
                    int nbtLen = 0;
                    byte[] nbt = null;
                    entityUUID = new UUID(d.readLong(), d.readLong());
                    d.readFully(sha);
                    nbtLen = d.readInt();
                    if (nbtLen < 0 || nbtLen > d.available()) throw new IOException("bad nbt lenght " + nbtLen);
                    nbt = new byte[nbtLen];
                    d.readFully(nbt);

                    try {
                        byte[] calcSha = MessageDigest.getInstance("SHA-256").digest(nbt);

                        if (!MessageDigest.isEqual(calcSha, sha)) {
                            throw new IOException("SHA Checksum invalid. Throwing data out.");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    System.out.println(String.format("Got mob data for entityUUID: %s with length %d and sha %s", entityUUID.toString(), nbtLen, HexFormat.of().formatHex(sha)));
                    System.out.println(HexFormat.of().formatHex(nbt));
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Connection error: " + e);
            e.printStackTrace();
        }
    }

}
