"""User state management data models."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import List, Literal, Optional

UserAction = Literal["USER_INFO", "INVALIDATE_STATE", "OPT_OUT"]


@dataclass
class UserActionRequest:
    """
    Request body for ``POST /api/v1/user/request/action``.

    Attributes:
        address:  Cardano address of the user.
        action:   The action to perform.
        state_id: State ID (required for INVALIDATE_STATE and OPT_OUT).
    """

    address: str
    action: UserAction
    state_id: Optional[str] = None

    def to_dict(self) -> dict:
        d: dict = {"address": self.address, "action": self.action}
        if self.state_id is not None:
            d["stateId"] = self.state_id
        return d


@dataclass
class UserActionRequestResponse:
    """
    Response from ``POST /api/v1/user/request/action``.

    The ``message`` field must be signed by the user's wallet before
    calling :meth:`~uverify_sdk.client.UVerifyClient.execute_user_action`.
    """

    address: str
    action: str
    signature: str
    timestamp: int
    message: str
    status: int
    error: Optional[str] = None

    @classmethod
    def from_dict(cls, data: dict) -> "UserActionRequestResponse":
        return cls(
            address=data["address"],
            action=data["action"],
            signature=data["signature"],
            timestamp=data["timestamp"],
            message=data["message"],
            status=data["status"],
            error=data.get("error"),
        )


@dataclass
class ExecuteUserActionRequest:
    """
    Request body for ``POST /api/v1/user/state/action``.

    Attributes:
        address:          Cardano address of the user.
        action:           The action to execute.
        message:          Message from the request/action step.
        signature:        Server signature from the request/action step.
        user_signature:   CIP-30 wallet signature over the message.
        user_public_key:  Hex-encoded public key of the signing key.
        timestamp:        Timestamp from the request/action step.
        state_id:         Optional state ID.
    """

    address: str
    action: UserAction
    message: str
    signature: str
    user_signature: str
    user_public_key: str
    timestamp: int
    state_id: Optional[str] = None

    def to_dict(self) -> dict:
        d: dict = {
            "address": self.address,
            "action": self.action,
            "message": self.message,
            "signature": self.signature,
            "userSignature": self.user_signature,
            "userPublicKey": self.user_public_key,
            "timestamp": self.timestamp,
        }
        if self.state_id is not None:
            d["stateId"] = self.state_id
        return d


@dataclass
class UVerifyCertificate:
    hash: str
    algorithm: str
    issuer: str
    extra: List[str] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: dict) -> "UVerifyCertificate":
        return cls(
            hash=data["hash"],
            algorithm=data["algorithm"],
            issuer=data["issuer"],
            extra=data.get("extra", []),
        )


@dataclass
class UserState:
    """On-chain state datum representing a user's issuing workspace."""

    id: str
    owner: str
    fee: int
    fee_interval: int
    fee_receivers: List[str]
    ttl: int
    countdown: int
    certificates: List[UVerifyCertificate]
    batch_size: int
    bootstrap_datum_name: str
    keep_as_oracle: bool

    @classmethod
    def from_dict(cls, data: dict) -> "UserState":
        return cls(
            id=data["id"],
            owner=data["owner"],
            fee=data["fee"],
            fee_interval=data["feeInterval"],
            fee_receivers=data.get("feeReceivers", []),
            ttl=data["ttl"],
            countdown=data["countdown"],
            certificates=[
                UVerifyCertificate.from_dict(c)
                for c in data.get("certificates", [])
            ],
            batch_size=data["batchSize"],
            bootstrap_datum_name=data["bootstrapDatumName"],
            keep_as_oracle=data.get("keepAsOracle", False),
        )


@dataclass
class ExecuteUserActionResponse:
    """Response from ``POST /api/v1/user/state/action``."""

    status: int
    state: Optional[UserState] = None
    error: Optional[str] = None
    unsigned_transaction: Optional[str] = None

    @classmethod
    def from_dict(cls, data: dict) -> "ExecuteUserActionResponse":
        state_data = data.get("state")
        return cls(
            status=data["status"],
            state=UserState.from_dict(state_data) if state_data else None,
            error=data.get("error"),
            unsigned_transaction=data.get("unsignedTransaction"),
        )
