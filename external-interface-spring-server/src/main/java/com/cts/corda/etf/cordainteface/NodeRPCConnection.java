package com.cts.corda.etf.cordainteface;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NodeRPCConnection {

    @NotNull
    private final CordaRPCOps proxy;

    public NodeRPCConnection(@Value("${config.rpc.host}") @NotNull String host,
                             @Value("${config.rpc.username}") @NotNull String username,
                             @Value("${config.rpc.password}") @NotNull String password,
                             @Value("${config.rpc.port}") int rpcPort) {
        super();
        NetworkHostAndPort rpcAddress = new NetworkHostAndPort(host, rpcPort);
        System.out.println("-->rpcAddress " + rpcAddress + "   username " + username);
        CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
        CordaRPCConnection rpcConnection = rpcClient.start(username, password);
        this.proxy = rpcConnection.getProxy();
    }

    @NotNull
    public final CordaRPCOps getProxy() {
        return this.proxy;
    }
}
