"""Certificate-related data models."""
from __future__ import annotations

import json as _json
from dataclasses import dataclass
from typing import Dict, Optional, Union


@dataclass
class CertificateData:
    """
    Certificate data provided when building a new certification transaction.

    Attributes:
        hash:      SHA-256 or SHA-512 hex hash of the data to certify.
        metadata:  Optional metadata to attach — either a pre-serialised JSON
                   string or a plain ``dict`` (automatically serialised).
        algorithm: Hashing algorithm, e.g. "SHA-256" or "SHA-512".
    """

    hash: str
    metadata: Optional[Union[str, Dict[str, str]]] = None
    algorithm: Optional[str] = None

    def to_dict(self) -> dict:
        d: dict = {"hash": self.hash}
        if self.metadata is not None:
            d["metadata"] = (
                _json.dumps(self.metadata)
                if isinstance(self.metadata, dict)
                else self.metadata
            )
        if self.algorithm is not None:
            d["algorithm"] = self.algorithm
        return d


@dataclass
class CertificateResponse:
    """
    A single certificate as returned by the verification endpoints.

    Attributes:
        hash:             SHA-256 or SHA-512 hash of the certified data.
        address:          Cardano address that issued the certificate.
        block_hash:       Hash of the containing block.
        block_number:     Block height.
        transaction_hash: Cardano transaction hash.
        slot:             Slot number.
        creation_time:    ISO-8601 timestamp.
        metadata:         Optional attached metadata.
        issuer:           Optional issuer identifier.
    """

    hash: str
    address: str
    block_hash: str
    block_number: int
    transaction_hash: str
    slot: int
    creation_time: str
    metadata: Optional[str] = None
    issuer: Optional[str] = None

    @classmethod
    def from_dict(cls, data: dict) -> "CertificateResponse":
        return cls(
            hash=data["hash"],
            address=data["address"],
            block_hash=data["blockHash"],
            block_number=data["blockNumber"],
            transaction_hash=data["transactionHash"],
            slot=data["slot"],
            creation_time=data["creationTime"],
            metadata=data.get("metadata"),
            issuer=data.get("issuer"),
        )
