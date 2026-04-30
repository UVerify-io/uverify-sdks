package io.uverify.sdk.exception;

/**
 * Thrown when a transaction cannot be built because the wallet has no spendable UTXOs
 * (HTTP 400, "No UTXOs found for user address").
 *
 * <p>Catch this specifically to trigger a faucet top-up flow rather than retrying:
 *
 * <pre>{@code
 * try {
 *     client.issueCertificates(address, certs);
 * } catch (InsufficientFundsError e) {
 *     client.fundWallet(address);
 * }
 * }</pre>
 */
public class InsufficientFundsError extends UVerifyException {

    public InsufficientFundsError(String message, String responseBody) {
        super(message, 400, responseBody);
    }
}
