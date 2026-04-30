package io.uverify.sdk.exception;

/**
 * Thrown when the UVerify API returns HTTP 429 (rate limit / cooldown active).
 *
 * <pre>{@code
 * try {
 *     client.fundWallet("addr_test1...");
 * } catch (RateLimitError e) {
 *     System.out.println("Faucet cooldown active, try again later.");
 * }
 * }</pre>
 */
public class RateLimitError extends UVerifyException {

    public RateLimitError(String message, String responseBody) {
        super(message, 429, responseBody);
    }
}
