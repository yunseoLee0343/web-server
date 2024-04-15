package servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import handlers.HTTPConnectionHandler;


public class TCPServer {
    private String host;
    private int port;
    private boolean useDaemonThreads;
    private ServerSocket serverSocket;
    private Semaphore semaphore;
    private final int connectionBacklogQueueSize = 5;
    private Map<Long, Thread> threads;

    public TCPServer(int port, Integer maxNumberOfActiveThreads, boolean useDaemonThreads) {
        this.host = "";
        this.port = port;
        this.useDaemonThreads = useDaemonThreads;
        this.threads = new HashMap<>();

        if (maxNumberOfActiveThreads != null) {
            this.semaphore = new Semaphore(maxNumberOfActiveThreads);
        } else {
            this.semaphore = null;
        }
    }

    private void findSocketToBindAndListen() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
    }

    public void serveForever(HTTPServer server) {
        try {
            findSocketToBindAndListen();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new client " + clientSocket.getRemoteSocketAddress());
                Thread thread = new Thread(() -> handleNewConnectionThread(server, clientSocket, clientSocket.getRemoteSocketAddress()));
                thread.setDaemon(useDaemonThreads);
                thread.start();
                if (!useDaemonThreads) {
                    threads.put(thread.getId(), thread);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleNewConnectionThread(HTTPServer server, Socket connection, SocketAddress clientAddress) {
        try {
            if (semaphore != null) {
                semaphore.acquire();
            }
            HTTPConnectionHandler connectionHandler = new HTTPConnectionHandler(connection, clientAddress.toString());
            connectionHandler.handle(server, connection, clientAddress);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (semaphore != null) {
                semaphore.release();
            }
            if (!useDaemonThreads) {
                threads.remove(Thread.currentThread().getId());
            }
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void serverClose() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!useDaemonThreads) {
            for (Thread thread : threads.values()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

