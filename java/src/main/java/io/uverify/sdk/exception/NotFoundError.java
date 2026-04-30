package io.uverify.sdk.exception;

/**
 * Thrown when the UVerify API returns HTTP 404 (resource not found).
 *
 * <p>When thrown from {@link io.uverify.sdk.UVerifyClient#fundWallet}, this indicates
 * the faucet endpoint is not available on this backend deployment.
 */
public class NotFoundError extends UVerifyException {

    public NotFoundError(String message, String responseBody) {
        super(message, 404, responseBody);
    }
}
