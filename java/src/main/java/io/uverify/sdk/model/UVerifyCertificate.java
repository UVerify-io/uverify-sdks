package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;

/**
 * A single certificate stored inside a user's on-chain state datum.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UVerifyCertificate {

    private String hash;
    private String algorithm;
    private String issuer;
    private List<String> extra;

    public UVerifyCertificate() {}

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public List<String> getExtra() { return extra != null ? extra : Collections.emptyList(); }
    public void setExtra(List<String> extra) { this.extra = extra; }

    @Override
    public String toString() {
        return "UVerifyCertificate{hash='" + hash + "', algorithm='" + algorithm + "'}";
    }
}
