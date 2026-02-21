package io.uverify.sdk.callback;

/**
 * Wallet-agnostic callback for signing a message (CIP-30 {@code api.signData}).
 *
 * <p>Implement this interface with your preferred Cardano wallet library.
 * The callback receives the raw message string returned by the UVerify API and
 * must return a {@link DataSignature} containing the CBOR-encoded public key and
 * signature.
 *
 * <pre>{@code
 * MessageSignCallback myCallback = message -> {
 *     // Example: call your CIP-30 wallet
 *     String key  = wallet.signData(address, message).key();
 *     String sig  = wallet.signData(address, message).signature();
 *     return new DataSignature(key, sig);
 * };
 * }</pre>
 */
@FunctionalInterface
public interface MessageSignCallback {

    /**
     * Sign the given message with the user's wallet.
     *
     * @param message Raw message string to sign.
     * @return {@link DataSignature} containing the public key and signature.
     */
    DataSignature sign(String message);
}
