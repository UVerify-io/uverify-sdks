package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Certificate data provided when building a new certification transaction.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificateData {

    /** SHA-256 or SHA-512 hex hash of the data to certify. */
    private String hash;

    /** Optional metadata string to attach to the certificate. */
    private String metadata;

    /** Hashing algorithm, e.g. {@code "SHA-256"} or {@code "SHA-512"}. */
    private String algorithm;

    public CertificateData() {}

    public CertificateData(String hash) {
        this.hash = hash;
    }

    public CertificateData(String hash, String algorithm) {
        this.hash = hash;
        this.algorithm = algorithm;
    }

    public CertificateData(String hash, String algorithm, String metadata) {
        this.hash = hash;
        this.algorithm = algorithm;
        this.metadata = metadata;
    }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
}
