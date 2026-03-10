package io.uverify.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.uverify.sdk.apps.UVerifyApps;
import io.uverify.sdk.callback.DataSignature;
import io.uverify.sdk.callback.MessageSignCallback;
import io.uverify.sdk.callback.TransactionSignCallback;
import io.uverify.sdk.exception.UVerifyException;
import io.uverify.sdk.exception.UVerifyTimeoutException;
import io.uverify.sdk.exception.UVerifyValidationException;
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
import java.util.concurrent.Callable;

/**
 * Main entry point for the UVerify SDK.
 *
 * <p><strong>Certificate verification:</strong>
 * <pre>{@code
 * UVerifyClient client = new UVerifyClient();
 * List<CertificateResponse> certs = client.verify("a3b4c5d6...");
 * certs.forEach(c -> System.out.println(c.getTransactionHash()));
 * }</pre>
 *
 * <p><strong>Issuing certificates (high-level helper):</strong>
 * <pre>{@code
 * UVerifyClient client = UVerifyClient.builder()
 *     .signTx(unsignedTx -> myWallet.signTx(unsignedTx))
 *     .build();
 *
 * client.issueCertificates(
 *     "addr1...",
 *     List.of(new CertificateData("sha256-hash", "SHA-256"))
 * );
 * }</pre>
 *
 * <p><strong>Low-level access via {@code .core}:</strong>
 * <pre>{@code
 * BuildTransactionResponse resp = client.core.buildTransaction(
 *     BuildTransactionRequest.defaultRequest("addr1...", "state-id",
 *         new CertificateData("sha256-hash", "SHA-256"))
 * );
 * String witnessSet = myWallet.signTx(resp.getUnsignedTransaction());
 * client.core.submitTransaction(resp.getUnsignedTransaction(), witnessSet);
 * }</pre>
 */
public class UVerifyClient {

    public static final String DEFAULT_BASE_URL = "https://api.preprod.uverify.io";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> defaultHeaders;
    private final MessageSignCallback defaultSignMessage;
    private final TransactionSignCallback defaultSignTx;

    /**
     * Low-level API access. Use when you need full control over the transaction lifecycle.
     * For common workflows prefer the high-level helpers on this class.
     */
    public final UVerifyCore core;

    /**
     * High-level application helpers for issuing well-known certificate types.
     *
     * @see UVerifyApps
     */
    public final UVerifyApps apps;

    private UVerifyClient(Builder builder) {
        this.baseUrl = builder.baseUrl.replaceAll("/$", "");
        this.httpClient = builder.httpClient != null
                ? builder.httpClient
                : HttpClient.newBuilder().connectTimeout(builder.timeout).build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.defaultHeaders = Collections.unmodifiableMap(builder.headers);
        this.defaultSignMessage = builder.signMessage;
        this.defaultSignTx = builder.signTx;

        this.core = new UVerifyCore() {
            @Override
            public BuildTransactionResponse buildTransaction(BuildTransactionRequest request) {
                return buildTransactionInternal(request);
            }

            @Override
            public String submitTransaction(String transaction, String witnessSet) {
                return submitTransactionInternal(transaction, witnessSet);
            }

            @Override
            public UserActionRequestResponse requestUserAction(UserActionRequest request) {
                return requestUserActionInternal(request);
            }

            @Override
            public ExecuteUserActionResponse executeUserAction(ExecuteUserActionRequest request) {
                return executeUserActionInternal(request);
            }

            @Override
            public FaucetChallengeResponse requestFaucetChallenge(String address) {
                return requestFaucetChallengeInternal(address);
            }

            @Override
            public FaucetClaimResponse claimFaucetFunds(FaucetClaimRequest request) {
                return claimFaucetFundsInternal(request);
            }
        };

        String verifyBaseUrl = builder.verifyBaseUrl != null
                ? builder.verifyBaseUrl
                : (this.baseUrl.contains("preprod")
                        ? "https://app.preprod.uverify.io/verify"
                        : "https://app.uverify.io/verify");
        this.apps = new UVerifyApps(
                (addr, certs, signTx) -> issueCertificates(addr, null, certs, signTx),
                verifyBaseUrl);
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
        private MessageSignCallback signMessage;
        private TransactionSignCallback signTx;
        private HttpClient httpClient;
        private String verifyBaseUrl;

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

        /** Add a default header sent with every request. */
        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        /**
         * Set a default {@link MessageSignCallback} for the high-level helpers
         * ({@code getUserInfo}, {@code invalidateState}, {@code optOut}).
         */
        public Builder signMessage(MessageSignCallback signMessage) {
            this.signMessage = signMessage;
            return this;
        }

        /**
         * Set a default {@link TransactionSignCallback} for {@code issueCertificates}.
         */
        public Builder signTx(TransactionSignCallback signTx) {
            this.signTx = signTx;
            return this;
        }

        /**
         * Inject a custom {@link HttpClient} (useful for testing).
         * When set, the {@link #timeout} setting is ignored.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Override the base URL used to construct verification links returned by
         * {@link UVerifyClient#apps}. Defaults to the preprod or production app URL
         * based on the API base URL.
         */
        public Builder verifyBaseUrl(String verifyBaseUrl) {
            this.verifyBaseUrl = verifyBaseUrl;
            return this;
        }

        public UVerifyClient build() {
            return new UVerifyClient(this);
        }
    }

