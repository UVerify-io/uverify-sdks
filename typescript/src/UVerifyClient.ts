import { UVerifyApiError, UVerifyValidationError, WaitForTimeoutError } from './errors.js';
import type {
  CertificateData,
  CertificateResponse,
  BuildTransactionRequest,
  BuildTransactionResponse,
  UserAction,
  UserActionRequest,
  UserActionRequestResponse,
  ExecuteUserActionRequest,
  ExecuteUserActionResponse,
  UserState,
  FaucetChallengeResponse,
  FaucetClaimRequest,
  FaucetClaimResponse,
} from './types/index.js';

/**
 * Callback used by high-level helpers for message-based user state actions.
 *
 * Receives the raw challenge message string from the server and must return
 * a CIP-30-compatible data signature object (`{ key, signature }`).
 *
 * The callback is wallet-agnostic — pass any implementation that speaks
 * CIP-30 (e.g. mesh.js, blaze, lucid, or a raw browser wallet API).
 *
 * @example CIP-30 browser wallet
 * ```ts
 * const signMessage: MessageSignCallback = async (message) => {
 *   const hexAddress = await api.getChangeAddress();
 *   const hexMessage = Array.from(message)
 *     .map((c) => c.charCodeAt(0).toString(16).padStart(2, '0'))
 *     .join('');
 *   return api.signData(hexAddress, hexMessage); // { key, signature }
 * };
 * ```
 */
export type MessageSignCallback = (
  message: string
) => Promise<{ key: string; signature: string }>;

/**
 * Callback used by high-level helpers that require signing a transaction.
 *
 * Receives the CBOR-hex unsigned transaction and must return the CBOR-hex
 * witness set after signing.
 *
 * @example CIP-30 browser wallet
 * ```ts
 * const signTx: TransactionSignCallback = (unsignedTx) =>
 *   api.signTx(unsignedTx, true); // partial = true
 * ```
 *
 * @example mesh.js
 * ```ts
 * const signTx: TransactionSignCallback = (unsignedTx) =>
 *   wallet.signTx(unsignedTx, true);
 * ```
 */
export type TransactionSignCallback = (unsignedTx: string) => Promise<string>;

const DEFAULT_BASE_URL = 'https://api.preprod.uverify.io';

/**
 * Low-level API surface, accessible via {@link UVerifyClient.core}.
 *
 * Use these methods when you need fine-grained control over the request/response
 * cycle (e.g. multi-sig flows, custom submission logic). For most use-cases
 * the high-level helpers on {@link UVerifyClient} are sufficient.
 */
export interface UVerifyCore {
  /**
   * Build an unsigned transaction for certificate issuance or state management.
   *
   * The returned `unsignedTransaction` is a CBOR-hex encoded transaction that
   * must be signed by the user's wallet before being submitted via
   * {@link submitTransaction}.
   */
  buildTransaction(request: BuildTransactionRequest): Promise<BuildTransactionResponse>;

  /**
   * Submit a signed transaction to the Cardano blockchain.
   *
   * @param transaction  - CBOR-hex encoded signed transaction.
   * @param witnessSet   - Optional separate witness set in CBOR-hex.
   * @returns The Cardano transaction hash.
   */
  submitTransaction(transaction: string, witnessSet?: string): Promise<string>;

  /**
   * Create a server-signed action request that the user must countersign.
   *
   * This is step 1 of the two-step user-state-action flow.
   */
  requestUserAction(request: UserActionRequest): Promise<UserActionRequestResponse>;

  /**
   * Execute a user state action using the signatures from both parties.
   *
   * This is step 2 of the two-step flow.
   */
  executeUserAction(request: ExecuteUserActionRequest): Promise<ExecuteUserActionResponse>;

  /**
   * Request a faucet challenge message from the server.
   *
   * This is step 1 of the two-step faucet flow. The returned `message` must be
   * signed by the user's wallet (CIP-30 `signData`) before calling
   * {@link claimFaucetFunds}.
   *
   * Only available when the backend is configured with `FAUCET_ENABLED=true`.
   */
  requestFaucetChallenge(address: string): Promise<FaucetChallengeResponse>;

