package models;

import java.util.HashMap;
import java.util.Map;

public class HTTPResponse {
    private static final String CRLF = "\r\n";
    private static final Map<Integer, String> CODE_DESCRIPTION = Map.of(
            200, "OK",
            400, "Bad Request",
            404, "Not Found"
    );

    private final int code;
    private final Map<String, String> headers;

    public HTTPResponse(int code, Map<String, String> headers) {
        this.code = code;
        this.headers = headers;
    }

    public String formattedString() {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(code).append(" ").append(CODE_DESCRIPTION.getOrDefault(code, "")).append(CRLF);
        response.append("Server: Too-simple-http-server").append(CRLF);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            response.append(entry.getKey()).append(": ").append(entry.getValue()).append(CRLF);
        }
        response.append(CRLF);
        return response.toString();
    }

    public static HTTPResponse clientError400() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Connection", "close");
        return new HTTPResponse(400, headers);
    }

    public static HTTPResponse notFound404() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Connection", "close");
        return new HTTPResponse(404, headers);
    }
}
