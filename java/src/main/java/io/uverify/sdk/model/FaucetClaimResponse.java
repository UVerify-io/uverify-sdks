package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from {@code POST /api/v1/faucet/claim}.
 *
 * <p>On success, {@link #getTxHash()} contains the Cardano transaction hash of the
 * faucet transfer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaucetClaimResponse {

    @JsonProperty("txHash")
    private String txHash;

    private int status;
    private String error;

    public FaucetClaimResponse() {}

    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