  /**
   * Claim testnet ADA from the faucet using the signed challenge.
   *
   * This is step 2 of the two-step faucet flow. Returns the Cardano transaction
   * hash of the faucet transfer on success.
   *
   * Only available when the backend is configured with `FAUCET_ENABLED=true`.
   */
  claimFaucetFunds(request: FaucetClaimRequest): Promise<FaucetClaimResponse>;
}

export interface UVerifyClientOptions {
  /** Base URL of the UVerify API. Defaults to `https://api.uverify.io`. */
  baseUrl?: string;
  /** Optional HTTP headers added to every request (e.g. an API key). */
  headers?: Record<string, string>;
  /**
   * Default callback for message signing, used by {@link UVerifyClient.getUserInfo},
   * {@link UVerifyClient.invalidateState}, and {@link UVerifyClient.optOut} when no
   * per-call callback is provided.
   *
   * @example CIP-30 browser wallet
   * ```ts
   * signMessage: async (message) => {
   *   const hexAddress = await api.getChangeAddress();
   *   const hexMessage = Array.from(message)
   *     .map((c) => c.charCodeAt(0).toString(16).padStart(2, '0'))
   *     .join('');
   *   return api.signData(hexAddress, hexMessage);
   * }
   * ```
   */
  signMessage?: MessageSignCallback;
  /**
   * Default callback for transaction signing, used by {@link UVerifyClient.issueCertificates}
   * when no per-call callback is provided.
   *
   * @example CIP-30 browser wallet
   * ```ts
   * signTx: (unsignedTx) => api.signTx(unsignedTx, true)
   * ```
   */
  signTx?: TransactionSignCallback;
}

/**
 * Main entry point for the UVerify SDK.
 *
 * @example
 * ```ts
 * import { UVerifyClient } from '@uverify/sdk';
 *
 * const client = new UVerifyClient();
 *
 * const certificates = await client.verify('a3b4c5...');
 * console.log(certificates);
 * ```
 */
export class UVerifyClient {
  private readonly baseUrl: string;
  private readonly defaultHeaders: Record<string, string>;
  private readonly defaultSignMessage?: MessageSignCallback;
  private readonly defaultSignTx?: TransactionSignCallback;

  /**
   * Low-level API access. Use these methods when you need full control over
   * the request/response cycle. For most use-cases the high-level helpers
   * (e.g. {@link issueCertificates}, {@link getUserInfo}) are sufficient.
   *
   * @example
   * ```ts
   * const { unsignedTransaction } = await client.core.buildTransaction({ ... });
   * const witnessSet = await wallet.signTx(unsignedTransaction, true);
   * await client.core.submitTransaction(unsignedTransaction, witnessSet);
   * ```
   */
  readonly core: UVerifyCore;

