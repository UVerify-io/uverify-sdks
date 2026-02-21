package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from {@code POST /api/v1/user/state/action}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecuteUserActionResponse {

    private int status;
    private UserState state;
    private String error;

    @JsonProperty("unsignedTransaction")
    private String unsignedTransaction;

    public ExecuteUserActionResponse() {}

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public UserState getState() { return state; }
    public void setState(UserState state) { this.state = state; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getUnsignedTransaction() { return unsignedTransaction; }
    public void setUnsignedTransaction(String unsignedTransaction) { this.unsignedTransaction = unsignedTransaction; }
}
