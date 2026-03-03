package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /api/v1/faucet/claim}.
 */
public class FaucetClaimRequest {

    private String address;
    private String message;
    private String signature;

    @JsonProperty("userSignature")
    private String userSignature;

    @JsonProperty("userPublicKey")
    private String userPublicKey;

    private long timestamp;

    public FaucetClaimRequest() {}

    /**
     * Convenience constructor that copies fields from a {@link FaucetChallengeResponse}
     * and adds the wallet-provided signature fields.
     *
     * @param challenge       Response from the challenge step.
     * @param userSignature   CIP-30 wallet signature over the message.
     * @param userPublicKey   Hex-encoded public key used for signing.
     */
    public FaucetClaimRequest(
            FaucetChallengeResponse challenge,
            String userSignature,
            String userPublicKey) {
        this.address = challenge.getAddress();
        this.message = challenge.getMessage();
        this.signature = challenge.getSignature();
        this.timestamp = challenge.getTimestamp();
        this.userSignature = userSignature;
        this.userPublicKey = userPublicKey;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getUserSignature() { return userSignature; }
    public void setUserSignature(String userSignature) { this.userSignature = userSignature; }

    public String getUserPublicKey() { return userPublicKey; }
    public void setUserPublicKey(String userPublicKey) { this.userPublicKey = userPublicKey; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
