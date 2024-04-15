import servers.HTTPServer;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        HTTPServer server = new HTTPServer(80);
        server.serve("sample-docroot", Map.ofEntries(
            Map.entry("index", "index.html"),
            Map.entry("gallery", "gallery.html"),
            Map.entry("400", "400.html"),
            Map.entry("404", "404.html"))
        );
        // Handle server close with ctrl+c in terminal
        Runtime.getRuntime().addShutdownHook(new Thread(server::serverClose));
        // Start the server
        server.serveForever(server);
    }
}