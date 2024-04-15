package handlers;

import errors.BadRequestError;
import errors.RecvTimeoutError;
import models.HTTPRequest;
import models.HTTPResponse;
import servers.HTTPServer;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HTTPConnectionHandler implements ConnectionHandler {
    private static final String CRLF = "\r\n";
    private static final String END_OF_REQUEST = "\r\n\r\n";
    private static final int BUFFER_SIZE = 4096;
    private static final int RECV_TIMEOUT = 3000;

    private static HTTPConnectionHandler instance;

    private final Socket connection;
    private final String clientAddress;
    private String unprocessedData;

    public HTTPConnectionHandler(Socket connection, String clientAddress) {
        this.connection = connection;
        this.clientAddress = clientAddress;
        this.unprocessedData = "";
    }

    @Override
    public void handle(Socket connection, SocketAddress clientAddress) {
        HTTPConnectionHandler httpConnection = new HTTPConnectionHandler(connection, clientAddress.toString());
        try {
            while (true) {
                HTTPRequest request = httpConnection.getRequest();
                if (request == null) {
                    break;
                }
                HTTPServer httpServer = HTTPServer.getInstance(80);
                httpServer.serveFile(request, httpConnection);
                if (!request.isConnectionKeepAlive()) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadRequestError e) {
            throw new RuntimeException(e);
        } catch (RecvTimeoutError e) {
            throw new RuntimeException(e);
        } finally {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized HTTPConnectionHandler getInstance(Socket connection, String clientAddress) {
        if (instance == null) {
            instance = new HTTPConnectionHandler(connection, clientAddress);
        }
        return instance;
    }

    public HTTPRequest getRequest() throws IOException, BadRequestError, RecvTimeoutError {
        return detectRequestFromSocket();
    }

    public void sendResponse(HTTPResponse response) throws IOException {
        String responseString = response.formattedString();
        connection.getOutputStream().write(responseString.getBytes(StandardCharsets.UTF_8));
    }

    public void sendFile(String filePath) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (FileInputStream fis = new FileInputStream(filePath)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                connection.getOutputStream().write(buffer, 0, bytesRead);
            }
        }
    }

    public void close() throws IOException {
        connection.close();
        System.out.println("Closed the socket " + clientAddress);
    }

    private HTTPRequest detectRequestFromSocket() throws IOException, BadRequestError, RecvTimeoutError {
        try {
            connection.setSoTimeout(RECV_TIMEOUT);
            String requestString;
            while (true) {
                if (unprocessedData.contains(END_OF_REQUEST)) {
                    int endOfRequestIndex = unprocessedData.indexOf(END_OF_REQUEST);
                    requestString = unprocessedData.substring(0, endOfRequestIndex);
                    unprocessedData = unprocessedData.substring(endOfRequestIndex + END_OF_REQUEST.length());
                    return parseRequest(requestString);
                }
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead = connection.getInputStream().read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                unprocessedData += new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            }
        } finally {
            connection.setSoTimeout(0); // Reset timeout
        }
        return null;
    }

    private HTTPRequest parseRequest(String requestString) throws BadRequestError {
        String[] lines = requestString.split(CRLF);
        String[] headlineComponents = lines[0].split(" ");
        if (headlineComponents.length != 3) {
            throw new BadRequestError("Headline must have 3 parts: " + lines[0]);
        }
        String command = headlineComponents[0];
        String resourcePath = headlineComponents[1];
        String httpVersion = headlineComponents[2];
        if (!command.equals("GET")) {
            throw new BadRequestError("Only GET is supported: " + command);
        }
        if (!httpVersion.equals("HTTP/1.1")) {
            throw new BadRequestError("Only HTTP/1 is supported: " + httpVersion);
        }
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String[] keyValue = lines[i].split(": ");
            if (keyValue.length != 2) {
                throw new BadRequestError("Malformed key-value header: " + lines[i]);
            }
            headers.put(keyValue[0], keyValue[1]);
        }
        String[] requiredKeys = {"Host"}; // specify required header(s) here
        for (String key : requiredKeys) {
            if (!headers.containsKey(key)) {
                throw new BadRequestError("Required header not found: " + key);
            }
        }
        return new HTTPRequest(command, resourcePath, headers);
    }
}
