package io.uverify.sdk.exception;

/**
 * Thrown by {@link io.uverify.sdk.UVerifyClient#waitFor} when the condition is not met
 * within the specified timeout.
 *
 * <p>Catch this specifically to distinguish a timeout from other errors:
 * <pre>{@code
 * try {
 *     UVerifyClient.waitFor(() -> !client.verify(hash).isEmpty(), 300_000, 5_000);
 * } catch (UVerifyTimeoutException e) {
 *     System.err.println(e.getMessage()); // advises the user to re-run
 * }
 * }</pre>
 */
public class UVerifyTimeoutException extends RuntimeException {

    private final long timeoutMs;

    public UVerifyTimeoutException(long timeoutMs) {
        super(
            "Transaction not confirmed within " + (timeoutMs / 1000) + " seconds. " +
            "The transaction may still be processing — please re-run the script. " +
            "If this happens repeatedly, increase the timeout value."
        );
        this.timeoutMs = timeoutMs;
    }

    /** The timeout value (in milliseconds) that was exceeded. */
    public long getTimeoutMs() {
        return timeoutMs;
    }
}