  constructor(options: UVerifyClientOptions = {}) {
    this.baseUrl = (options.baseUrl ?? DEFAULT_BASE_URL).replace(/\/$/, '');
    this.defaultHeaders = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...options.headers,
    };
    this.defaultSignMessage = options.signMessage;
    this.defaultSignTx = options.signTx;
    this.core = {
      buildTransaction: this._buildTransaction.bind(this),
      submitTransaction: this._submitTransaction.bind(this),
      requestUserAction: this._requestUserAction.bind(this),
      executeUserAction: this._executeUserAction.bind(this),
      requestFaucetChallenge: this._requestFaucetChallenge.bind(this),
      claimFaucetFunds: this._claimFaucetFunds.bind(this),
    };
  }

  // ---------------------------------------------------------------------------
  // Internal helpers
  // ---------------------------------------------------------------------------

  private async request<T>(
    method: string,
    path: string,
    body?: unknown
  ): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const init: RequestInit = {
      method,
      headers: this.defaultHeaders,
    };
    if (body !== undefined) {
      init.body = JSON.stringify(body);
    }

    const response = await fetch(url, init);

    if (!response.ok) {
      let responseBody: unknown;
      try {
        responseBody = await response.json();
      } catch {
        responseBody = await response.text();
      }
      throw new UVerifyApiError(
        `UVerify API error ${response.status}: ${response.statusText}`,
        response.status,
        responseBody
      );
    }

    // Some endpoints return an empty body on success
    const text = await response.text();
    if (!text) return undefined as unknown as T;
    return JSON.parse(text) as T;
  }

  private get<T>(path: string): Promise<T> {
    return this.request<T>('GET', path);
  }

  private post<T>(path: string, body: unknown): Promise<T> {
    return this.request<T>('POST', path, body);
  }

  // ---------------------------------------------------------------------------
  // Certificate Verification
  // ---------------------------------------------------------------------------

  /**
   * Retrieve all on-chain certificates associated with the given data hash.
   *
   * @param hash - SHA-256 or SHA-512 hex hash of the data to look up.
   * @returns Array of certificates, or an empty array if none exist.
   *
   * @example
   * ```ts
   * const certs = await client.verify('a3b4c5d6...');
   * if (certs.length > 0) {
   *   console.log('First issued at block', certs[0].blockNumber);
   * }
   * ```
   */
  verify(hash: string): Promise<CertificateResponse[]> {
    return this.get<CertificateResponse[]>(`/api/v1/verify/${hash}`);
  }

  /**
   * Retrieve a single certificate by its Cardano transaction hash and data hash.
   *
   * @param transactionHash - 64-character hex transaction hash.
   * @param dataHash        - SHA-256 or SHA-512 hex hash of the certified data.
   */
  verifyByTransaction(
    transactionHash: string,
    dataHash: string
  ): Promise<CertificateResponse> {
    return this.get<CertificateResponse>(
      `/api/v1/verify/by-transaction-hash/${transactionHash}/${dataHash}`
    );
  }

  // ---------------------------------------------------------------------------
  // Low-level API (exposed via .core)
  // ---------------------------------------------------------------------------

  private _buildTransaction(
    request: BuildTransactionRequest
  ): Promise<BuildTransactionResponse> {
    const serialized = {
      ...request,
      certificates: request.certificates.map((c) => ({
        ...c,
        metadata:
          c.metadata !== undefined && typeof c.metadata === 'object'
            ? JSON.stringify(c.metadata)
            : c.metadata,
      })),
    };
    return this.post<BuildTransactionResponse>(
      '/api/v1/transaction/build',
      serialized
    );
  }

  private async _submitTransaction(
    transaction: string,
    witnessSet?: string
  ): Promise<string> {
    const result = await this.post<{ transactionHash: string }>(
      '/api/v1/transaction/submit',
      { transaction, witnessSet }
    );
    return result.transactionHash;
  }

  private _requestUserAction(
    request: UserActionRequest
  ): Promise<UserActionRequestResponse> {
    return this.post<UserActionRequestResponse>(
      '/api/v1/user/request/action',
      request
    );
  }

  private _executeUserAction(
    request: ExecuteUserActionRequest
  ): Promise<ExecuteUserActionResponse> {
    return this.post<ExecuteUserActionResponse>(
      '/api/v1/user/state/action',
      request
    );
  }

  private _requestFaucetChallenge(address: string): Promise<FaucetChallengeResponse> {
    return this.post<FaucetChallengeResponse>('/api/v1/faucet/request', { address });
  }

  private _claimFaucetFunds(request: FaucetClaimRequest): Promise<FaucetClaimResponse> {
    return this.post<FaucetClaimResponse>('/api/v1/faucet/claim', request);
  }

  // ---------------------------------------------------------------------------
  // High-level helpers
  // ---------------------------------------------------------------------------

  /**
   * Issue certificates in a single step.
   *
   * Builds the unsigned transaction, passes it to `signCallback` to obtain a
   * witness set, then submits the signed transaction to the blockchain.
   *
   * This is the recommended way to issue certificates when you control the full
   * flow in one place. For more control (e.g. multi-sig flows) use
   * {@link buildTransaction} and {@link submitTransaction} separately.
   *
   * @param address      Cardano address of the signer / payer.
   * @param stateId      ID of the issuing state.
   * @param certificates One or more certificates to certify.
   * @param signCallback Callback that receives the unsigned transaction and
   *                     returns the CIP-30 witness set CBOR hex.
   *
   * @example CIP-30 browser wallet
   * ```ts
   * await client.issueCertificates(
   *   'addr1...',
   *   'my-state-id',
   *   [{ hash: 'sha256-hash-of-doc', algorithm: 'SHA-256' }],
   *   (unsignedTx) => api.signTx(unsignedTx, true),
   * );
   * ```
   *
   * @returns The Cardano transaction hash of the submitted transaction.
   *
   * @example mesh.js
   * ```ts
   * const txHash = await client.issueCertificates(
   *   'addr1...', 'my-state-id',
   *   [{ hash: 'sha256-hash-of-doc', algorithm: 'SHA-256' }],
   *   (unsignedTx) => wallet.signTx(unsignedTx, true),
   * );
   * ```
   */
  async issueCertificates(
    address: string,
    certificates: CertificateData[],
    signCallback?: TransactionSignCallback,
    stateId?: string
  ): Promise<string> {
    const cb = this._resolveTxCallback(signCallback);
    const { unsignedTransaction } = await this._buildTransaction({
      type: 'default',
      address,
      stateId,
      certificates,
    });
    const witnessSet = await cb(unsignedTransaction);
    return this._submitTransaction(unsignedTransaction, witnessSet);
  }

  /**
   * Retrieve the current state information for a user in a single step.
   *
   * Internally calls {@link requestUserAction} to obtain a server-signed
   * challenge, invokes `signCallback` so the user's wallet can countersign,
   * then calls {@link executeUserAction} and returns the resolved state.
   *
   * @param address      Cardano address of the user.
   * @param signCallback Callback that signs the challenge message and returns
   *                     the CIP-30 `{ key, signature }` data signature object.
   *
   * @example CIP-30 browser wallet
   * ```ts
   * const state = await client.getUserInfo('addr1...', async (message) => {
   *   const hexAddress = await api.getChangeAddress();
   *   const hexMessage = Array.from(message)
   *     .map((c) => c.charCodeAt(0).toString(16).padStart(2, '0'))
   *     .join('');
   *   return api.signData(hexAddress, hexMessage);
   * });
   * console.log('Certificates left before renewal:', state?.countdown);
   * ```
   */
  async getUserInfo(
    address: string,
    signCallback?: MessageSignCallback
  ): Promise<UserState | undefined> {
    const result = await this._performUserStateAction(
      { address, action: 'USER_INFO' },
      signCallback
    );
    return result.state;
  }

  /**
   * Invalidate a user state in a single step.
   *
   * @param address      Cardano address of the user.
   * @param stateId      ID of the state to invalidate.
   * @param signCallback Callback that signs the challenge message.
   *                     Falls back to the `signMessage` set in the constructor.
   */
  async invalidateState(
    address: string,
    stateId: string,
    signCallback?: MessageSignCallback
  ): Promise<ExecuteUserActionResponse> {
    return this._performUserStateAction(
      { address, action: 'INVALIDATE_STATE', stateId },
      signCallback
    );
  }

  /**
   * Opt out of UVerify in a single step, removing the user's state.
   *
   * @param address      Cardano address of the user.
   * @param stateId      ID of the state to remove.
   * @param signCallback Callback that signs the challenge message.
   *                     Falls back to the `signMessage` set in the constructor.
   */
  async optOut(
    address: string,
    stateId: string,
    signCallback?: MessageSignCallback
  ): Promise<ExecuteUserActionResponse> {
    return this._performUserStateAction(
      { address, action: 'OPT_OUT', stateId },
      signCallback
    );
  }

  /**
   * Internal helper that runs the two-step user state action flow:
   * request challenge → user signs → execute action.
   */
  private async _performUserStateAction(
    request: UserActionRequest,
    signCallback?: MessageSignCallback
  ): Promise<ExecuteUserActionResponse> {
    const cb = this._resolveMessageCallback(signCallback);
    const challenge = await this._requestUserAction(request);
    const { key, signature } = await cb(challenge.message);
    return this._executeUserAction({
      address: challenge.address,
      action: challenge.action as UserAction,
      message: challenge.message,
      signature: challenge.signature,
      timestamp: challenge.timestamp,
      userSignature: signature,
      userPublicKey: key,
    });
  }

  /**
   * Fund a wallet with testnet ADA from the UVerify dev faucet in a single step.
   *
   * Orchestrates the full challenge-sign-claim flow:
   * 1. Requests a server-signed challenge for `address`.
   * 2. Passes the challenge message to `signMessage` (CIP-30 `signData`).
   * 3. Submits both signatures to the faucet claim endpoint.
   *
   * Returns the Cardano transaction hash of the faucet transfer on success.
   * Use {@link waitFor} to poll until the funds are queryable on-chain.
   *
   * **Only available when the backend is configured with `FAUCET_ENABLED=true`.**
   * This is intended for testnet development environments only.
   *
   * @param address     Cardano address that should receive the testnet funds.
   * @param signMessage Callback to sign the challenge message. Falls back to
   *                    the `signMessage` option passed to the constructor.
   *
   * @example CIP-30 browser wallet
   * ```ts
   * const result = await client.fundWallet(
   *   'addr_test1...',
   *   async (message) => {
   *     const hexAddress = await api.getChangeAddress();
   *     const hexMessage = Buffer.from(message).toString('hex');
   *     return api.signData(hexAddress, hexMessage);
   *   }
   * );
   * console.log('Funded by tx:', result.txHash);
   * ```
   */
  async fundWallet(
    address: string,
    signMessage?: MessageSignCallback
  ): Promise<FaucetClaimResponse> {
    const sign = this._resolveMessageCallback(signMessage);
    try {
      const challenge = await this.core.requestFaucetChallenge(address);
      const { key, signature } = await sign(challenge.message);
      return await this.core.claimFaucetFunds({
        address,
        message: challenge.message,
        signature: challenge.signature,
        userSignature: signature,
        userPublicKey: key,
        timestamp: challenge.timestamp,
      });
    } catch (err) {
      if (err instanceof UVerifyApiError) {
        if (err.statusCode === 404) {
          throw new UVerifyApiError(
            'Faucet endpoint not found (HTTP 404). ' +
              'The faucet is only available on backends started with FAUCET_ENABLED=true. ' +
              'This feature does not exist on mainnet — acquire ADA from a cryptocurrency exchange instead.',
            404,
            err.responseBody
          );
        }
        if (err.statusCode === 429) {
          throw new UVerifyApiError(
            'Faucet cooldown active (HTTP 429). ' +
              'This address recently received testnet funds. Please wait a few minutes before trying again.',
            429,
            err.responseBody
          );
        }
      }
      throw err;
    }
  }

  private _resolveMessageCallback(
    override?: MessageSignCallback
  ): MessageSignCallback {
    const cb = override ?? this.defaultSignMessage;
    if (!cb) {
      throw new UVerifyValidationError(
        'No message sign callback provided. ' +
          'Pass one to this method or set signMessage in UVerifyClientOptions.'
      );
    }
    return cb;
  }

  private _resolveTxCallback(
    override?: TransactionSignCallback
  ): TransactionSignCallback {
    const cb = override ?? this.defaultSignTx;
    if (!cb) {
      throw new UVerifyValidationError(
        'No transaction sign callback provided. ' +
          'Pass one to this method or set signTx in UVerifyClientOptions.'
      );
    }
    return cb;
  }
}

