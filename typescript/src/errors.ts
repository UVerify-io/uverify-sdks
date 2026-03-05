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
