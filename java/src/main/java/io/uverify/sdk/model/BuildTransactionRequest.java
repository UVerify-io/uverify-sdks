package io.uverify.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Request body for {@code POST /api/v1/transaction/build}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuildTransactionRequest {

    /**
     * Available transaction types.
     */
    public enum TransactionType {
        DEFAULT("default"),
        BOOTSTRAP("bootstrap"),
        CUSTOM("custom"),
        BURN_STATE("burn_state"),
        BURN_BOOTSTRAP("burn_bootstrap"),
        INIT("init"),
        DEPLOY("deploy");

        private final String value;

        TransactionType(String value) { this.value = value; }

        @Override
        public String toString() { return value; }
    }

    private List<CertificateData> certificates;
    private String type;
    private String address;

    @JsonProperty("bootstrapDatum")
    private Map<String, Object> bootstrapDatum;

    @JsonProperty("stateId")
    private String stateId;

    public BuildTransactionRequest() {}

    /**
     * Build a {@code default} certificate issuance request.
     *
     * @param address      Cardano address of the signer.
     * @param stateId      Existing state ID.
     * @param certificates Certificates to issue.
     */
    public static BuildTransactionRequest defaultRequest(
            String address, String stateId, CertificateData... certificates) {
        BuildTransactionRequest r = new BuildTransactionRequest();
        r.setType(TransactionType.DEFAULT.toString());
        r.setAddress(address);
        r.setStateId(stateId);
        r.setCertificates(Arrays.asList(certificates));
        return r;
    }

    /**
     * Build a {@code bootstrap} request to initialise a new state.
     *
     * @param address         Cardano address of the signer.
     * @param bootstrapDatum  Bootstrap datum reference map.
     * @param certificates    Certificates to include.
     */
    public static BuildTransactionRequest bootstrapRequest(
            String address,
            Map<String, Object> bootstrapDatum,
            CertificateData... certificates) {
        BuildTransactionRequest r = new BuildTransactionRequest();
        r.setType(TransactionType.BOOTSTRAP.toString());
        r.setAddress(address);
        r.setBootstrapDatum(bootstrapDatum);
        r.setCertificates(Arrays.asList(certificates));
        return r;
    }

    public List<CertificateData> getCertificates() {
        return certificates != null ? certificates : Collections.emptyList();
    }
    public void setCertificates(List<CertificateData> certificates) { this.certificates = certificates; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Map<String, Object> getBootstrapDatum() { return bootstrapDatum; }
    public void setBootstrapDatum(Map<String, Object> bootstrapDatum) { this.bootstrapDatum = bootstrapDatum; }

    public String getStateId() { return stateId; }
    public void setStateId(String stateId) { this.stateId = stateId; }
}
