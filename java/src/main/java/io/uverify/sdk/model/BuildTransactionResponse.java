package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from {@code POST /api/v1/transaction/build}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildTransactionResponse {

    /** CBOR-hex encoded unsigned transaction, ready to be signed by the user's wallet. */
    @JsonProperty("unsignedTransaction")
    private String unsignedTransaction;

    private String type;

    private BuildStatus status;

    public BuildTransactionResponse() {}

    public String getUnsignedTransaction() { return unsignedTransaction; }
    public void setUnsignedTransaction(String unsignedTransaction) {
        this.unsignedTransaction = unsignedTransaction;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BuildStatus getStatus() { return status; }
    public void setStatus(BuildStatus status) { this.status = status; }

    /**
     * Build status information.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BuildStatus {
        private String message;
        private int code;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
    }
}
