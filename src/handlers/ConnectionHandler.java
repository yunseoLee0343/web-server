package handlers;

import servers.HTTPServer;

import java.net.Socket;
import java.net.SocketAddress;

public interface ConnectionHandler {
    void handle(HTTPServer server, Socket connection, SocketAddress clientAddress);
}