    // -------------------------------------------------------------------------
    // Internal HTTP helpers
    // -------------------------------------------------------------------------

    private <T> T get(String path, Class<T> responseType) {
        return executeRequest(buildGetRequest(path), responseType);
    }

    private <T> T get(String path, TypeReference<T> responseType) {
        return executeRequest(buildGetRequest(path), responseType);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        return executeRequest(buildPostRequest(path, body), responseType);
    }

    private <T> T post(String path, Object body, TypeReference<T> responseType) {
        return executeRequest(buildPostRequest(path, body), responseType);
    }

    private void post(String path, Object body) {
        executeVoidRequest(buildPostRequest(path, body));
    }

    private HttpRequest buildGetRequest(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .header("Accept", "application/json");
        defaultHeaders.forEach(b::header);
        return b.build();
    }

    private HttpRequest buildPostRequest(String path, Object body) {
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new UVerifyException("Failed to serialize request body", e);
        }
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .POST(BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        defaultHeaders.forEach(b::header);
        return b.build();
    }

    private <T> T executeRequest(HttpRequest request, Class<T> responseType) {
        HttpResponse<String> response = sendRequest(request);
        checkStatus(response);
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
        checkStatus(response);
        String body = response.body();
        if (body == null || body.isBlank()) return null;
        try {
            return objectMapper.readValue(body, responseType);
        } catch (IOException e) {
            throw new UVerifyException("Failed to parse response body", e);
        }
    }

    private void executeVoidRequest(HttpRequest request) {
        checkStatus(sendRequest(request));
    }

