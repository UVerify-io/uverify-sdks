/** Request body for `POST /api/v1/faucet/request`. */
export interface FaucetChallengeRequest {
  /** Cardano address that should receive the testnet funds. */
  address: string;
}

/**
 * Response from `POST /api/v1/faucet/request`.
 * Contains a server-signed challenge message that the user must countersign
 * before calling `POST /api/v1/faucet/claim`.
 */
export interface FaucetChallengeResponse {
  address: string;
  /** Challenge message the user must sign with their wallet (CIP-30 `signData`). */
  message: string;
  /** Server signature over the message (used for verification in the claim step). */
  signature: string;
  /** UNIX timestamp of the request. */
  timestamp: number;
  status: number;
  error?: string;
}

/** Request body for `POST /api/v1/faucet/claim`. */
export interface FaucetClaimRequest {
  address: string;
  /** Challenge message from the request step. */
  message: string;
  /** Server signature from the request step. */
  signature: string;
  /** CIP-30 wallet signature over the challenge message. */
  userSignature: string;
  /** Hex-encoded public key of the signing key. */
  userPublicKey: string;
  /** Timestamp from the request step. */
  timestamp: number;
}

/** Response from `POST /api/v1/faucet/claim`. */
export interface FaucetClaimResponse {
  /** Cardano transaction hash of the faucet transfer. Present on success. */
  txHash?: string;
  status: number;
  error?: string;
}
