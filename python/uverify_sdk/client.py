"""Main UVerify API client."""
from __future__ import annotations

import time
from dataclasses import dataclass
from typing import Callable, List, Optional, TypeVar, Union

_T = TypeVar("_T")

import requests

from .exceptions import UVerifyApiError, UVerifyValidationError
from .models.certificate import CertificateData, CertificateResponse
from .models.transaction import (
    BuildTransactionRequest,
    BuildTransactionResponse,
)
from .models.faucet import FaucetChallengeResponse, FaucetClaimRequest, FaucetClaimResponse
from .models.user_state import (
    ExecuteUserActionRequest,
    ExecuteUserActionResponse,
    UserAction,
    UserActionRequest,
    UserActionRequestResponse,
)

DEFAULT_BASE_URL = "https://api.preprod.uverify.io"


@dataclass
class DataSignature:
    """
    Holds the CIP-30 ``signData`` result.

    Attributes:
        key:       Hex-encoded CBOR public key returned by the wallet.
        signature: Hex-encoded CBOR signature returned by the wallet.
    """

    key: str
    signature: str


# Wallet-agnostic callback types.
# MessageSignCallback: receives the challenge string, returns a DataSignature.
# TransactionSignCallback: receives the unsigned CBOR-hex tx, returns the witness-set CBOR-hex.
MessageSignCallback = Callable[[str], DataSignature]
TransactionSignCallback = Callable[[str], str]


class UVerifyCore:
    """
    Low-level access to the UVerify API primitives.

    Obtain an instance from :attr:`UVerifyClient.core`. These methods map
    one-to-one to the underlying REST endpoints and give you full control over
    the transaction lifecycle. For common workflows prefer the high-level helpers
    on :class:`UVerifyClient` (``issue_certificates``, ``get_user_info``, etc.).

    Example::

        resp = client.core.build_transaction(
            BuildTransactionRequest(
                type="default",
                address="addr1...",
                state_id="my-state",
                certificates=[CertificateData(hash="sha256-hash", algorithm="SHA-256")],
            )
        )
        witness_set = my_wallet.sign_tx(resp.unsigned_transaction)
        client.core.submit_transaction(resp.unsigned_transaction, witness_set)
    """

    def __init__(self, client: "UVerifyClient") -> None:
        self._client = client

    def build_transaction(
        self, request: BuildTransactionRequest
    ) -> BuildTransactionResponse:
        """Build an unsigned transaction for certificate issuance or state management."""
        return self._client._build_transaction(request)

    def submit_transaction(
        self, transaction: str, witness_set: Optional[str] = None
    ) -> None:
        """Submit a signed transaction to the Cardano blockchain."""
        self._client._submit_transaction(transaction, witness_set)

    def request_user_action(
        self, request: UserActionRequest
    ) -> UserActionRequestResponse:
        """Create a server-signed action request that the user must countersign (step 1)."""
        return self._client._request_user_action(request)

    def execute_user_action(
        self, request: ExecuteUserActionRequest
    ) -> ExecuteUserActionResponse:
        """Execute a user state action using signatures from both parties (step 2)."""
        return self._client._execute_user_action(request)

    def request_faucet_challenge(self, address: str) -> FaucetChallengeResponse:
        """
        Request a faucet challenge message from the server (step 1 of 2).

        Only available when the backend is configured with ``FAUCET_ENABLED=true``.
        """
        return self._client._request_faucet_challenge(address)

    def claim_faucet_funds(self, request: FaucetClaimRequest) -> FaucetClaimResponse:
        """
        Claim testnet ADA using the signed challenge (step 2 of 2).

        Only available when the backend is configured with ``FAUCET_ENABLED=true``.
        """
        return self._client._claim_faucet_funds(request)


