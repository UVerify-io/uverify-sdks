/**
 * Available actions for user state management.
 *
 * - `USER_INFO`        — retrieve current state information.
 * - `INVALIDATE_STATE` — mark the current state as invalid.
 * - `OPT_OUT`          — opt out and remove the user's state.
 */
export type UserAction = 'USER_INFO' | 'INVALIDATE_STATE' | 'OPT_OUT';

/** Request body for `POST /api/v1/user/request/action`. */
export interface UserActionRequest {
  /** Cardano address of the user. */
  address: string;
  /** The action to perform. */
  action: UserAction;
  /** State ID to act on (required for INVALIDATE_STATE and OPT_OUT). */
  stateId?: string;
}

/**
 * Response from `POST /api/v1/user/request/action`.
 * Contains a server-side signed message that the user must countersign
 * before calling `POST /api/v1/user/state/action`.
 */
export interface UserActionRequestResponse {
  address: string;
  action: UserAction;
  /** Server signature over the message. */
  signature: string;
  /** UNIX timestamp of the request. */
  timestamp: number;
  /** Message that the user must sign with their wallet. */
  message: string;
  status: number;
  error?: string;
}

/** Request body for `POST /api/v1/user/state/action`. */
export interface ExecuteUserActionRequest {
  address: string;
  action: UserAction;
  stateId?: string;
  /** Message returned by the request/action step. */
  message: string;
  /** Server signature from the request/action step. */
  signature: string;
  /** CIP-30 wallet signature over the message. */
  userSignature: string;
  /** Hex-encoded public key of the signing key. */
  userPublicKey: string;
  /** Timestamp from the request/action step. */
  timestamp: number;
}

/** An on-chain certificate entry inside a state datum. */
export interface UVerifyCertificate {
  hash: string;
  algorithm: string;
  issuer: string;
  extra: string[];
}

/** On-chain state datum representing a user's issuing workspace. */
export interface UserState {
  /** Unique identifier of this state (derived from an output reference). */
  id: string;
  /** Payment credential of the owner. */
  owner: string;
  fee: number;
  feeInterval: number;
  feeReceivers: string[];
  /** UNIX timestamp — expiry of this state. */
  ttl: number;
  /** Certificates remaining before renewal is required. */
  countdown: number;
  certificates: UVerifyCertificate[];
  batchSize: number;
  bootstrapDatumName: string;
  keepAsOracle: boolean;
}

/** Response from `POST /api/v1/user/state/action`. */
export interface ExecuteUserActionResponse {
  state?: UserState;
  status: number;
  error?: string;
  /** Unsigned transaction hex when the action requires an on-chain transaction. */
  unsignedTransaction?: string;
}
