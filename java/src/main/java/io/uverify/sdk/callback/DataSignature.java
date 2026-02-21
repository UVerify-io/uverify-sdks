package io.uverify.sdk.callback;

/**
 * Holds the CIP-30 {@code signData} result: a public key and a signature, both
 * in hex-encoded CBOR format.
 */
public final class DataSignature {

    private final String key;
    private final String signature;

    /**
     * @param key       Hex-encoded CBOR public key returned by the CIP-30 wallet.
     * @param signature Hex-encoded CBOR signature returned by the CIP-30 wallet.
     */
    public DataSignature(String key, String signature) {
        this.key = key;
        this.signature = signature;
    }

    /** Hex-encoded CBOR public key. */
    public String getKey() {
        return key;
    }

    /** Hex-encoded CBOR signature. */
    public String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return "DataSignature{key='" + key + "'}";
    }
}
