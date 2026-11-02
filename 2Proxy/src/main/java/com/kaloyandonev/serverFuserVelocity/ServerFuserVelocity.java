package com.kaloyandonev.serverFuserVelocity;

import com.google.inject.Inject;
import com.kaloyandonev.serverFuserVelocity.Interconnect.TCP;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(id = "serverfuservelocity", name = "ServerFuserVelocity", version = "1.0-SNAPSHOT")
public class ServerFuserVelocity {

    @Inject
    public static Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            TCP.CreateInterconnectSocket(6666);
        } catch (Exception e) {
            logger.error("Interconnect has blown up.");
        }
    }
}
