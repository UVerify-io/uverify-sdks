package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /api/v1/user/state/action}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecuteUserActionRequest {

    private String address;
    private String action;

    @JsonProperty("stateId")
    private String stateId;

    private String message;
    private String signature;

    @JsonProperty("userSignature")
    private String userSignature;

    @JsonProperty("userPublicKey")
    private String userPublicKey;

    private long timestamp;

    public ExecuteUserActionRequest() {}

    /**
     * Convenience constructor that copies fields from a {@link UserActionRequestResponse}
     * and adds the wallet-provided signature fields.
     *
     * @param requestResponse Response from the first step.
     * @param userSignature   CIP-30 wallet signature over the message.
     * @param userPublicKey   Hex-encoded public key used for signing.
     */
    public ExecuteUserActionRequest(
            UserActionRequestResponse requestResponse,
            String userSignature,
            String userPublicKey) {
        this.address = requestResponse.getAddress();
        this.action = requestResponse.getAction();
        this.message = requestResponse.getMessage();
        this.signature = requestResponse.getSignature();
        this.timestamp = requestResponse.getTimestamp();
        this.userSignature = userSignature;
        this.userPublicKey = userPublicKey;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }

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
