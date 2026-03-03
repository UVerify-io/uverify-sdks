"""Dev faucet data models."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Optional


@dataclass
class FaucetChallengeResponse:
    """
    Response from ``POST /api/v1/faucet/request``.

    The ``message`` field must be signed by the user's wallet (CIP-30 ``signData``)
    before calling :meth:`~uverify_sdk.client.UVerifyClient.request_faucet`.

    Attributes:
        address:   Cardano address that will receive the funds.
        message:   Challenge message the user must sign.
        signature: Server signature over the message.
        timestamp: UNIX timestamp of the challenge.
        status:    HTTP status code.
        error:     Error description if the request failed.
    """

    address: str
    message: str
    signature: str
    timestamp: int
    status: int
    error: Optional[str] = None

    @classmethod
    def from_dict(cls, data: dict) -> "FaucetChallengeResponse":
        return cls(
            address=data.get("address", ""),
            message=data.get("message", ""),
            signature=data.get("signature", ""),
            timestamp=data.get("timestamp", 0),
            status=data.get("status", 0),
            error=data.get("error"),
        )


@dataclass
class FaucetClaimRequest:
    """
    Request body for ``POST /api/v1/faucet/claim``.

    Attributes:
        address:        Cardano address that should receive the funds.
        message:        Challenge message from the request step.
        signature:      Server signature from the request step.
        user_signature: CIP-30 wallet signature over the challenge message.
        user_public_key: Hex-encoded public key of the signing key.
        timestamp:      Timestamp from the request step.
    """

    address: str
    message: str
    signature: str
    user_signature: str
    user_public_key: str
    timestamp: int

    def to_dict(self) -> dict:
        return {
            "address": self.address,
            "message": self.message,
            "signature": self.signature,
            "userSignature": self.user_signature,
            "userPublicKey": self.user_public_key,
            "timestamp": self.timestamp,
        }


@dataclass
class FaucetClaimResponse:
    """
    Response from ``POST /api/v1/faucet/claim``.

    Attributes:
        tx_hash: Cardano transaction hash of the faucet transfer. Present on success.
        status:  HTTP status code.
        error:   Error description if the claim failed.
    """

    status: int
    tx_hash: Optional[str] = None
    error: Optional[str] = None

    @classmethod
    def from_dict(cls, data: dict) -> "FaucetClaimResponse":
        return cls(
            tx_hash=data.get("txHash"),
            status=data.get("status", 0),
            error=data.get("error"),
        )
