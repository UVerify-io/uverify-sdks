/**
 * A single certificate entry as stored on-chain and returned by the verification endpoints.
 */
export interface CertificateResponse {
  /** SHA-256 or SHA-512 hash of the certified data. */
  hash: string;
  /** Cardano address that issued the certificate. */
  address: string;
  /** Hash of the block that contains this certificate. */
  blockHash: string;
  /** Block height at which the certificate was recorded. */
  blockNumber: number;
  /** Transaction hash on the Cardano blockchain. */
  transactionHash: string;
  /** Slot number at which the certificate was recorded. */
  slot: number;
  /** ISO-8601 timestamp of the block. */
  creationTime: string;
  /** Optional metadata attached to the certificate. */
  metadata?: string;
  /** Optional issuer identifier. */
  issuer?: string;
}

/**
 * Certificate data provided when building a new certification transaction.
 */
export interface CertificateData {
  /** SHA-256 or SHA-512 hash of the data to be certified. */
  hash: string;
  /**
   * Optional metadata to attach to the certificate.
   * Pass a plain object and the SDK will serialize it to JSON automatically,
   * so you don't need to call `JSON.stringify` yourself.
   */
  metadata?: string | Record<string, unknown>;
  /**
   * Hashing algorithm used to produce the hash.
   * Common values: "SHA-256", "SHA-512".
   */
  algorithm?: string;
}
