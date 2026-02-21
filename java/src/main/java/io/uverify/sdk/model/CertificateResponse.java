package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single certificate as returned by the verification endpoints.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificateResponse {

    private String hash;
    private String address;

    @JsonProperty("blockHash")
    private String blockHash;

    @JsonProperty("blockNumber")
    private long blockNumber;

    @JsonProperty("transactionHash")
    private String transactionHash;

    private long slot;

    @JsonProperty("creationTime")
    private String creationTime;

    private String metadata;
    private String issuer;

    public CertificateResponse() {}

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBlockHash() { return blockHash; }
    public void setBlockHash(String blockHash) { this.blockHash = blockHash; }

    public long getBlockNumber() { return blockNumber; }
    public void setBlockNumber(long blockNumber) { this.blockNumber = blockNumber; }

    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }

    public long getSlot() { return slot; }
    public void setSlot(long slot) { this.slot = slot; }

    public String getCreationTime() { return creationTime; }
    public void setCreationTime(String creationTime) { this.creationTime = creationTime; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    @Override
    public String toString() {
        return "CertificateResponse{hash='" + hash + "', transactionHash='" + transactionHash
                + "', creationTime='" + creationTime + "'}";
    }
}
