package com.kaloyandonev.serverFuserVelocity;

import com.google.inject.Inject;
import com.kaloyandonev.serverFuserVelocity.Interconnect.TCP;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "serverfuservelocity", name = "ServerFuserVelocity", version = "1.0-SNAPSHOT")
public class ServerFuserVelocity {

    public static Logger logger;
    public static ProxyServer proxy;

    @Inject
    public ServerFuserVelocity(ProxyServer server, Logger logger) {
        ServerFuserVelocity.proxy = server;
        ServerFuserVelocity.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Thread t = new Thread(() -> {
            try {
                TCP.CreateInterconnectSocket(6666);
            } catch (Exception e) {
                logger.error("Interconnect has blown up.", e);
            }
        }, "ServerFuser-Interconnect-Listener");
        t.setDaemon(true);
        t.start();
    }
}
