import models.HTTPRequest;
import models.HTTPResponse;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HTTPServer extends TCPServer {
    private String serveDocroot;
    private Map<String, String> serveConfig;

    public HTTPServer(int port) {
        super(port, new HTTPConnectionHandler(), null, true);
        this.serveDocroot = null;
        this.serveConfig = new HashMap<>();
    }

    @Override
    public void handleTCPConnection(Socket connection, String clientAddress) {
        HTTPConnectionHandler httpConnection = new HTTPConnectionHandler();
        try {
            while (true) {
                HTTPRequest request = httpConnection.getRequest(connection);
                if (request == null) {
                    break;
                }
                serveFile(request, httpConnection);
                if (!request.isConnectionKeepAlive()) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void serve(String docroot, Map<String, String> serveConfig) {
        this.serveDocroot = docroot;
        this.serveConfig = serveConfig;
    }

    public String getIndexHtmlPath() {
        return Paths.get("/", serveConfig.getOrDefault("index", "index.html")).toString();
    }

    public String getAbsolutePathRelativeToDocroot(String path) {
        assert serveDocroot != null : "Must setup `serveDocroot` first.";
        return Paths.get(serveDocroot, path).toAbsolutePath().toString();
    }

    private void serveFile(HTTPRequest request, HTTPConnectionHandler httpConnection) throws IOException {
        String requestedPath = request.getPath().equals("/") ? getIndexHtmlPath() : request.getPath();
        if (requestedPath.startsWith("/")) {
            requestedPath = requestedPath.substring(1);
        }
        String absRequestedPath = getAbsolutePathRelativeToDocroot(requestedPath);
        String absDocrootPath = Paths.get(serveDocroot).toAbsolutePath().toString();
        // Won't serve out of docroot
        if (!absRequestedPath.startsWith(absDocrootPath)) {
            if (serveConfig.containsKey("400")) {
                httpConnection.sendResponse(HTTPResponse.clientError400());
                httpConnection.sendFile(getAbsolutePathRelativeToDocroot(serveConfig.get("400")));
            } else {
                httpConnection.sendResponse(HTTPResponse.clientError400());
            }
            return;
        }
        // File not exists
        if (!Files.exists(Paths.get(absRequestedPath))) {
            if (serveConfig.containsKey("404")) {
                httpConnection.sendResponse(HTTPResponse.notFound404());
                httpConnection.sendFile(getAbsolutePathRelativeToDocroot(serveConfig.get("404")));
            } else {
                httpConnection.sendResponse(HTTPResponse.notFound404());
            }
            return;
        }

        String mimeType = Files.probeContentType(Paths.get(absRequestedPath));
        long fileSize = Files.size(Paths.get(absRequestedPath));
        String lastModifiedTime = getLastModifiedFormattedString(absRequestedPath);

        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", mimeType);
        responseHeaders.put("Content-Length", String.valueOf(fileSize));
        responseHeaders.put("Last-Modified", lastModifiedTime);

        // Send first-line and headers
        httpConnection.sendResponse(new HTTPResponse(200, responseHeaders));
        // Send body
        httpConnection.sendFile(absRequestedPath);
    }

    private String getLastModifiedFormattedString(String requestedPath) {
        File file = new File(requestedPath);
        long timestamp = file.lastModified();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        return dateFormat.format(new Date(timestamp));
    }

    public static void main(String[] args) {
        HTTPServer server = new HTTPServer(8080);
        Map<String, String> serveConfig = new HashMap<>();
        serveConfig.put("index", "custom_index.html");
        serveConfig.put("400", "400.html");
        serveConfig.put("404", "404.html");
        server.serve("/path/to/docroot", serveConfig);
        server.serveForever();
    }
}
