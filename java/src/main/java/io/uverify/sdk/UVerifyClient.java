package io.uverify.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            public void submitTransaction(String transaction, String witnessSet) {
                submitTransactionInternal(transaction, witnessSet);
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

    private void submitTransactionInternal(String transaction, String witnessSet) {
        Map<String, Object> body = new HashMap<>();
        body.put("transaction", transaction);
        if (witnessSet != null) body.put("witnessSet", witnessSet);
        post("/api/v1/transaction/submit", body);
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
     */
    public void issueCertificates(
            String address,
            List<CertificateData> certificates,
            TransactionSignCallback signTx) {
        issueCertificates(address, null, certificates, signTx);
    }

    /**
     * Issue certificates using the bootstrap flow with the constructor-level callback.
     *
     * @throws UVerifyValidationException if no sign callback was configured.
     */
    public void issueCertificates(String address, List<CertificateData> certificates) {
        issueCertificates(address, null, certificates, null);
    }

    /**
     * Issue certificates into an existing state.
     *
     * @param address      Cardano address of the signer.
     * @param stateId      Existing state ID ({@code null} triggers bootstrap flow).
     * @param certificates Certificates to issue.
     * @param signTx       Wallet callback ({@code null} uses constructor-level default).
     */
    public void issueCertificates(
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
        BuildTransactionResponse response = buildTransactionInternal(request);
        String witnessSet = cb.sign(response.getUnsignedTransaction());
        submitTransactionInternal(response.getUnsignedTransaction(), witnessSet);
    }

    /**
     * Issue certificates into an existing state using the constructor-level callback.
     *
     * @throws UVerifyValidationException if no sign callback was configured.
     */
    public void issueCertificates(
            String address, String stateId, List<CertificateData> certificates) {
        issueCertificates(address, stateId, certificates, null);
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
     * returns {@code true}, or throws once {@code timeoutMs} has elapsed.
     *
     * <p>Useful for waiting on eventual-consistency operations such as faucet
     * funds settling on-chain or a submitted certificate becoming queryable.
     *
     * <pre>{@code
     * client.fundWallet("addr_test1...", signMessage);
     * UVerifyClient.waitFor(
     *     () -> !client.verify("sha256-hash").isEmpty(),
     *     120_000,  // 2-minute timeout
     *     2_000     // poll every 2 s
     * );
     * }</pre>
     *
     * @param condition   Returns {@code true} when polling should stop.
     * @param timeoutMs   Maximum wait in milliseconds.
     * @param intervalMs  Delay between polls in milliseconds.
     * @throws Exception               if {@code condition} throws.
     * @throws UVerifyTimeoutException if the timeout is reached.
     */
    public static void waitFor(
            Callable<Boolean> condition,
            long timeoutMs,
            long intervalMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(condition.call())) return;
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
    public static void waitFor(Callable<Boolean> condition) throws Exception {
        waitFor(condition, 60_000, 2_000);
    }
}
