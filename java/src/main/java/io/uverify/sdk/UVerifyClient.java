package io.uverify.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.uverify.sdk.exception.UVerifyException;
import io.uverify.sdk.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry point for the UVerify SDK.
 *
 * <p>Example usage:
 * <pre>{@code
 * UVerifyClient client = new UVerifyClient();
 *
 * List<CertificateResponse> certs = client.verify("a3b4c5d6...");
 * certs.forEach(c -> System.out.println(c.getTransactionHash()));
 * }</pre>
 *
 * <p>To connect to a self-hosted instance:
 * <pre>{@code
 * UVerifyClient client = UVerifyClient.builder()
 *     .baseUrl("https://my-instance.example.com")
 *     .timeout(Duration.ofSeconds(60))
 *     .build();
 * }</pre>
 */
public class UVerifyClient {

    public static final String DEFAULT_BASE_URL = "https://api.uverify.io";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> defaultHeaders;

    private UVerifyClient(Builder builder) {
        this.baseUrl = builder.baseUrl.replaceAll("/$", "");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(builder.timeout)
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.defaultHeaders = Collections.unmodifiableMap(builder.headers);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** Create a new {@link Builder} for constructing a {@code UVerifyClient}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Construct a client with all defaults (connects to the public API). */
    public UVerifyClient() {
        this(new Builder());
    }

    public static final class Builder {
        private String baseUrl = DEFAULT_BASE_URL;
        private Duration timeout = Duration.ofSeconds(30);
        private final Map<String, String> headers = new HashMap<>();

        private Builder() {}

        /** Override the API base URL (e.g. for a self-hosted instance). */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** Set the HTTP request timeout. Defaults to 30 seconds. */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /** Add a default header to every request. */
        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public UVerifyClient build() {
            return new UVerifyClient(this);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private <T> T get(String path, Class<T> responseType) {
        HttpRequest request = buildGetRequest(path);
        return executeRequest(request, responseType);
    }

    private <T> T get(String path, TypeReference<T> responseType) {
        HttpRequest request = buildGetRequest(path);
        return executeRequest(request, responseType);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        HttpRequest request = buildPostRequest(path, body);
        return executeRequest(request, responseType);
    }

    private void post(String path, Object body) {
        HttpRequest request = buildPostRequest(path, body);
        executeVoidRequest(request);
    }

    private HttpRequest buildGetRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .header("Accept", "application/json");
        defaultHeaders.forEach(builder::header);
        return builder.build();
    }

    private HttpRequest buildPostRequest(String path, Object body) {
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new UVerifyException("Failed to serialize request body", e);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .POST(BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        defaultHeaders.forEach(builder::header);
        return builder.build();
    }

    private <T> T executeRequest(HttpRequest request, Class<T> responseType) {
        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new UVerifyException(
                    "UVerify API error " + response.statusCode(),
                    response.statusCode(),
                    response.body());
        }
        String body = response.body();
        if (body == null || body.isBlank()) return null;
        try {
            return objectMapper.readValue(body, responseType);
        } catch (IOException e) {
            throw new UVerifyException("Failed to parse response body", e);
        }
    }

    private <T> T executeRequest(HttpRequest request, TypeReference<T> responseType) {
        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new UVerifyException(
                    "UVerify API error " + response.statusCode(),
                    response.statusCode(),
                    response.body());
        }
        String body = response.body();
        if (body == null || body.isBlank()) return null;
        try {
            return objectMapper.readValue(body, responseType);
        } catch (IOException e) {
            throw new UVerifyException("Failed to parse response body", e);
        }
    }

    private void executeVoidRequest(HttpRequest request) {
        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new UVerifyException(
                    "UVerify API error " + response.statusCode(),
                    response.statusCode(),
                    response.body());
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            return httpClient.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new UVerifyException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Certificate Verification
    // -------------------------------------------------------------------------

    /**
     * Retrieve all on-chain certificates associated with the given data hash.
     *
     * @param hash SHA-256 or SHA-512 hex hash of the data to look up.
     * @return List of certificates; empty list if none found.
     * @throws UVerifyException if the API returns an error.
     *
     * <pre>{@code
     * List<CertificateResponse> certs = client.verify("a3b4c5d6...");
     * certs.forEach(c -> System.out.println(c.getTransactionHash()));
     * }</pre>
     */
    public List<CertificateResponse> verify(String hash) {
        List<CertificateResponse> result = get(
                "/api/v1/verify/" + hash,
                new TypeReference<List<CertificateResponse>>() {});
        return result != null ? result : Collections.emptyList();
    }

    /**
     * Retrieve a single certificate by its Cardano transaction hash and data hash.
     *
     * @param transactionHash 64-character hex transaction hash.
     * @param dataHash        SHA-256 or SHA-512 hex hash of the certified data.
     * @return The matching certificate.
     */
    public CertificateResponse verifyByTransaction(String transactionHash, String dataHash) {
        return get(
                "/api/v1/verify/by-transaction-hash/" + transactionHash + "/" + dataHash,
                CertificateResponse.class);
    }

    // -------------------------------------------------------------------------
    // Transaction Management
    // -------------------------------------------------------------------------

    /**
     * Build an unsigned transaction for certificate issuance or state management.
     *
     * <p>The returned {@code unsignedTransaction} is a CBOR-hex encoded transaction
     * that must be signed by the user's wallet before being submitted via
     * {@link #submitTransaction(String)}.
     *
     * <pre>{@code
     * BuildTransactionResponse response = client.buildTransaction(
     *     BuildTransactionRequest.defaultRequest(
     *         "addr1...",
     *         "my-state-id",
     *         new CertificateData("sha256-hash", "SHA-256")
     *     )
     * );
     * String unsignedTx = response.getUnsignedTransaction();
     * }</pre>
     */
    public BuildTransactionResponse buildTransaction(BuildTransactionRequest request) {
        return post("/api/v1/transaction/build", request, BuildTransactionResponse.class);
    }

    /**
     * Submit a signed transaction to the Cardano blockchain.
     *
     * @param transaction CBOR-hex encoded signed transaction.
     */
    public void submitTransaction(String transaction) {
        submitTransaction(transaction, null);
    }

    /**
     * Submit a signed transaction with a separate witness set.
     *
     * @param transaction CBOR-hex encoded signed transaction.
     * @param witnessSet  Optional separate witness set in CBOR-hex.
     */
    public void submitTransaction(String transaction, String witnessSet) {
        Map<String, Object> body = new HashMap<>();
        body.put("transaction", transaction);
        if (witnessSet != null) body.put("witnessSet", witnessSet);
        post("/api/v1/transaction/submit", body);
    }

    // -------------------------------------------------------------------------
    // User State Management
    // -------------------------------------------------------------------------

    /**
     * Create a server-signed action request that the user must countersign.
     *
     * <p>This is step 1 of the two-step user-state-action flow.
     * Pass the returned {@code message} to your wallet for signing, then call
     * {@link #executeUserAction(ExecuteUserActionRequest)}.
     *
     * <pre>{@code
     * UserActionRequestResponse req = client.requestUserAction(
     *     new UserActionRequest("addr1...", UserActionRequest.UserAction.USER_INFO)
     * );
     * // Sign req.getMessage() with your CIP-30 wallet...
     * }</pre>
     */
    public UserActionRequestResponse requestUserAction(UserActionRequest request) {
        return post("/api/v1/user/request/action", request, UserActionRequestResponse.class);
    }

    /**
     * Execute a user state action using signatures from both parties.
     *
     * <p>This is step 2. Use the convenience constructor
     * {@link ExecuteUserActionRequest#ExecuteUserActionRequest(UserActionRequestResponse, String, String)}
     * to build the request from the first step's response.
     */
    public Map<String, Object> executeUserAction(ExecuteUserActionRequest request) {
        return post("/api/v1/user/state/action", request,
                new TypeReference<Map<String, Object>>() {});
    }

    // -------------------------------------------------------------------------
    // Connected Goods Extension
    // -------------------------------------------------------------------------

    /**
     * Build an unsigned transaction to mint a batch of connected-goods tokens.
     *
     * <pre>{@code
     * MintConnectedGoodsResponse resp = client.mintConnectedGoodsBatch(
     *     new MintConnectedGoodsRequest(
     *         "addr1...", "MY_ITEM",
     *         new MintConnectedGoodsRequest.Item("secret1", "ITEM001"),
     *         new MintConnectedGoodsRequest.Item("secret2", "ITEM002")
     *     )
     * );
     * String batchId = resp.getBatchId();
     * }</pre>
     */
    public MintConnectedGoodsResponse mintConnectedGoodsBatch(MintConnectedGoodsRequest request) {
        return post("/api/v1/extension/connected-goods/mint/batch", request,
                MintConnectedGoodsResponse.class);
    }

    /** Claim a connected-goods item using its password. */
    public void claimConnectedGoodsItem(ClaimUpdateConnectedGoodsRequest request) {
        post("/api/v1/extension/connected-goods/claim/item", request);
    }

    /** Update the social profile associated with a claimed item. */
    public void updateConnectedGoodsItem(ClaimUpdateConnectedGoodsRequest request) {
        post("/api/v1/extension/connected-goods/update/item", request);
    }

    /**
     * Retrieve a connected-goods item.
     *
     * @param batchIds Comma-separated batch IDs.
     * @param itemId   Item identifier within the batch.
     */
    public Map<String, Object> getConnectedGoodsItem(String batchIds, String itemId) {
        return get("/api/v1/extension/connected-goods/" + batchIds + "/" + itemId,
                new TypeReference<Map<String, Object>>() {});
    }

    // -------------------------------------------------------------------------
    // Transaction Service
    // -------------------------------------------------------------------------

    /** Retrieve all contract scripts used in a transaction. */
    public List<Map<String, Object>> getTransactionScripts(String txHash) {
        List<Map<String, Object>> result = get("/api/v1/txs/" + txHash + "/scripts",
                new TypeReference<List<Map<String, Object>>>() {});
        return result != null ? result : Collections.emptyList();
    }

    /** Retrieve redeemer data from a transaction. */
    public List<Map<String, Object>> getTransactionRedeemers(String txHash) {
        List<Map<String, Object>> result = get("/api/v1/txs/" + txHash + "/redeemers",
                new TypeReference<List<Map<String, Object>>>() {});
        return result != null ? result : Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // Script Service
    // -------------------------------------------------------------------------

    /** Get basic information about a script. */
    public Map<String, Object> getScript(String scriptHash) {
        return get("/api/v1/scripts/" + scriptHash,
                new TypeReference<Map<String, Object>>() {});
    }

    /** Get the JSON representation of a script. */
    public Map<String, Object> getScriptJson(String scriptHash) {
        return get("/api/v1/scripts/" + scriptHash + "/json",
                new TypeReference<Map<String, Object>>() {});
    }

    /** Get detailed information about a script including its content. */
    public Map<String, Object> getScriptDetails(String scriptHash) {
        return get("/api/v1/scripts/" + scriptHash + "/details",
                new TypeReference<Map<String, Object>>() {});
    }

    /** Get the CBOR encoding of a script. */
    public Map<String, Object> getScriptCbor(String scriptHash) {
        return get("/api/v1/scripts/" + scriptHash + "/cbor",
                new TypeReference<Map<String, Object>>() {});
    }

    /** Get a datum by its hash. */
    public Object getDatum(String datumHash) {
        return get("/api/v1/scripts/datum/" + datumHash, Object.class);
    }

    /** Get the CBOR encoding of a datum by its hash. */
    public Object getDatumCbor(String datumHash) {
        return get("/api/v1/scripts/datum/" + datumHash + "/cbor", Object.class);
    }

    // -------------------------------------------------------------------------
    // Library Controller
    // -------------------------------------------------------------------------

    /** Deploy a proxy contract for state management. */
    public void deployProxy() {
        post("/api/v1/library/deploy/proxy", Collections.emptyMap());
    }

    /** Upgrade an existing proxy contract. */
    public void upgradeProxy(String params) {
        post("/api/v1/library/upgrade/proxy", params);
    }

    /** List all current library deployments. */
    public Object getDeployments() {
        return get("/api/v1/library/deployments", Object.class);
    }

    /**
     * Undeploy a specific contract.
     *
     * @param transactionHash Transaction hash of the deployment.
     * @param outputIndex     Output index of the deployment UTXO.
     */
    public Object undeployContract(String transactionHash, int outputIndex) {
        return get("/api/v1/library/undeploy/" + transactionHash + "/" + outputIndex,
                Object.class);
    }

    /** Undeploy all unused contract instances. */
    public Object undeployUnused() {
        return get("/api/v1/library/undeploy/unused", Object.class);
    }

    // -------------------------------------------------------------------------
    // Statistics
    // -------------------------------------------------------------------------

    /** Get transaction fee statistics. */
    public Object getTransactionFees() {
        return get("/api/v1/statistic/tx-fees", Object.class);
    }

    /** Get certificate counts grouped by category. */
    public Object getCertificatesByCategory() {
        return get("/api/v1/statistic/certificate/by-category", Object.class);
    }

    // -------------------------------------------------------------------------
    // Helpers for TypeReference-based deserialization (private overload)
    // -------------------------------------------------------------------------

    private <T> T post(String path, Object body, TypeReference<T> responseType) {
        HttpRequest request = buildPostRequest(path, body);
        return executeRequest(request, responseType);
    }
}
