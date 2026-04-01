/** Thrown when the UVerify API returns a non-2xx response. */
export class UVerifyApiError extends Error {
  constructor(
    message: string,
    public readonly statusCode: number,
    public readonly responseBody?: unknown
  ) {
    super(message);
    this.name = 'UVerifyApiError';
  }
}

/**
 * Thrown when the API returns HTTP 404 (resource not found).
 *
 * When thrown from {@link UVerifyClient.fundWallet}, this indicates the faucet
 * endpoint is not available on this backend deployment.
 */
export class NotFoundError extends UVerifyApiError {
  constructor(message: string, responseBody?: unknown) {
    super(message, 404, responseBody);
    this.name = 'NotFoundError';
  }
}

/**
 * Thrown when the API returns HTTP 429 (rate limit / cooldown active).
 *
 * @example
 * ```ts
 * try {
 *   await client.fundWallet(address, signMessage);
 * } catch (err) {
 *   if (err instanceof RateLimitError) {
 *     console.warn('Faucet cooldown active, try again later.');
 *   }
 * }
 * ```
 */
export class RateLimitError extends UVerifyApiError {
  constructor(message: string, responseBody?: unknown) {
    super(message, 429, responseBody);
    this.name = 'RateLimitError';
  }
}

/**
 * Thrown when a transaction cannot be built because the wallet has no spendable
 * UTXOs (HTTP 400, "No UTXOs found for user address").
 *
 * Catch this specifically to trigger a faucet top-up flow rather than retrying:
 *
 * ```ts
 * try {
 *   await client.issueCertificates(address, certs, signTx);
 * } catch (err) {
 *   if (err instanceof InsufficientFundsError) {
 *     await client.fundWallet(address, signMessage);
 *   }
 * }
 * ```
 */
export class InsufficientFundsError extends UVerifyApiError {
  constructor(message: string, responseBody?: unknown) {
    super(message, 400, responseBody);
    this.name = 'InsufficientFundsError';
  }
}

/** Thrown when a required parameter is missing or invalid. */
export class UVerifyValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'UVerifyValidationError';
  }
}

/**
 * Thrown by {@link waitFor} when the condition is not met within the given timeout.
 *
 * Catch this specifically to distinguish a timeout from other errors and advise
 * the user to wait longer or re-run:
 *
 * ```ts
 * try {
 *   await waitFor(() => client.verify(hash).then(c => c.length > 0), 300_000);
 * } catch (err) {
 *   if (err instanceof WaitForTimeoutError) {
 *     console.error(err.message);
 *   } else throw err;
 * }
 * ```
 */
export class WaitForTimeoutError extends Error {
  constructor(public readonly timeoutMs: number) {
    super(
      `Transaction not confirmed within ${timeoutMs / 1000} seconds. ` +
      `The transaction may still be processing — please re-run the script. ` +
      `If this happens repeatedly, increase the timeout value.`
    );
    this.name = 'WaitForTimeoutError';
  }
}
