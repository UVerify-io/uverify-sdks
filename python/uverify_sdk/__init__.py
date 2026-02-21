"""
uverify-sdk — Official Python SDK for the UVerify API.

Quick start::

    from uverify_sdk import UVerifyClient

    client = UVerifyClient()
    certificates = client.verify("sha256-hash-of-your-document")
    print(certificates)
"""

from .client import UVerifyClient
from .exceptions import UVerifyApiError, UVerifyError, UVerifyValidationError
from .models import (
    BuildStatus,
    BuildTransactionRequest,
    BuildTransactionResponse,
    CertificateData,
    CertificateResponse,
    ClaimUpdateConnectedGoodsRequest,
    ConnectedGoodsItemInput,
    ExecuteUserActionRequest,
    ExecuteUserActionResponse,
    MintConnectedGoodsRequest,
    MintConnectedGoodsResponse,
    SocialHub,
    TransactionType,
    TxContractDetails,
    TxRedeemerDto,
    UserAction,
    UserActionRequest,
    UserActionRequestResponse,
    UserState,
    UVerifyCertificate,
)

__version__ = "0.1.0"
__all__ = [
    "UVerifyClient",
    "UVerifyError",
    "UVerifyApiError",
    "UVerifyValidationError",
    "CertificateData",
    "CertificateResponse",
    "BuildStatus",
    "BuildTransactionRequest",
    "BuildTransactionResponse",
    "TransactionType",
    "TxContractDetails",
    "TxRedeemerDto",
    "UserAction",
    "UserActionRequest",
    "UserActionRequestResponse",
    "ExecuteUserActionRequest",
    "ExecuteUserActionResponse",
    "UserState",
    "UVerifyCertificate",
    "MintConnectedGoodsRequest",
    "MintConnectedGoodsResponse",
    "ConnectedGoodsItemInput",
    "ClaimUpdateConnectedGoodsRequest",
    "SocialHub",
]
