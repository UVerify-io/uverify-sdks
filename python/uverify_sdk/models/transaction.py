"""Transaction-related data models."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Dict, List, Literal, Optional

from .certificate import CertificateData

TransactionType = Literal[
    "default",
    "bootstrap",
    "custom",
    "burn_state",
    "burn_bootstrap",
    "init",
    "deploy",
]


@dataclass
class BuildTransactionRequest:
    """
    Request body for ``POST /api/v1/transaction/build``.

    Attributes:
        certificates:    List of certificates to issue.
        type:            Transaction type.
        address:         Cardano address of the signer / payer.
        bootstrap_datum: Optional bootstrap datum reference.
        state_id:        Existing state ID (required for ``default`` / ``burn_state``).
    """

    certificates: List[CertificateData]
    type: TransactionType
    address: str
    bootstrap_datum: Optional[Dict[str, Any]] = None
    state_id: Optional[str] = None

    def to_dict(self) -> dict:
        d: dict = {
            "certificates": [c.to_dict() for c in self.certificates],
            "type": self.type,
            "address": self.address,
        }
        if self.bootstrap_datum is not None:
            d["bootstrapDatum"] = self.bootstrap_datum
        if self.state_id is not None:
            d["stateId"] = self.state_id
        return d


@dataclass
class BuildStatus:
    message: str
    code: int

    @classmethod
    def from_dict(cls, data: dict) -> "BuildStatus":
        return cls(message=data["message"], code=data["code"])


@dataclass
class BuildTransactionResponse:
    """
    Response from ``POST /api/v1/transaction/build``.

    Attributes:
        unsigned_transaction: CBOR-hex encoded unsigned transaction.
        type:                 Echoed back transaction type.
        status:               Build status.
    """

    unsigned_transaction: str
    type: str
    status: BuildStatus

    @classmethod
    def from_dict(cls, data: dict) -> "BuildTransactionResponse":
        return cls(
            unsigned_transaction=data["unsignedTransaction"],
            type=data["type"],
            status=BuildStatus.from_dict(data["status"]),
        )
