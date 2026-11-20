package com.example.billard.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

/**
 * Hosts a match on this computer: listens on a port and takes whoever calls in. The link checks who it is, so a
 * stranger's connection is simply dropped and the next one accepted.
 */
public final class DirectHost implements Connector {

    public static final int DEFAULT_PORT = 47474;

    private final ServerSocket server;

    /**
     * @param port the port to listen on, or 0 for any free one
     * @throws IOException if the port is taken or cannot be opened
     */
    public DirectHost(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    public int port() {
        return server.getLocalPort();
    }

    @Override
    public LineChannel connect() throws IOException {
        Socket socket = server.accept();
        socket.setTcpNoDelay(true);
        return new LineChannel(socket);
    }

    @Override
    public void close() {
        try {
            server.close();
        } catch (IOException ignored) {
            // Already closed or never fully open; the port is free either way.
        }
    }

    /** This computer's addresses on its local networks, which a player on the same network can join. */
    public static List<String> localAddresses() {
        try {
            return NetworkInterface.networkInterfaces()
                    .filter(DirectHost::isUsable)
                    .flatMap(NetworkInterface::inetAddresses)
                    .filter(address -> address instanceof Inet4Address && !address.isLoopbackAddress())
                    .map(InetAddress::getHostAddress)
                    .toList();
        } catch (SocketException e) {
            return List.of();
        }
    }

    private static boolean isUsable(NetworkInterface network) {
        try {
            return network.isUp() && !network.isLoopback() && !network.isVirtual();
        } catch (SocketException e) {
            return false;
        }
    }
}
