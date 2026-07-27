package io.github.lijinhong11.nexusmcpublisher;

public class NexusMCApiException extends RuntimeException {
    private final int statusCode;

    public NexusMCApiException(int statusCode, String responseBody) {
        super("NexusMC API request failed with HTTP " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
    }

    public NexusMCApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int getStatusCode() {
        return statusCode;
    }
}