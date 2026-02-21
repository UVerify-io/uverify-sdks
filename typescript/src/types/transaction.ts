import type { CertificateData } from './certificate.js';

/**
 * The type of transaction to build.
 *
 * - `default`        — standard certificate issuance against an existing state.
 * - `bootstrap`      — initialise a new state (fork) from a bootstrap datum.
 * - `custom`         — custom certificate transaction.
 * - `burn_state`     — burn the state token and close the workspace.
 * - `burn_bootstrap` — burn a bootstrap datum token.
 * - `init`           — deploy the initial bootstrap datum.
 * - `deploy`         — deploy a library proxy contract.
 */
export type TransactionType =
  | 'default'
  | 'bootstrap'
  | 'custom'
  | 'burn_state'
  | 'burn_bootstrap'
  | 'init'
  | 'deploy';

/** Status information returned alongside an unsigned transaction. */
export interface BuildStatus {
  message: string;
  code: number;
}

/** Request body for `POST /api/v1/transaction/build`. */
export interface BuildTransactionRequest {
  /** Array of certificates to issue in this transaction. */
  certificates: CertificateData[];
  /** Transaction type. */
  type: TransactionType;
  /** Cardano address of the signer / payer. */
  address: string;
  /**
   * Bootstrap datum reference. Required when `type` is `bootstrap`
   * or when you want to specify which bootstrap datum to fork from.
   */
  bootstrapDatum?: Record<string, unknown>;
  /** Existing state ID to update. Required for `default` and `burn_state` types. */
  stateId?: string;
}

/** Response body for `POST /api/v1/transaction/build`. */
export interface BuildTransactionResponse {
  /** CBOR-hex encoded unsigned transaction, ready to be signed. */
  unsignedTransaction: string;
  /** Echoes back the transaction type. */
  type: TransactionType;
  /** Build status with a human-readable message and code. */
  status: BuildStatus;
}

