package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * On-chain state datum representing a user's issuing workspace.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserState {

    private String id;
    private String owner;
    private long fee;

    @JsonProperty("feeInterval")
    private long feeInterval;

    @JsonProperty("feeReceivers")
    private List<String> feeReceivers;

    private long ttl;
    private long countdown;
    private List<UVerifyCertificate> certificates;

    @JsonProperty("batchSize")
    private int batchSize;

    @JsonProperty("bootstrapDatumName")
    private String bootstrapDatumName;

    @JsonProperty("keepAsOracle")
    private boolean keepAsOracle;

    public UserState() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public long getFee() { return fee; }
    public void setFee(long fee) { this.fee = fee; }

    public long getFeeInterval() { return feeInterval; }
    public void setFeeInterval(long feeInterval) { this.feeInterval = feeInterval; }

    public List<String> getFeeReceivers() { return feeReceivers != null ? feeReceivers : Collections.emptyList(); }
    public void setFeeReceivers(List<String> feeReceivers) { this.feeReceivers = feeReceivers; }

    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }

    public long getCountdown() { return countdown; }
    public void setCountdown(long countdown) { this.countdown = countdown; }

    public List<UVerifyCertificate> getCertificates() { return certificates != null ? certificates : Collections.emptyList(); }
    public void setCertificates(List<UVerifyCertificate> certificates) { this.certificates = certificates; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public String getBootstrapDatumName() { return bootstrapDatumName; }
    public void setBootstrapDatumName(String bootstrapDatumName) { this.bootstrapDatumName = bootstrapDatumName; }

    public boolean isKeepAsOracle() { return keepAsOracle; }
    public void setKeepAsOracle(boolean keepAsOracle) { this.keepAsOracle = keepAsOracle; }
}
