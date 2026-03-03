"""
uverify-sdk — Official Python SDK for the UVerify API.

Quick start::

    from uverify_sdk import UVerifyClient

    client = UVerifyClient()
    certificates = client.verify("sha256-hash-of-your-document")
    print(certificates)
"""

from .client import DataSignature, MessageSignCallback, TransactionSignCallback, UVerifyClient, UVerifyCore, wait_for
from .exceptions import UVerifyApiError, UVerifyError, UVerifyValidationError
from .models import (
    BuildStatus,
    BuildTransactionRequest,
    BuildTransactionResponse,
    CertificateData,
    CertificateResponse,
    ExecuteUserActionRequest,
    ExecuteUserActionResponse,
    FaucetChallengeResponse,
    FaucetClaimRequest,
    FaucetClaimResponse,
    TransactionType,
    UserAction,
    UserActionRequest,
    UserActionRequestResponse,
    UserState,
    UVerifyCertificate,
)

__version__ = "0.1.1"
__all__ = [
    "UVerifyClient",
    "UVerifyCore",
    "DataSignature",
    "MessageSignCallback",
    "TransactionSignCallback",
    "wait_for",
    "UVerifyError",
    "UVerifyApiError",
    "UVerifyValidationError",
    "CertificateData",
    "CertificateResponse",
    "BuildStatus",
    "BuildTransactionRequest",
    "BuildTransactionResponse",
    "TransactionType",
    "UserAction",
    "UserActionRequest",
    "UserActionRequestResponse",
    "ExecuteUserActionRequest",
    "ExecuteUserActionResponse",
    "UserState",
    "UVerifyCertificate",
    "FaucetChallengeResponse",
    "FaucetClaimRequest",
    "FaucetClaimResponse",
]
