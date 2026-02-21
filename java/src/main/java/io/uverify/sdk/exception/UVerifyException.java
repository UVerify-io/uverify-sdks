package io.uverify.sdk.exception;

/**
 * Thrown when the UVerify API returns a non-2xx HTTP response.
 */
public class UVerifyException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    /**
     * @param message      Human-readable error message.
     * @param statusCode   HTTP status code returned by the API.
     * @param responseBody Raw response body (may be {@code null}).
     */
    public UVerifyException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public UVerifyException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    /** HTTP status code returned by the API, or {@code -1} for non-HTTP errors. */
    public int getStatusCode() {
        return statusCode;
    }

    /** Raw response body, or {@code null} if unavailable. */
    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public String toString() {
        return "UVerifyException{statusCode=" + statusCode + ", message=" + getMessage() + "}";
    }
}
