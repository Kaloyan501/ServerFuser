package com.kaloyandonev.serverFuserVelocity.PacketHandler;

import com.kaloyandonev.serverfuser.Messages.Chat;

public class ChatHandler {
    public static void handle(Chat c){
        System.out.printf("Got chat message from shard: %s %s %s %s%n", c.senderUUID(), c.senderName(), c.chatTypeKey(), c.chatComponentJSON());
    }
}
