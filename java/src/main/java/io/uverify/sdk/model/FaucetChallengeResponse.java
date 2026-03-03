package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from {@code POST /api/v1/faucet/request}.
 *
 * <p>The {@code message} field must be signed by the user's wallet (CIP-30 {@code signData})
 * before calling {@code POST /api/v1/faucet/claim}.
 *
 * <p>Only available when the backend is configured with {@code FAUCET_ENABLED=true}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaucetChallengeResponse {

    private String address;
    /** Challenge message the user must sign with their wallet. */
    private String message;
    /** Server signature over the message. */
    private String signature;
    /** UNIX timestamp of the request. */
    private long timestamp;
    private int status;
    private String error;

    public FaucetChallengeResponse() {}

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
