package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from {@code POST /api/v1/extension/connected-goods/mint/batch}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MintConnectedGoodsResponse {

    private String message;
    private int status;
    private String error;

    @JsonProperty("unsigned_transaction")
    private String unsignedTransaction;

    @JsonProperty("batch_id")
    private String batchId;

    public MintConnectedGoodsResponse() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    /** CBOR-hex unsigned transaction to mint the batch. */
    public String getUnsignedTransaction() { return unsignedTransaction; }
    public void setUnsignedTransaction(String unsignedTransaction) {
        this.unsignedTransaction = unsignedTransaction;
    }

    /** Batch ID used in subsequent claim / update calls. */
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
}
