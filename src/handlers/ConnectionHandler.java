package handlers;

import java.net.Socket;
import java.net.SocketAddress;

public interface ConnectionHandler {
    void handle(Socket connection, SocketAddress clientAddress);
}
