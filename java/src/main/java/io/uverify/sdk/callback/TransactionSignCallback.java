package io.uverify.sdk.callback;

/**
 * Wallet-agnostic callback for signing a Cardano transaction (CIP-30 {@code api.signTx}).
 *
 * <p>Implement this interface with your preferred Cardano wallet library.
 * The callback receives a CBOR-hex encoded unsigned transaction and must return the
 * CBOR-hex encoded witness set produced by the wallet.
 *
 * <pre>{@code
 * TransactionSignCallback myCallback = unsignedTx -> {
 *     // Example: call your CIP-30 wallet
 *     return wallet.signTx(unsignedTx, true);
 * };
 * }</pre>
 */
@FunctionalInterface
public interface TransactionSignCallback {

    /**
     * Sign the given unsigned transaction with the user's wallet.
     *
     * @param unsignedTx CBOR-hex encoded unsigned transaction.
     * @return CBOR-hex encoded witness set.
     */
    String sign(String unsignedTx);
}
