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
