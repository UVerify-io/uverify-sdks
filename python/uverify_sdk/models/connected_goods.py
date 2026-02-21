"""Connected Goods extension data models."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional


@dataclass
class ConnectedGoodsItemInput:
    """A single item definition used when minting a batch."""

    password: str
    asset_name: str

    def to_dict(self) -> dict:
        return {"password": self.password, "asset_name": self.asset_name}


@dataclass
class MintConnectedGoodsRequest:
    """Request body for ``POST /api/v1/extension/connected-goods/mint/batch``."""

    items: List[ConnectedGoodsItemInput]
    address: str
    token_name: str

    def to_dict(self) -> dict:
        return {
            "items": [i.to_dict() for i in self.items],
            "address": self.address,
            "token_name": self.token_name,
        }


@dataclass
class MintConnectedGoodsResponse:
    message: str
    status: int
    error: Optional[str] = None
    unsigned_transaction: Optional[str] = None
    batch_id: Optional[str] = None

    @classmethod
    def from_dict(cls, data: dict) -> "MintConnectedGoodsResponse":
        return cls(
            message=data["message"],
            status=data["status"],
            error=data.get("error"),
            unsigned_transaction=data.get("unsigned_transaction"),
            batch_id=data.get("batch_id"),
        )


@dataclass
class SocialHub:
    """Social profile associated with a claimed connected-goods item."""

    owner: Optional[str] = None
    picture: Optional[str] = None
    name: Optional[str] = None
    twitter: Optional[str] = None
    instagram: Optional[str] = None
    website: Optional[str] = None
    extra: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict:
        d: dict = {}
        if self.owner is not None:
            d["owner"] = self.owner
        if self.picture is not None:
            d["picture"] = self.picture
        if self.name is not None:
            d["name"] = self.name
        if self.twitter is not None:
            d["twitter"] = self.twitter
        if self.instagram is not None:
            d["instagram"] = self.instagram
        if self.website is not None:
            d["website"] = self.website
        d.update(self.extra)
        return d


@dataclass
class ClaimUpdateConnectedGoodsRequest:
    """Request body for claim and update connected-goods endpoints."""

    password: str
    batch_id: str
    user_address: str
    social_hub: Optional[SocialHub] = None

    def to_dict(self) -> dict:
        d: dict = {
            "password": self.password,
            "batch_id": self.batch_id,
            "user_address": self.user_address,
        }
        if self.social_hub is not None:
            d["social_hub"] = self.social_hub.to_dict()
        return d
