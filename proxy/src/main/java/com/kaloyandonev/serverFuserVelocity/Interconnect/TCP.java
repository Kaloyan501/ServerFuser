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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.CONNECTIONS;
import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.PENDING;
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

    private static void forward(ShardConnection from, UUID receiver, long txId, byte[] body) throws IOException {
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

    private static void failToSender(InterconnectRuntimeConstants.TxKey k, byte status) {
        ShardConnection s = CONNECTIONS.get(k.sender());
        if (s != null) try { s.send(Packets.encode(new TransferAck(k.sender(), k.txId(), status))); } catch (IOException ignored) {}
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
                Packet p = Packets.read(d);
                if (!(p instanceof Handshake) && conn == null) {
                    System.out.println("Packet " + p.id() + " before handshake from " + remote + ", dropping");
                    return;
                }

                switch (p) {
                    case Handshake h -> {
                        boolean protoFlag;
                        boolean dataVerFlag;
                        boolean modHashSetFlag;
                        synchronized (InterconnectRuntimeConstants.class) {
                            if (!InterconnectRuntimeConstants.baselineSet) {

                                InterconnectRuntimeConstants.dataVer = h.dataVer();
                                InterconnectRuntimeConstants.modHashSet = h.modHashSet();
                                InterconnectRuntimeConstants.baselineSet = true;

                                System.out.println("Baseline set by shard " + h.shardUUID() + ": dataVer " + h.dataVer()
                                        + ", modHashSet " + HexFormat.of().formatHex(h.modHashSet()));
                            }

                            protoFlag = h.protocolVer() == MessageTypes.PROTOCOL_VER;
                            dataVerFlag = h.dataVer() == InterconnectRuntimeConstants.dataVer;
                            modHashSetFlag = MessageDigest.isEqual(h.modHashSet(), InterconnectRuntimeConstants.modHashSet);
                        }

                        if (!protoFlag){
                            System.out.println(String.format("Rejected shard %s because of a protocol version mismatch. Expected value: %d, Actual value: %d", h.shardUUID(), MessageTypes.PROTOCOL_VER, h.protocolVer()));
                            reject(socket, h.shardUUID(), MessageTypes.HS_PROTOCOLVER_MISMATCH);
                            socket.shutdownOutput();
                            return;
                        }
                        if (!dataVerFlag){
                            System.out.println(String.format("Rejected shard %s because of a data version mismatch. You are most likely trying to connect shard with different Minecraft versions. This is not supported. Expected value: %d, Actual value: %d", h.shardUUID(), InterconnectRuntimeConstants.dataVer, h.dataVer()));
                            reject(socket, h.shardUUID(), MessageTypes.HS_DATAVER_MISMATCH);
                            socket.shutdownOutput();
                            return;
                        }
                        if (!modHashSetFlag){
                            System.out.println(String.format("Rejected shard %s because of a mod hash set mismatch. Please make sure that the mods installed on every shard match exactly.", h.shardUUID()));
                            reject(socket, h.shardUUID(), MessageTypes.HS_MODHASHSET_MISMATCH);
                            socket.shutdownOutput();
                            return;
                        }

                        conn = new ShardConnection(h.shardUUID(), socket);
                        ShardConnection old = CONNECTIONS.put(h.shardUUID(), conn);
                        if (old != null) old.close();
                        sendHandshakeReply(conn, MessageTypes.HS_OK);
                        System.out.println("Handshake from shard " + h.shardUUID() + " with protocolVer " + h.protocolVer()
                                + " and dataVer " + h.dataVer() + " and modHashSet " + HexFormat.of().formatHex(h.modHashSet()));
                    }
                    case Chat c -> {
                        System.out.println(String.format("Got chat message from shard: %s %s %s %s", c.senderUUID(), c.senderName(), c.chatTypeKey(), c.chatComponentJSON()));
                    }
                    case EntityData e -> {
                        ShardConnection target = CONNECTIONS.get(e.recieverUUID());

                        System.out.println("send: nbtLen=" + e.nbt().length + " sha=" + HexFormat.of().formatHex(e.sha()));

                        try {
                            byte[] calcSha = MessageDigest.getInstance("SHA-256").digest(e.nbt());

                            if (!MessageDigest.isEqual(calcSha, e.sha())) {
                                conn.send(Packets.encode(new TransferAck(conn.shardId(), e.txID(), MessageTypes.STATUS_CHECKSUM_INVALID)));
                                continue;
                            }

                        } catch (Exception o) {
                            o.printStackTrace();
                            return;
                        }

                        ByteBuffer.wrap(body)
                                .putLong(1, conn.shardId().getMostSignificantBits())
                                .putLong(9, conn.shardId().getLeastSignificantBits());

                        forward(conn, e.recieverUUID(), e.txID(), body);

                        System.out.println(String.format("Got mob data for entityUUID: %s with length %d and sha %s", e.entityUUID().toString(), e.nbtLen(), HexFormat.of().formatHex(e.sha())));
                        System.out.println(HexFormat.of().formatHex(e.nbt()));
                    }
                    case PlayerData pd -> {
                        try {
                            byte[] calcSha = MessageDigest.getInstance("SHA-256").digest(pd.nbt());

                            if (!MessageDigest.isEqual(calcSha, pd.sha())) {
                                throw new IOException("SHA Checksum invalid. Throwing data out.");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }


                        System.out.println(String.format("Got player data for playerUUID: %s with length %d and sha %s", pd.playerUUID().toString(), pd.nbtLen(), HexFormat.of().formatHex(pd.sha())));
                        System.out.println(HexFormat.of().formatHex(pd.nbt()));
                    }
                    case TransferAck a -> {
                        var key = new InterconnectRuntimeConstants.TxKey(a.senderUUID(), a.txId());
                        InterconnectRuntimeConstants.Pending pend = PENDING.get(key);
                        if (pend == null || !pend.target().equals(conn.shardId())) continue;
                        if (!PENDING.remove(key, pend)) continue;
                        ShardConnection origin = CONNECTIONS.get(a.senderUUID());
                        if (origin != null) origin.send(Packets.encode(a));
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
            if (conn != null) {
                UUID id = conn.shardId();
                CONNECTIONS.remove(id, conn);
                PENDING.forEach((k, v) -> {
                    if (v.target().equals(id) && PENDING.remove(k, v)) failToSender(k, MessageTypes.STATUS_TARGET_OFFLINE);
                    else if (k.sender().equals(id)) PENDING.remove(k);
                });
            }
        }
    }
}
