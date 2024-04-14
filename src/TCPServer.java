import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class TCPServer {
    private String host;
    private int port;
    private ConnectionHandler connectionHandler;
    private boolean useDaemonThreads;
    private ServerSocket serverSocket;
    private Semaphore semaphore;
    private int connectionBacklogQueueSize = 5;
    private Map<Long, Thread> threads;

    public TCPServer(int port, ConnectionHandler connectionHandler, Integer maxNumberOfActiveThreads, boolean useDaemonThreads) {
        this.host = "";
        this.port = port;
        this.connectionHandler = connectionHandler;
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

    public void serveForever() {
        try {
            findSocketToBindAndListen();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new client " + clientSocket.getRemoteSocketAddress());
                Thread thread = new Thread(() -> handleNewConnectionThread(clientSocket));
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

    private void handleNewConnectionThread(Socket connection) {
        try {
            if (semaphore != null) {
                semaphore.acquire();
            }
            connectionHandler.handle(connection);
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

    public static void main(String[] args) {
        // Example usage
        TCPServer server = new TCPServer(8080, (connection -> {
            // Handle connection
        }), 10, false);
        server.serveForever();
    }
}

interface ConnectionHandler {
    void handle(Socket connection);
}
