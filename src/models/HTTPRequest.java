package models;

import java.util.Map;

public class HTTPRequest {
    private final String command;
    private final String path;
    private final Map<String, String> headers;

    public HTTPRequest(String command, String path, Map<String, String> headers) {
        this.command = command;
        this.path = path;
        this.headers = headers;
    }

    public String getCommand() {
        return command;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public boolean isConnectionKeepAlive() {
        return headers.containsKey("Connection") && headers.get("Connection").equalsIgnoreCase("keep-alive");
    }
}