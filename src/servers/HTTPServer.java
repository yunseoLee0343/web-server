package servers;

import handlers.HTTPConnectionHandler;
import models.HTTPRequest;
import models.HTTPResponse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HTTPServer extends TCPServer {
    private String serveDocroot;
    private Map<String, String> serveConfig;
    private static HTTPServer instance;

    public static synchronized HTTPServer getInstance(int port) {
        if (instance == null) {
            instance = new HTTPServer(port);
        }
        return instance;
    }

    public HTTPServer(int port) {
        super(port, null, true);
        this.serveDocroot = null;
        this.serveConfig = new HashMap<>();
    }

    public String getServeDocroot() {
        return serveDocroot;
    }

    public void serve(String docroot, Map<String, String> serveConfig) {
        this.serveDocroot = docroot;
        this.serveConfig = serveConfig;
    }

    public String getIndexHtmlPath() {
        return Paths.get(serveConfig.getOrDefault("index", "index.html")).toString();
    }

    public String getAbsolutePathRelativeToDocroot(String path) {
        assert serveDocroot != null : "Must setup `serveDocroot` first.";
        return Paths.get(serveDocroot, path).toAbsolutePath().toString();
    }

    private String getLastModifiedFormattedString(String requestedPath) {
        File file = new File(requestedPath);
        long timestamp = file.lastModified();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        return dateFormat.format(new Date(timestamp));
    }

    public void serveFile(HTTPRequest request, HTTPConnectionHandler httpConnection) throws IOException {
        String requestedPath = request.getPath().equals("/") ? getIndexHtmlPath() : request.getPath(); //index.html
        if (requestedPath.startsWith("/")) {
            requestedPath = requestedPath.substring(1);
        }
        String absRequestedPath = getAbsolutePathRelativeToDocroot(requestedPath);
        System.out.println("Requested path: " + absRequestedPath);
        String absDocrootPath = Paths.get(serveDocroot).toAbsolutePath().toString();
        System.out.println("Docroot path: " + absDocrootPath);
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
}