class UVerifyClient:
    """
    Main entry point for the UVerify SDK.

    **Certificate verification**::

        from uverify_sdk import UVerifyClient

        client = UVerifyClient()
        certificates = client.verify("a3b4c5d6...")
        print(certificates)

    **Issuing certificates (high-level helper)**::

        client = UVerifyClient(sign_tx=lambda tx: my_wallet.sign_tx(tx))
        client.issue_certificates(
            address="addr1...",
            certificates=[CertificateData(hash="sha256-hash", algorithm="SHA-256")],
        )

    **Low-level access via** ``.core``::

        resp = client.core.build_transaction(BuildTransactionRequest(...))
        witness_set = my_wallet.sign_tx(resp.unsigned_transaction)
        client.core.submit_transaction(resp.unsigned_transaction, witness_set)

    Args:
        base_url:     Base URL of the UVerify API. Defaults to ``https://api.uverify.io``.
        headers:      Additional HTTP headers added to every request.
        timeout:      Request timeout in seconds. Defaults to 30.
        session:      Optional ``requests.Session`` (useful for testing).
        sign_message: Default :data:`MessageSignCallback` for user-state actions.
        sign_tx:      Default :data:`TransactionSignCallback` for certificate issuance.
    """

    def __init__(
        self,
        base_url: str = DEFAULT_BASE_URL,
        headers: Optional[dict] = None,
        timeout: int = 30,
        session: Optional[requests.Session] = None,
        sign_message: Optional[MessageSignCallback] = None,
        sign_tx: Optional[TransactionSignCallback] = None,
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._session = session or requests.Session()
        self._session.headers.update(
            {"Content-Type": "application/json", "Accept": "application/json"}
        )
        if headers:
            self._session.headers.update(headers)
        self._default_sign_message = sign_message
        self._default_sign_tx = sign_tx
        self.core = UVerifyCore(self)

    # -------------------------------------------------------------------------
    # Internal HTTP helpers
    # -------------------------------------------------------------------------

    def _request(self, method: str, path: str, json=None):
        url = f"{self._base_url}{path}"
        response = self._session.request(
            method, url, json=json, timeout=self._timeout
        )
        if not response.ok:
            try:
                body = response.json()
            except Exception:
                body = response.text
            raise UVerifyApiError(
                f"UVerify API error {response.status_code}: {response.reason}",
                status_code=response.status_code,
                response_body=body,
            )
        if not response.text:
            return None
        return response.json()

    def _get(self, path: str):
        return self._request("GET", path)

    def _post(self, path: str, data):
        return self._request("POST", path, json=data)

    # -------------------------------------------------------------------------
    # Callback resolution
    # -------------------------------------------------------------------------

    def _resolve_sign_message(
        self, per_call: Optional[MessageSignCallback]
    ) -> MessageSignCallback:
        cb = per_call if per_call is not None else self._default_sign_message
        if cb is None:
            raise UVerifyValidationError(
                "A sign_message callback is required. Pass one to the method or set a "
                "default via UVerifyClient(sign_message=...)."
            )
        return cb

    def _resolve_sign_tx(
        self, per_call: Optional[TransactionSignCallback]
    ) -> TransactionSignCallback:
        cb = per_call if per_call is not None else self._default_sign_tx
        if cb is None:
            raise UVerifyValidationError(
                "A sign_tx callback is required. Pass one to the method or set a "
                "default via UVerifyClient(sign_tx=...)."
            )
        return cb

    # -------------------------------------------------------------------------
    # Private low-level methods (exposed via .core)
    # -------------------------------------------------------------------------

    def _build_transaction(
        self, request: BuildTransactionRequest
    ) -> BuildTransactionResponse:
        data = self._post("/api/v1/transaction/build", request.to_dict())
        return BuildTransactionResponse.from_dict(data)

    def _submit_transaction(
        self, transaction: str, witness_set: Optional[str] = None
    ) -> None:
        body: dict = {"transaction": transaction}
        if witness_set is not None:
            body["witnessSet"] = witness_set
        self._post("/api/v1/transaction/submit", body)

    def _request_user_action(
        self, request: UserActionRequest
    ) -> UserActionRequestResponse:
        data = self._post("/api/v1/user/request/action", request.to_dict())
        return UserActionRequestResponse.from_dict(data)

    def _execute_user_action(
        self, request: ExecuteUserActionRequest
    ) -> ExecuteUserActionResponse:
        data = self._post("/api/v1/user/state/action", request.to_dict())
        return ExecuteUserActionResponse.from_dict(data)

    def _request_faucet_challenge(self, address: str) -> FaucetChallengeResponse:
        data = self._post("/api/v1/faucet/request", {"address": address})
        return FaucetChallengeResponse.from_dict(data)

    def _claim_faucet_funds(self, request: FaucetClaimRequest) -> FaucetClaimResponse:
        data = self._post("/api/v1/faucet/claim", request.to_dict())
        return FaucetClaimResponse.from_dict(data)

    def _perform_user_state_action(
        self,
        address: str,
        action: UserAction,
        sign_message: Optional[MessageSignCallback],
        state_id: Optional[str] = None,
    ) -> ExecuteUserActionResponse:
        cb = self._resolve_sign_message(sign_message)
        request_response = self._request_user_action(
            UserActionRequest(address=address, action=action, state_id=state_id)
        )
        sig = cb(request_response.message)
        return self._execute_user_action(
            ExecuteUserActionRequest(
                address=request_response.address,
                action=request_response.action,
                message=request_response.message,
                signature=request_response.signature,
                user_signature=sig.signature,
                user_public_key=sig.key,
                timestamp=request_response.timestamp,
                state_id=state_id,
            )
        )

    # -------------------------------------------------------------------------
    # Certificate Verification
    # -------------------------------------------------------------------------

    def verify(self, hash: str) -> List[CertificateResponse]:
        """
        Retrieve all on-chain certificates associated with the given data hash.

        Args:
            hash: SHA-256 or SHA-512 hex hash of the data to look up.

        Returns:
            A list of :class:`~uverify_sdk.models.CertificateResponse` objects.

        Example::

            certs = client.verify("a3b4c5d6...")
            for cert in certs:
                print(cert.transaction_hash, cert.creation_time)
        """
        try:
            data = self._get(f"/api/v1/verify/{hash}")
        except UVerifyApiError as e:
            if e.status_code == 404:
                return []
            raise
        return [CertificateResponse.from_dict(item) for item in (data or [])]

    def verify_by_transaction(
        self, transaction_hash: str, data_hash: str
    ) -> CertificateResponse:
        """
        Retrieve a single certificate by transaction hash and data hash.

        Args:
            transaction_hash: 64-character hex Cardano transaction hash.
            data_hash:        SHA-256 or SHA-512 hex hash of the certified data.
        """
        data = self._get(
            f"/api/v1/verify/by-transaction-hash/{transaction_hash}/{data_hash}"
        )
        return CertificateResponse.from_dict(data)

    # -------------------------------------------------------------------------
    # High-level helpers
    # -------------------------------------------------------------------------

    def issue_certificates(
        self,
        address: str,
        certificates: List[CertificateData],
        sign_tx: Optional[TransactionSignCallback] = None,
        state_id: Optional[str] = None,
    ) -> None:
        """
        Build and submit a certificate issuance transaction.

        Uses the bootstrap (new-state) flow when ``state_id`` is ``None``,
        or the default (existing-state) flow otherwise.

        Args:
            address:      Cardano address of the signer.
            certificates: Certificates to issue.
            sign_tx:      Wallet callback; falls back to constructor-level default.
            state_id:     Existing state ID. Omit for bootstrap flow.

        Raises:
            :exc:`~uverify_sdk.exceptions.UVerifyValidationError`:
                If no ``sign_tx`` callback is available.

        Example::

            client.issue_certificates(
                address="addr1...",
                certificates=[CertificateData(hash="sha256-hash", algorithm="SHA-256")],
            )
        """
        cb = self._resolve_sign_tx(sign_tx)
        request = BuildTransactionRequest(
            type="default" if state_id else "bootstrap",
            address=address,
            certificates=certificates,
            state_id=state_id,
        )
        max_attempts = 3
        last_error: Optional[Exception] = None
        for attempt in range(1, max_attempts + 1):
            try:
                response = self._build_transaction(request)
                witness_set = cb(response.unsigned_transaction)
                self._submit_transaction(response.unsigned_transaction, witness_set)
                return
            except UVerifyApiError as exc:
                last_error = exc
                if attempt < max_attempts:
                    print(
                        f"Certificate issuance failed (attempt {attempt}/{max_attempts}), "
                        "retrying in 5 s (waiting for chain state to propagate) …"
                    )
                    time.sleep(5)
        raise last_error  # type: ignore[misc]

    def get_user_info(
        self,
        address: str,
        sign_message: Optional[MessageSignCallback] = None,
    ) -> ExecuteUserActionResponse:
        """
        Retrieve the user's on-chain state (two-step signed action).

        Args:
            address:      Cardano address of the user.
            sign_message: Wallet callback; falls back to constructor-level default.

        Returns:
            :class:`~uverify_sdk.models.ExecuteUserActionResponse` which may
            contain the user's :class:`~uverify_sdk.models.UserState`.

        Example::

            resp = client.get_user_info(
                address="addr1...",
                sign_message=lambda msg: DataSignature(key=..., signature=...),
            )
            print(resp.state)
        """
        return self._perform_user_state_action(address, "USER_INFO", sign_message)

    def invalidate_state(
        self,
        address: str,
        state_id: str,
        sign_message: Optional[MessageSignCallback] = None,
    ) -> ExecuteUserActionResponse:
        """
        Invalidate a specific on-chain state.

        Args:
            address:      Cardano address of the user.
            state_id:     ID of the state to invalidate.
            sign_message: Wallet callback; falls back to constructor-level default.
        """
        return self._perform_user_state_action(
            address, "INVALIDATE_STATE", sign_message, state_id=state_id
        )

    def opt_out(
        self,
        address: str,
        state_id: str,
        sign_message: Optional[MessageSignCallback] = None,
    ) -> ExecuteUserActionResponse:
        """
        Opt out from a specific on-chain state.

        Args:
            address:      Cardano address of the user.
            state_id:     ID of the state to opt out from.
            sign_message: Wallet callback; falls back to constructor-level default.
        """
        return self._perform_user_state_action(
            address, "OPT_OUT", sign_message, state_id=state_id
        )

    def fund_wallet(
        self,
        address: str,
        sign_message: Optional[MessageSignCallback] = None,
    ) -> FaucetClaimResponse:
        """
        Fund a wallet with testnet ADA from the UVerify dev faucet in a single step.

        Orchestrates the full challenge-sign-claim flow:

        1. Requests a server-signed challenge for ``address``.
        2. Passes the challenge message to ``sign_message`` (CIP-30 ``signData``).
        3. Submits both signatures to the faucet claim endpoint.

        Returns a :class:`~uverify_sdk.models.FaucetClaimResponse` with the
        Cardano transaction hash on success. Use :func:`~uverify_sdk.wait_for`
        to poll until the funds are queryable on-chain.

        **Only available when the backend is configured with** ``FAUCET_ENABLED=true``.
        This is intended for testnet development environments only.

        Args:
            address:      Cardano address that should receive the testnet funds.
            sign_message: Wallet callback; falls back to constructor-level default.

        Example::

            from uverify_sdk import UVerifyClient, DataSignature, wait_for

            client = UVerifyClient(
                base_url="http://localhost:9090",
                sign_message=lambda msg: DataSignature(key=..., signature=...),
            )
            result = client.fund_wallet("addr_test1...")
            print("Funded by tx:", result.tx_hash)
        """
        cb = self._resolve_sign_message(sign_message)
        try:
            challenge = self._request_faucet_challenge(address)
            sig = cb(challenge.message)
            return self._claim_faucet_funds(
                FaucetClaimRequest(
                    address=address,
                    message=challenge.message,
                    signature=challenge.signature,
                    user_signature=sig.signature,
                    user_public_key=sig.key,
                    timestamp=challenge.timestamp,
                )
            )
        except UVerifyApiError as e:
            if e.status_code == 404:
                raise UVerifyApiError(
                    "Faucet endpoint not found (HTTP 404). "
                    "The faucet is only available on backends started with FAUCET_ENABLED=true. "
                    "This feature does not exist on mainnet — acquire ADA from a cryptocurrency exchange instead.",
                    404,
                    e.response_body,
                ) from e
            if e.status_code == 429:
                raise UVerifyApiError(
                    "Faucet cooldown active (HTTP 429). "
                    "This address recently received testnet funds. Please wait a few minutes before trying again.",
                    429,
                    e.response_body,
                ) from e
            raise


# ---------------------------------------------------------------------------
# Polling utility
# ---------------------------------------------------------------------------


def wait_for(
    condition: Callable[[], Union[_T, bool]],
    timeout_ms: int = 60_000,
    interval_ms: int = 2_000,
) -> _T:
    """
    Poll *condition* every *interval_ms* milliseconds until it returns a
    non-``False`` value, then return that value.

    Return ``False`` (the boolean literal) from your condition to keep
    polling; return any other value — including ``True``, an object, or a
    list — to stop and return it. Errors raised by the condition propagate
    immediately without retrying.

    Args:
        condition:    Callable that returns ``False`` to keep polling, or any
                      other value to stop and return it.
        timeout_ms:   Maximum wait in milliseconds. Default: 60 000 (1 min).
        interval_ms:  Delay between polls in milliseconds. Default: 2 000 (2 s).

    Returns:
        The first non-``False`` value returned by *condition*.

    Raises:
        UVerifyTimeoutError: If *condition* keeps returning ``False`` until
                             *timeout_ms* has elapsed.

    Example — poll until a certificate is queryable::

        from uverify_sdk import UVerifyClient, wait_for, UVerifyTimeoutError

        client = UVerifyClient()

        try:
            wait_for(lambda: len(client.verify(data_hash)) > 0, timeout_ms=300_000)
        except UVerifyTimeoutError as e:
            print(e)

    Example — return the certificates once they appear::

        certs = wait_for(
            lambda: client.verify(data_hash) or False,
            timeout_ms=300_000,
        )
    """
    import time
    from .exceptions import UVerifyTimeoutError

    deadline = time.monotonic() + timeout_ms / 1000.0
    while time.monotonic() < deadline:
        result = condition()
        if result is not False:
            return result  # type: ignore[return-value]
        time.sleep(interval_ms / 1000.0)
    raise UVerifyTimeoutError(timeout_ms)
