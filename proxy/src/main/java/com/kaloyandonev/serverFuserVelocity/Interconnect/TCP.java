package com.kaloyandonev.serverFuserVelocity.Interconnect;

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

import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.CONNECTIONS;
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

    static void sendHandshakeReply(ShardConnection c, String status) throws IOException {
        c.send(Packets.encode(new HandshakeResponce(status)));
    }

    static void reject(Socket socket, UUID shardId, String status) {
        System.out.println(status);
        try { sendHandshakeReply(new ShardConnection(shardId, socket), status); }
        catch (IOException ignored) { }
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

        SocketAddress remote = socket.getRemoteSocketAddress();
        ShardConnection conn = null;

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
                    Handshake hs = Handshake.read(d);

                    boolean protoFlag;
                    boolean dataVerFlag;
                    boolean modHashSetFlag;
                    synchronized (InterconnectRuntimeConstants.class) {
                        if (!InterconnectRuntimeConstants.baselineSet) {
                            InterconnectRuntimeConstants.dataVer = hs.dataVer();
                            InterconnectRuntimeConstants.modHashSet = hs.modHashSet();
                            InterconnectRuntimeConstants.baselineSet = true;
                        }

                        System.out.println("Baseline set by shard " + hs.shardUUID() + ": dataVer " + hs.dataVer()
                                + ", modHashSet " + HexFormat.of().formatHex(hs.modHashSet()));

                        protoFlag = hs.protocolVer() == MessageTypes.PROTOCOL_VER;
                        dataVerFlag = hs.dataVer() == InterconnectRuntimeConstants.dataVer;
                        modHashSetFlag = MessageDigest.isEqual(hs.modHashSet(), InterconnectRuntimeConstants.modHashSet);
                    }

                    if (!protoFlag){
                        System.out.println(String.format("Rejected shard %s because of a protocol version mismatch. Expected value: %d, Actual value: %d", hs.shardUUID(), MessageTypes.PROTOCOL_VER, hs.protocolVer()));
                        reject(socket, hs.shardUUID(), MessageTypes.HS_PROTOCOLVER_MISMATCH);
                        socket.shutdownOutput();
                        return;
                    }
                    if (!dataVerFlag){
                        System.out.println(String.format("Rejected shard %s because of a data version mismatch. You are most likely trying to connect shard with different Minecraft versions. This is not supported. Expected value: %d, Actual value: %d", hs.shardUUID(), InterconnectRuntimeConstants.dataVer, hs.dataVer()));
                        reject(socket, hs.shardUUID(), MessageTypes.HS_DATAVER_MISMATCH);
                        socket.shutdownOutput();
                        return;
                    }
                    if (!modHashSetFlag){
                        System.out.println(String.format("Rejected shard %s because of a mod hash set mismatch. Please make sure that the mods installed on every shard match exactly.", hs.shardUUID()));
                        reject(socket, hs.shardUUID(), MessageTypes.HS_MODHASHSET_MISMATCH);
                        socket.shutdownOutput();
                        return;
                    }

                    conn = new ShardConnection(hs.shardUUID(), socket);
                    ShardConnection old = CONNECTIONS.put(hs.shardUUID(), conn);
                    if (old != null) old.close();
                    sendHandshakeReply(conn, MessageTypes.HS_OK);
                    System.out.println("Handshake from shard " + hs.shardUUID() + " with protocolVer " + hs.protocolVer()
                            + " and dataVer " + hs.dataVer() + " and modHashSet " + HexFormat.of().formatHex(hs.modHashSet()));


                }
                if (id == MessageTypes.CHAT) {
                    if (id != MessageTypes.HANDSHAKE && conn == null) {
                        System.out.println("Packet " + id + " before handshake from " + remote + ", dropping");
                        return;
                    }

                    Chat chat = Chat.read(d);
                    System.out.println(String.format("Got chat message from shard: %s %s %s %s", chat.senderUUID(), chat.senderName(), chat.chatTypeKey(), chat.chatComponentJSON()));
                }
                if (id == MessageTypes.PLAYERDATA) {

                    if (id != MessageTypes.HANDSHAKE && conn == null) {
                        System.out.println("Packet " + id + " before handshake from " + remote + ", dropping");
                        return;
                    }

                    PlayerData playerData = PlayerData.read(d);

                    try {
                        byte[] calcSha = MessageDigest.getInstance("SHA-256").digest(playerData.nbt());

                        if (!MessageDigest.isEqual(calcSha, playerData.sha())) {
                            throw new IOException("SHA Checksum invalid. Throwing data out.");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }


                    System.out.println(String.format("Got player data for playerUUID: %s with length %d and sha %s", playerData.playerUUID().toString(), playerData.nbtLen(), HexFormat.of().formatHex(playerData.sha())));
                    System.out.println(HexFormat.of().formatHex(playerData.nbt()));
                }
                if (id == MessageTypes.ENTITYDATA) {

                    if (id != MessageTypes.HANDSHAKE && conn == null) {
                        System.out.println("Packet " + id + " before handshake from " + remote + ", dropping");
                        return;
                    }

                    EntityData entityData = EntityData.read(d);

                    ShardConnection target = CONNECTIONS.get(entityData.recieverUUID());

                    if (entityData.nbtLen() < 0 || entityData.nbtLen() > d.available()) throw new IOException("bad nbt lenght " + entityData.nbtLen());

                    System.out.println("send: nbtLen=" + entityData.nbt().length + " sha=" + HexFormat.of().formatHex(entityData.sha()));

                    try {
                        byte[] calcSha = MessageDigest.getInstance("SHA-256").digest(entityData.nbt());

                        if (!MessageDigest.isEqual(calcSha, entityData.sha())) {
                            throw new IOException("SHA Checksum invalid. Throwing data out.");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    ByteBuffer.wrap(body)
                            .putLong(1, conn.shardId().getMostSignificantBits())
                            .putLong(9, conn.shardId().getLeastSignificantBits());

                    //TODO: remember who to route the ack to.

                    target.send(body);

                    System.out.println(String.format("Got mob data for entityUUID: %s with length %d and sha %s", entityData.entityUUID().toString(), entityData.nbtLen(), HexFormat.of().formatHex(entityData.sha())));
                    System.out.println(HexFormat.of().formatHex(entityData.nbt()));
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + remote);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Connection error: " + remote + ": " + e);
            e.printStackTrace();
        } finally {
            if (conn != null) {
                CONNECTIONS.remove(conn.shardId(), conn);
                System.out.println("Shard " + conn.shardId() + " removed");
            }
        }
    }

}
