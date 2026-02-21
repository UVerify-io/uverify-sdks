package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /api/v1/user/request/action}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserActionRequest {

    /** Available user state actions. */
    public enum UserAction {
        USER_INFO,
        INVALIDATE_STATE,
        OPT_OUT
    }

    private String address;
    private String action;

    @JsonProperty("stateId")
    private String stateId;

    public UserActionRequest() {}

    public UserActionRequest(String address, UserAction action) {
        this.address = address;
        this.action = action.name();
    }

    public UserActionRequest(String address, UserAction action, String stateId) {
        this(address, action);
        this.stateId = stateId;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public void setAction(UserAction action) { this.action = action.name(); }

    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
}
