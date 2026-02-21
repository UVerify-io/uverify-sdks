"""Main UVerify API client."""
from __future__ import annotations

from typing import Any, Dict, List, Optional

import requests

from .exceptions import UVerifyApiError
from .models.certificate import CertificateData, CertificateResponse
from .models.connected_goods import (
    ClaimUpdateConnectedGoodsRequest,
    MintConnectedGoodsRequest,
    MintConnectedGoodsResponse,
)
from .models.transaction import (
    BuildTransactionRequest,
    BuildTransactionResponse,
    TxContractDetails,
    TxRedeemerDto,
)
from .models.user_state import (
    ExecuteUserActionRequest,
    ExecuteUserActionResponse,
    UserAction,
    UserActionRequest,
    UserActionRequestResponse,
)

DEFAULT_BASE_URL = "https://api.uverify.io"


class UVerifyClient:
    """
    Main entry point for the UVerify SDK.

    Example::

        from uverify_sdk import UVerifyClient

        client = UVerifyClient()
        certificates = client.verify("a3b4c5d6...")
        print(certificates)

    Args:
        base_url: Base URL of the UVerify API.
                  Defaults to ``https://api.uverify.io``.
        headers:  Additional HTTP headers added to every request.
        timeout:  Request timeout in seconds. Defaults to 30.
        session:  Optional ``requests.Session`` to use (useful for testing).
    """

    def __init__(
        self,
        base_url: str = DEFAULT_BASE_URL,
        headers: Optional[Dict[str, str]] = None,
        timeout: int = 30,
        session: Optional[requests.Session] = None,
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._session = session or requests.Session()
        self._session.headers.update(
            {"Content-Type": "application/json", "Accept": "application/json"}
        )
        if headers:
            self._session.headers.update(headers)

    # -------------------------------------------------------------------------
    # Internal helpers
    # -------------------------------------------------------------------------

    def _request(
        self,
        method: str,
        path: str,
        json: Optional[Any] = None,
    ) -> Any:
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

    def _get(self, path: str) -> Any:
        return self._request("GET", path)

    def _post(self, path: str, data: Any) -> Any:
        return self._request("POST", path, json=data)

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
        data = self._get(f"/api/v1/verify/{hash}")
        return [CertificateResponse.from_dict(item) for item in (data or [])]

    def verify_by_transaction(
        self, transaction_hash: str, data_hash: str
    ) -> CertificateResponse:
        """
        Retrieve a single certificate by transaction hash and data hash.

        Args:
            transaction_hash: 64-character hex Cardano transaction hash.
            data_hash:        SHA-256 or SHA-512 hex hash of the certified data.

        Returns:
            A :class:`~uverify_sdk.models.CertificateResponse`.
        """
        data = self._get(
            f"/api/v1/verify/by-transaction-hash/{transaction_hash}/{data_hash}"
        )
        return CertificateResponse.from_dict(data)

    # -------------------------------------------------------------------------
    # Transaction Management
    # -------------------------------------------------------------------------

    def build_transaction(
        self, request: BuildTransactionRequest
    ) -> BuildTransactionResponse:
        """
        Build an unsigned transaction for certificate issuance or state management.

        The returned ``unsigned_transaction`` is a CBOR-hex encoded transaction
        that must be signed by the user's wallet before being submitted via
        :meth:`submit_transaction`.

        Example::

            from uverify_sdk import UVerifyClient
            from uverify_sdk.models import BuildTransactionRequest, CertificateData

            client = UVerifyClient()
            response = client.build_transaction(
                BuildTransactionRequest(
                    type="default",
                    address="addr1...",
                    state_id="my-state-id",
                    certificates=[CertificateData(hash="sha256-hash", algorithm="SHA-256")],
                )
            )
            print(response.unsigned_transaction)
        """
        data = self._post("/api/v1/transaction/build", request.to_dict())
        return BuildTransactionResponse.from_dict(data)

    def submit_transaction(
        self, transaction: str, witness_set: Optional[str] = None
    ) -> None:
        """
        Submit a signed transaction to the Cardano blockchain.

        Args:
            transaction: CBOR-hex encoded signed transaction.
            witness_set: Optional separate witness set in CBOR-hex.
        """
        body: Dict[str, Any] = {"transaction": transaction}
        if witness_set is not None:
            body["witnessSet"] = witness_set
        self._post("/api/v1/transaction/submit", body)

    # -------------------------------------------------------------------------
    # User State Management
    # -------------------------------------------------------------------------

    def request_user_action(
        self, request: UserActionRequest
    ) -> UserActionRequestResponse:
        """
        Create a server-signed action request that the user must countersign.

        This is step 1 of the two-step user-state-action flow.
        Pass the returned ``message`` to your wallet for signing, then call
        :meth:`execute_user_action` with the combined signatures.

        Example::

            req = client.request_user_action(
                UserActionRequest(address="addr1...", action="USER_INFO")
            )
            # Sign req.message with your CIP-30 wallet...
        """
        data = self._post("/api/v1/user/request/action", request.to_dict())
        return UserActionRequestResponse.from_dict(data)

    def execute_user_action(
        self, request: ExecuteUserActionRequest
    ) -> ExecuteUserActionResponse:
        """
        Execute a user state action using signatures from both parties.

        This is step 2 of the two-step flow. See :meth:`request_user_action`.
        """
        data = self._post("/api/v1/user/state/action", request.to_dict())
        return ExecuteUserActionResponse.from_dict(data)

    # -------------------------------------------------------------------------
    # Connected Goods Extension
    # -------------------------------------------------------------------------

    def mint_connected_goods_batch(
        self, request: MintConnectedGoodsRequest
    ) -> MintConnectedGoodsResponse:
        """
        Build an unsigned transaction to mint a batch of connected-goods tokens.

        Example::

            from uverify_sdk.models import (
                MintConnectedGoodsRequest, ConnectedGoodsItemInput
            )

            response = client.mint_connected_goods_batch(
                MintConnectedGoodsRequest(
                    address="addr1...",
                    token_name="MY_ITEM",
                    items=[
                        ConnectedGoodsItemInput(password="secret1", asset_name="ITEM001"),
                        ConnectedGoodsItemInput(password="secret2", asset_name="ITEM002"),
                    ],
                )
            )
            print(response.batch_id)
        """
        data = self._post(
            "/api/v1/extension/connected-goods/mint/batch", request.to_dict()
        )
        return MintConnectedGoodsResponse.from_dict(data)

    def claim_connected_goods_item(
        self, request: ClaimUpdateConnectedGoodsRequest
    ) -> None:
        """Claim a connected-goods item using its password."""
        self._post(
            "/api/v1/extension/connected-goods/claim/item", request.to_dict()
        )

    def update_connected_goods_item(
        self, request: ClaimUpdateConnectedGoodsRequest
    ) -> None:
        """Update the social profile associated with a claimed item."""
        self._post(
            "/api/v1/extension/connected-goods/update/item", request.to_dict()
        )

    def get_connected_goods_item(
        self, batch_ids: str, item_id: str
    ) -> Any:
        """
        Retrieve a connected-goods item.

        Args:
            batch_ids: Comma-separated batch IDs.
            item_id:   Item identifier within the batch.
        """
        return self._get(
            f"/api/v1/extension/connected-goods/{batch_ids}/{item_id}"
        )

    # -------------------------------------------------------------------------
    # Transaction Service
    # -------------------------------------------------------------------------

    def get_transaction_scripts(self, tx_hash: str) -> List[TxContractDetails]:
        """Retrieve all contract scripts used in a transaction."""
        data = self._get(f"/api/v1/txs/{tx_hash}/scripts")
        return [TxContractDetails.from_dict(item) for item in (data or [])]

    def get_transaction_redeemers(self, tx_hash: str) -> List[TxRedeemerDto]:
        """Retrieve redeemer data from a transaction."""
        data = self._get(f"/api/v1/txs/{tx_hash}/redeemers")
        return [TxRedeemerDto.from_dict(item) for item in (data or [])]

    # -------------------------------------------------------------------------
    # Script Service
    # -------------------------------------------------------------------------

    def get_script(self, script_hash: str) -> Any:
        """Get basic information about a script."""
        return self._get(f"/api/v1/scripts/{script_hash}")

    def get_script_json(self, script_hash: str) -> Any:
        """Get the JSON representation of a script."""
        return self._get(f"/api/v1/scripts/{script_hash}/json")

    def get_script_details(self, script_hash: str) -> Any:
        """Get detailed information about a script including its content."""
        return self._get(f"/api/v1/scripts/{script_hash}/details")

    def get_script_cbor(self, script_hash: str) -> Any:
        """Get the CBOR encoding of a script."""
        return self._get(f"/api/v1/scripts/{script_hash}/cbor")

    def get_datum(self, datum_hash: str) -> Any:
        """Get a datum by its hash."""
        return self._get(f"/api/v1/scripts/datum/{datum_hash}")

    def get_datum_cbor(self, datum_hash: str) -> Any:
        """Get the CBOR encoding of a datum by its hash."""
        return self._get(f"/api/v1/scripts/datum/{datum_hash}/cbor")

    # -------------------------------------------------------------------------
    # Library Controller
    # -------------------------------------------------------------------------

    def deploy_proxy(self) -> None:
        """Deploy a proxy contract for state management."""
        self._post("/api/v1/library/deploy/proxy", {})

    def upgrade_proxy(self, params: str) -> None:
        """Upgrade an existing proxy contract."""
        self._post("/api/v1/library/upgrade/proxy", params)

    def get_deployments(self) -> Any:
        """List all current library deployments."""
        return self._get("/api/v1/library/deployments")

    def undeploy_contract(self, transaction_hash: str, output_index: int) -> Any:
        """
        Undeploy a specific contract.

        Args:
            transaction_hash: Transaction hash of the deployment.
            output_index:     Output index of the deployment UTXO.
        """
        return self._get(
            f"/api/v1/library/undeploy/{transaction_hash}/{output_index}"
        )

    def undeploy_unused(self) -> Any:
        """Undeploy all unused contract instances."""
        return self._get("/api/v1/library/undeploy/unused")

    # -------------------------------------------------------------------------
    # Statistics
    # -------------------------------------------------------------------------

    def get_transaction_fees(self) -> Any:
        """Get transaction fee statistics."""
        return self._get("/api/v1/statistic/tx-fees")

    def get_certificates_by_category(self) -> Any:
        """Get certificate counts grouped by category."""
        return self._get("/api/v1/statistic/certificate/by-category")