    private void checkStatus(HttpResponse<String> response) {
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
    // Callback resolution
    // -------------------------------------------------------------------------

    private MessageSignCallback resolveSignMessage(MessageSignCallback perCall) {
        MessageSignCallback cb = perCall != null ? perCall : defaultSignMessage;
        if (cb == null) {
            throw new UVerifyValidationException(
                    "A MessageSignCallback is required. Pass one to the method or set a " +
                    "default via UVerifyClient.builder().signMessage(...).");
        }
        return cb;
    }

    private TransactionSignCallback resolveSignTx(TransactionSignCallback perCall) {
        TransactionSignCallback cb = perCall != null ? perCall : defaultSignTx;
        if (cb == null) {
            throw new UVerifyValidationException(
                    "A TransactionSignCallback is required. Pass one to the method or set a " +
                    "default via UVerifyClient.builder().signTx(...).");
        }
        return cb;
    }

    // -------------------------------------------------------------------------
    // Private low-level methods (exposed via .core)
    // -------------------------------------------------------------------------

    private BuildTransactionResponse buildTransactionInternal(BuildTransactionRequest request) {
        return post("/api/v1/transaction/build", request, BuildTransactionResponse.class);
    }

    @SuppressWarnings("unchecked")
    private String submitTransactionInternal(String transaction, String witnessSet) {
        Map<String, Object> body = new HashMap<>();
        body.put("transaction", transaction);
        if (witnessSet != null) body.put("witnessSet", witnessSet);
        Map<String, Object> result = post(
                "/api/v1/transaction/submit", body,
                new TypeReference<Map<String, Object>>() {});
        Object hash = result != null ? result.getOrDefault("transactionHash", result.get("value")) : null;
        if (hash == null) {
            throw new UVerifyValidationException("Submit endpoint did not return a transaction hash");
        }
        return (String) hash;
    }

    private UserActionRequestResponse requestUserActionInternal(UserActionRequest request) {
        return post("/api/v1/user/request/action", request, UserActionRequestResponse.class);
    }

    private ExecuteUserActionResponse executeUserActionInternal(ExecuteUserActionRequest request) {
        return post("/api/v1/user/state/action", request, ExecuteUserActionResponse.class);
    }

    private FaucetChallengeResponse requestFaucetChallengeInternal(String address) {
        Map<String, Object> body = new HashMap<>();
        body.put("address", address);
        return post("/api/v1/faucet/request", body, FaucetChallengeResponse.class);
    }

    private FaucetClaimResponse claimFaucetFundsInternal(FaucetClaimRequest request) {
        return post("/api/v1/faucet/claim", request, FaucetClaimResponse.class);
    }

    private ExecuteUserActionResponse performUserStateAction(
            String address,
            UserActionRequest.UserAction action,
            MessageSignCallback signMessage,
            String stateId) {
        MessageSignCallback cb = resolveSignMessage(signMessage); // validate before any HTTP call
        UserActionRequest req = stateId != null
                ? new UserActionRequest(address, action, stateId)
                : new UserActionRequest(address, action);
        UserActionRequestResponse requestResponse = requestUserActionInternal(req);
        DataSignature sig = cb.sign(requestResponse.getMessage());
        ExecuteUserActionRequest execReq = new ExecuteUserActionRequest(
                requestResponse, sig.getSignature(), sig.getKey());
        return executeUserActionInternal(execReq);
    }

    // -------------------------------------------------------------------------
    // Certificate Verification
    // -------------------------------------------------------------------------

    /**
     * Retrieve all on-chain certificates for the given data hash.
     *
     * @param hash SHA-256 or SHA-512 hex hash of the data to look up.
     * @return List of certificates; empty list if none found.
     */
    public List<CertificateResponse> verify(String hash) {
        try {
            List<CertificateResponse> result = get(
                    "/api/v1/verify/" + hash,
                    new TypeReference<List<CertificateResponse>>() {});
            return result != null ? result : Collections.emptyList();
        } catch (UVerifyException e) {
            if (e.getStatusCode() == 404) return Collections.emptyList();
            throw e;
        }
    }

    /**
     * Retrieve a single certificate by its Cardano transaction hash and data hash.
     *
     * @param transactionHash 64-character hex transaction hash.
     * @param dataHash        SHA-256 or SHA-512 hex hash of the certified data.
     */
    public CertificateResponse verifyByTransaction(String transactionHash, String dataHash) {
        return get(
                "/api/v1/verify/by-transaction-hash/" + transactionHash + "/" + dataHash,
                CertificateResponse.class);
    }

    // -------------------------------------------------------------------------
    // High-level helpers
    // -------------------------------------------------------------------------

    /**
     * Issue certificates using the bootstrap (new state) flow.
     *
     * @param address      Cardano address of the signer.
     * @param certificates Certificates to issue.
     * @param signTx       Wallet callback to sign the unsigned transaction.
     * @return The Cardano transaction hash of the submitted transaction.
     */
    public String issueCertificates(
            String address,
            List<CertificateData> certificates,
            TransactionSignCallback signTx) {
        return issueCertificates(address, null, certificates, signTx);
    }

    /**
     * Issue certificates using the bootstrap flow with the constructor-level callback.
     *
     * @return The Cardano transaction hash of the submitted transaction.
     * @throws UVerifyValidationException if no sign callback was configured.
     */
    public String issueCertificates(String address, List<CertificateData> certificates) {
        return issueCertificates(address, null, certificates, null);
    }

    /**
     * Issue certificates into an existing state.
     *
     * @param address      Cardano address of the signer.
     * @param stateId      Existing state ID ({@code null} triggers bootstrap flow).
     * @param certificates Certificates to issue.
     * @param signTx       Wallet callback ({@code null} uses constructor-level default).
     * @return The Cardano transaction hash of the submitted transaction.
     */
    public String issueCertificates(
            String address,
            String stateId,
            List<CertificateData> certificates,
            TransactionSignCallback signTx) {
        TransactionSignCallback cb = resolveSignTx(signTx);
        BuildTransactionRequest request = stateId != null
                ? BuildTransactionRequest.defaultRequest(
                        address, stateId, certificates.toArray(new CertificateData[0]))
                : BuildTransactionRequest.bootstrapRequest(
                        address, null, certificates.toArray(new CertificateData[0]));
        int maxAttempts = 3;
        UVerifyException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                BuildTransactionResponse response = buildTransactionInternal(request);
                String witnessSet = cb.sign(response.getUnsignedTransaction());
                return submitTransactionInternal(response.getUnsignedTransaction(), witnessSet);
            } catch (UVerifyException e) {
                lastError = e;
                if (attempt < maxAttempts) {
                    System.out.printf("Certificate issuance failed (attempt %d/%d), retrying in 5 s (waiting for chain state to propagate) …%n",
                            attempt, maxAttempts);
                    try {
                        Thread.sleep(5_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw lastError;
    }

    /**
     * Issue certificates into an existing state using the constructor-level callback.
     *
     * @return The Cardano transaction hash of the submitted transaction.
     * @throws UVerifyValidationException if no sign callback was configured.
     */
    public String issueCertificates(
            String address, String stateId, List<CertificateData> certificates) {
        return issueCertificates(address, stateId, certificates, null);
    }

    /**
     * Retrieve the user's on-chain state (two-step signed action).
     *
     * @param address     Cardano address of the user.
     * @param signMessage Wallet callback to countersign the server's challenge.
     */
    public ExecuteUserActionResponse getUserInfo(String address, MessageSignCallback signMessage) {
        return performUserStateAction(
                address, UserActionRequest.UserAction.USER_INFO, signMessage, null);
    }

    /**
     * Retrieve the user's on-chain state using the constructor-level callback.
     *
     * @throws UVerifyValidationException if no sign callback was configured.
     */
    public ExecuteUserActionResponse getUserInfo(String address) {
        return getUserInfo(address, null);
    }

    /**
     * Invalidate a specific on-chain state.
     *
     * @param address     Cardano address of the user.
     * @param stateId     ID of the state to invalidate.
     * @param signMessage Wallet callback to countersign the server's challenge.
     */
    public ExecuteUserActionResponse invalidateState(
            String address, String stateId, MessageSignCallback signMessage) {
        return performUserStateAction(
                address, UserActionRequest.UserAction.INVALIDATE_STATE, signMessage, stateId);
    }

    /**
     * Invalidate a specific on-chain state using the constructor-level callback.
     *
     * @throws UVerifyValidationException if no sign callback was configured.
     */
    public ExecuteUserActionResponse invalidateState(String address, String stateId) {
        return invalidateState(address, stateId, null);
    }

    /**
     * Opt out from a specific on-chain state.
     *
     * @param address     Cardano address of the user.
     * @param stateId     ID of the state to opt out from.
     * @param signMessage Wallet callback to countersign the server's challenge.
     */
    public ExecuteUserActionResponse optOut(
            String address, String stateId, MessageSignCallback signMessage) {
        return performUserStateAction(
                address, UserActionRequest.UserAction.OPT_OUT, signMessage, stateId);
    }

    /**
     * Opt out from a specific on-chain state using the constructor-level callback.
     *
     * @throws UVerifyValidationException if no sign callback was configured.
     */
    public ExecuteUserActionResponse optOut(String address, String stateId) {
        return optOut(address, stateId, null);
    }

    /**
     * Request testnet ADA from the UVerify dev faucet in a single step.
     *
     * <p>Orchestrates the full challenge-sign-claim flow:
     * <ol>
     *   <li>Requests a server-signed challenge for {@code address}.
     *   <li>Passes the challenge message to {@code signMessage} (CIP-30 {@code signData}).
     *   <li>Submits both signatures to the faucet claim endpoint.
     * </ol>
     *
     * <p>Returns a {@link FaucetClaimResponse} with the Cardano transaction hash on success.
     *
     * <p><strong>Only available when the backend is configured with
     * {@code FAUCET_ENABLED=true}.</strong>
     * This is intended for testnet development environments only.
     *
     * <pre>{@code
     * UVerifyClient client = UVerifyClient.builder()
     *     .baseUrl("http://localhost:9090")
     *     .signMessage(msg -> myWallet.signData(address, msg))
     *     .build();
     *
     * FaucetClaimResponse result = client.fundWallet("addr_test1...");
     * System.out.println("Funded by tx: " + result.getTxHash());
     * }</pre>
     *
     * <p>Use {@link #waitFor} to poll until the funds are queryable on-chain.
     *
     * @param address     Cardano address that should receive the testnet funds.
     * @param signMessage Wallet callback to sign the challenge; uses the constructor-level
     *                    default when {@code null}.
     * @throws UVerifyValidationException if no sign callback is available.
     */
    public FaucetClaimResponse fundWallet(String address, MessageSignCallback signMessage) {
        MessageSignCallback cb = resolveSignMessage(signMessage);
        try {
            FaucetChallengeResponse challenge = requestFaucetChallengeInternal(address);
            DataSignature sig = cb.sign(challenge.getMessage());
            return claimFaucetFundsInternal(new FaucetClaimRequest(challenge, sig.getSignature(), sig.getKey()));
        } catch (UVerifyException e) {
            if (e.getStatusCode() == 404) {
                throw new UVerifyException(
                        "Faucet endpoint not found (HTTP 404). " +
                        "The faucet is only available on backends started with FAUCET_ENABLED=true. " +
                        "This feature does not exist on mainnet — acquire ADA from a cryptocurrency exchange instead.",
                        404, e.getResponseBody());
            }
            if (e.getStatusCode() == 429) {
                throw new UVerifyException(
                        "Faucet cooldown active (HTTP 429). " +
                        "This address recently received testnet funds. Please wait a few minutes before trying again.",
                        429, e.getResponseBody());
            }
            throw e;
        }
    }

    /**
     * Fund a wallet with testnet ADA using the constructor-level callback.
     *
     * @throws UVerifyValidationException if no sign callback was configured.
     */
    public FaucetClaimResponse fundWallet(String address) {
        return fundWallet(address, null);
    }

    // -------------------------------------------------------------------------
    // Polling utility
    // -------------------------------------------------------------------------

    /**
     * Polls {@code condition} every {@code intervalMs} milliseconds until it
     * returns a non-{@code null} value, then returns that value. Throws
     * {@link UVerifyTimeoutException} once {@code timeoutMs} has elapsed.
     *
     * <p>Return {@code null} from the condition to keep polling; return any
     * other value (including {@link Boolean#TRUE}) to stop and return it.
     * Exceptions thrown by the condition propagate immediately without retrying.
     *
     * <pre>{@code
     * // Poll until certificates appear, return them
     * List<CertificateResponse> certs = UVerifyClient.waitFor(
     *     () -> {
     *         var c = client.verify("sha256-hash");
     *         return c.isEmpty() ? null : c;
     *     },
     *     300_000, 2_000
     * );
     * }</pre>
     *
     * @param <T>         The type of value returned when polling succeeds.
     * @param condition   Returns {@code null} to keep polling, or any non-null
     *                    value to stop.
     * @param timeoutMs   Maximum wait in milliseconds.
     * @param intervalMs  Delay between polls in milliseconds.
     * @return The first non-{@code null} value returned by {@code condition}.
     * @throws Exception               if {@code condition} throws.
     * @throws UVerifyTimeoutException if the timeout is reached.
     */
    public static <T> T waitFor(
            Callable<T> condition,
            long timeoutMs,
            long intervalMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            T result = condition.call();
            if (result != null) return result;
            Thread.sleep(intervalMs);
        }
        throw new UVerifyTimeoutException(timeoutMs);
    }

    /**
     * Overload with defaults: 60-second timeout, 2-second poll interval.
     *
     * @throws Exception               if {@code condition} throws.
     * @throws UVerifyTimeoutException if the timeout is reached.
     */
    public static <T> T waitFor(Callable<T> condition) throws Exception {
        return waitFor(condition, 60_000, 2_000);
    }
}
