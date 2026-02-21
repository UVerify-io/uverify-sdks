package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Request body for the claim and update connected-goods endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaimUpdateConnectedGoodsRequest {

    private String password;

    @JsonProperty("batch_id")
    private String batchId;

    @JsonProperty("user_address")
    private String userAddress;

    @JsonProperty("social_hub")
    private Map<String, String> socialHub;

    public ClaimUpdateConnectedGoodsRequest() {}

    public ClaimUpdateConnectedGoodsRequest(String password, String batchId, String userAddress) {
        this.password = password;
        this.batchId = batchId;
        this.userAddress = userAddress;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }

    public Map<String, String> getSocialHub() { return socialHub; }
    public void setSocialHub(Map<String, String> socialHub) { this.socialHub = socialHub; }

    /** Convenience method to add a social hub entry. */
    public ClaimUpdateConnectedGoodsRequest withSocialHubEntry(String key, String value) {
        if (this.socialHub == null) this.socialHub = new HashMap<>();
        this.socialHub.put(key, value);
        return this;
    }
}
