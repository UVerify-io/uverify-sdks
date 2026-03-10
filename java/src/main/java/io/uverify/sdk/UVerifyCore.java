package io.uverify.sdk;

import io.uverify.sdk.model.*;

/**
 * Low-level access to the UVerify API primitives.
 *
 * <p>Obtain an instance from {@link UVerifyClient#core}. These methods map
 * one-to-one to the underlying REST endpoints and give you full control over
 * the transaction lifecycle. For common workflows prefer the high-level helpers
 * on {@link UVerifyClient} ({@code issueCertificates}, {@code getUserInfo}, etc.).
 *
 * <pre>{@code
 * UVerifyClient client = UVerifyClient.builder().build();
 *
 * // Build an unsigned transaction
 * BuildTransactionResponse resp = client.core.buildTransaction(
 *     BuildTransactionRequest.defaultRequest("addr1...", "state-id",
 *         new CertificateData("sha256-hash", "SHA-256"))
 * );
 *
 * // Sign externally, then submit
 * String witnessSet = myWallet.signTx(resp.getUnsignedTransaction());
 * client.core.submitTransaction(resp.getUnsignedTransaction(), witnessSet);
 * }</pre>
 */
public interface UVerifyCore {

    /**
     * Build an unsigned transaction for certificate issuance or state management.
     *
     * @param request Build request specifying type, address, certificates, and optional state ID.
     * @return Response containing the CBOR-hex unsigned transaction.
     */
    BuildTransactionResponse buildTransaction(BuildTransactionRequest request);

    /**
     * Submit a signed transaction to the Cardano blockchain.
     *
     * @param transaction CBOR-hex encoded signed transaction.
     * @param witnessSet  Optional separate witness set in CBOR-hex (may be {@code null}).
     * @return The Cardano transaction hash of the submitted transaction.
     */
    String submitTransaction(String transaction, String witnessSet);

    /**
     * Create a server-signed action request that the user must countersign (step 1).
     *
     * @param request Action request specifying address, action type, and optional state ID.
     * @return Response containing the message to be signed by the user's wallet.
     */
    UserActionRequestResponse requestUserAction(UserActionRequest request);

    /**
     * Execute a user state action using both parties' signatures (step 2).
     *
     * @param request Execute request built from the step-1 response plus wallet signatures.
     * @return Action response; may contain updated {@link io.uverify.sdk.model.UserState}.
     */
    ExecuteUserActionResponse executeUserAction(ExecuteUserActionRequest request);

    /**
     * Request a faucet challenge message from the server (step 1 of the faucet flow).
     *
     * <p>The returned {@link FaucetChallengeResponse#getMessage()} must be signed by the
     * user's wallet before calling {@link #claimFaucetFunds}.
     *
     * <p>Only available when the backend is configured with {@code FAUCET_ENABLED=true}.
     *
     * @param address Cardano address that should receive the testnet funds.
     */
    FaucetChallengeResponse requestFaucetChallenge(String address);

    /**
     * Claim testnet ADA using the signed challenge (step 2 of the faucet flow).
     *
     * <p>On success, {@link FaucetClaimResponse#getTxHash()} contains the Cardano transaction
     * hash of the faucet transfer.
     *
     * <p>Only available when the backend is configured with {@code FAUCET_ENABLED=true}.
     *
     * @param request Claim request built from the challenge response plus wallet signature.
     */
    FaucetClaimResponse claimFaucetFunds(FaucetClaimRequest request);
}
