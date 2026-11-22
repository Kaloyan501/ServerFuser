package com.kaloyandonev.serverFuserVelocity.PacketHandler;

import com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants;
import com.kaloyandonev.serverFuserVelocity.Interconnect.ShardConnection;
import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.Handshake;
import com.kaloyandonev.serverfuser.Messages.HandshakeResponce;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;

import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.HexFormat;

import static com.kaloyandonev.serverFuserVelocity.Interconnect.InterconnectRuntimeConstants.CONNECTIONS;
import static com.kaloyandonev.serverFuserVelocity.Interconnect.TCP.reject;

public class HandshakeHandler {
    public static ShardConnection handle(Handshake h, Socket socket) throws IOException {
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
            return null;
        }
        if (!dataVerFlag){
            System.out.println(String.format("Rejected shard %s because of a data version mismatch. You are most likely trying to connect shard with different Minecraft versions. This is not supported. Expected value: %d, Actual value: %d", h.shardUUID(), InterconnectRuntimeConstants.dataVer, h.dataVer()));
            reject(socket, h.shardUUID(), MessageTypes.HS_DATAVER_MISMATCH);
            socket.shutdownOutput();
            return null;
        }
        if (!modHashSetFlag){
            System.out.println(String.format("Rejected shard %s because of a mod hash set mismatch. Please make sure that the mods installed on every shard match exactly.", h.shardUUID()));
            reject(socket, h.shardUUID(), MessageTypes.HS_MODHASHSET_MISMATCH);
            socket.shutdownOutput();
            return null;
        }

        ShardConnection conn = new ShardConnection(h.shardUUID(), socket);
        ShardConnection old = CONNECTIONS.put(h.shardUUID(), conn);
        if (old != null) old.close();
        conn.send(Packets.encode(new HandshakeResponce(MessageTypes.HS_OK)));
        System.out.println("Handshake from shard " + h.shardUUID() + " with protocolVer " + h.protocolVer()
                + " and dataVer " + h.dataVer() + " and modHashSet " + HexFormat.of().formatHex(h.modHashSet()));
        return conn;
    }
}