// ---------------------------------------------------------------------------
// Polling utility
// ---------------------------------------------------------------------------

/**
 * Polls `condition` every `intervalMs` milliseconds until it returns `true`,
 * or rejects once `timeoutMs` has elapsed.
 *
 * Useful for waiting on eventual-consistency operations such as faucet funds
 * settling on-chain or a submitted certificate becoming queryable.
 *
 * @param condition   Async predicate — return `true` to stop polling.
 * @param timeoutMs   Maximum wait in milliseconds. Default: 60 000 (1 min).
 * @param intervalMs  Delay between polls in milliseconds. Default: 2 000 (2 s).
 *
 * @example Wait until a certificate is queryable after issuance
 * ```ts
 * import { UVerifyClient, waitFor } from '@uverify/sdk';
 *
 * const client = new UVerifyClient();
 * await client.issueCertificates(address, certs, signTx);
 * await waitFor(() => client.verify(dataHash).then((c) => c.length > 0));
 * ```
 *
 * @example Wait until faucet funds settle with a custom timeout
 * ```ts
 * await client.fundWallet(address, signMessage);
 * await waitFor(
 *   () => client.verify(dataHash).then((c) => c.length > 0),
 *   120_000, // 2-minute timeout
 * );
 * ```
 */
export async function waitFor(
  condition: () => Promise<boolean>,
  timeoutMs = 60_000,
  intervalMs = 2_000
): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await condition()) return;
    await new Promise<void>((resolve) => setTimeout(resolve, intervalMs));
  }
  throw new WaitForTimeoutError(timeoutMs);
}
