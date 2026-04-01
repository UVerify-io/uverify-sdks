"""High-level application helpers for issuing well-known certificate types."""
from __future__ import annotations

import hashlib
import json
from typing import Any, Callable, Dict, List, Optional
from urllib.parse import urlencode

import requests as _requests

from ..models.certificate import CertificateData
from .types import (
    DigitalProductPassportInput,
    DigitalProductPassportResult,
    DiplomaCertificateResult,
    DiplomaInput,
    DiplomaResult,
    LaboratoryReportCertificateResult,
    LaboratoryReportInput,
    LaboratoryReportResult,
    TokenizableCertificateClaimInput,
    TokenizableCertificateInput,
    TokenizableCertificateResult,
    TokenizableCertificateStatus,
)

# Type alias for the bound issue_certificates method passed in from UVerifyClient.
# (address, certificates, sign_tx) -> tx_hash
_IssueFn = Callable[[str, List[CertificateData], Optional[Callable[[str], str]]], str]


def _sha256hex(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


class UVerifyApps:
    """
    High-level application helpers for issuing well-known certificate types.

    Accessible via :attr:`~uverify_sdk.UVerifyClient.apps`. Each method handles
    hashing, metadata construction, and verification URL generation automatically.

    Example::

        result = client.apps.issue_diploma(address, [
            DiplomaInput(
                student_id='TUM-2021-0042',
                name='Maria Müller',
                degree='Master of Science in Computer Science',
                institution='Technical University of Munich',
                graduation_date='2024-06-28',
                honors='Summa Cum Laude',
            ),
        ])
    """

    def __init__(
        self,
        issue_fn: _IssueFn,
        verify_base_url: str,
        base_url: Optional[str] = None,
        session: Optional[_requests.Session] = None,
        sign_tx: Optional[Callable[[str], str]] = None,
        submit_fn: Optional[Callable[[str, Optional[str]], str]] = None,
    ) -> None:
        self._issue = issue_fn
        self._verify_base_url = verify_base_url
        self._base_url = base_url
        self._session = session
        self._default_sign_tx = sign_tx
        self._submit_fn = submit_fn

    def _require_extension_support(self, method_name: str):
        if not self._base_url or not self._default_sign_tx or not self._submit_fn or not self._session:
            raise RuntimeError(
                f"{method_name} requires the UVerify client to be configured with "
                f"a base_url, sign_tx callback, and submit_fn. "
                f"Ensure you are using UVerifyClient to access client.apps."
            )

    def _extension_post(self, path: str, body: Dict[str, Any]) -> Any:
        url = f"{self._base_url}{path}"
        response = self._session.post(url, json=body)
        if not response.ok:
            raise RuntimeError(
                f"UVerify extension error {response.status_code}: {response.text}"
            )
        if not response.text:
            return None
        return response.json()

    def _extension_get(self, path: str) -> Any:
        url = f"{self._base_url}{path}"
        response = self._session.get(url)
        if not response.ok:
            raise RuntimeError(
                f"UVerify extension error {response.status_code}: {response.text}"
            )
        if not response.text:
            return None
        return response.json()

    def issue_diploma(
        self,
        address: str,
        diplomas: List[DiplomaInput],
        sign_tx: Optional[Callable[[str], str]] = None,
    ) -> DiplomaResult:
        """
        Issue one or more diploma certificates in a single transaction.

        Each diploma's ``student_id`` is hashed with SHA-256 to produce the
        on-chain fingerprint. The recipient's ``name`` is stored as a hash and
        revealed only via the ``?name=`` URL parameter in the returned
        verification link.

        The update policy is ``first`` — the initial issuance is permanent and
        cannot be overwritten.

        Args:
            address:  Cardano address of the signer / payer.
            diplomas: One or more diploma inputs.
            sign_tx:  Wallet callback; falls back to constructor-level default.

        Returns:
            :class:`DiplomaResult` with the transaction hash and per-diploma
            verification URLs.
        """
        hashes = [_sha256hex(d.student_id) for d in diplomas]
        name_hashes = [_sha256hex(d.name) for d in diplomas]

        certs = []
        for i, d in enumerate(diplomas):
            description = (
                f"This is to certify that the above-named individual has successfully "
                f"completed all requirements for the degree of {d.degree} at {d.institution}"
                + (f", awarded with {d.honors}" if d.honors else "")
                + f", on {d.graduation_date}."
            )
            certs.append(
                CertificateData(
                    hash=hashes[i],
                    algorithm="SHA-256",
                    metadata={
                        "uverify_template_id": "diploma",
                        "uverify_update_policy": "first",
                        "issuer": d.institution,
                        "uv_url_name": name_hashes[i],
                        "title": d.degree,
                        "description": description,
                    },
                )
            )

        tx_hash = self._issue(address, certs, sign_tx)

        certificates = [
            DiplomaCertificateResult(
                student_id=d.student_id,
                name=d.name,
                hash=hashes[i],
                verify_url=(
                    f"{self._verify_base_url}/{hashes[i]}/{tx_hash}"
                    f"?{urlencode({'name': d.name})}"
                ),
            )
            for i, d in enumerate(diplomas)
        ]
        return DiplomaResult(tx_hash=tx_hash, certificates=certificates)

    def issue_digital_product_passport(
        self,
        address: str,
        product: DigitalProductPassportInput,
        sign_tx: Optional[Callable[[str], str]] = None,
    ) -> DigitalProductPassportResult:
        """
        Issue a Digital Product Passport for a single product unit.

        The hash is computed as ``sha256(gtin + serial_number)``, uniquely
        identifying this product instance. The serial number is stored as a hash
        on-chain and revealed via ``?serial=`` in the verification link.

        ``materials`` keys receive a ``mat_`` prefix automatically;
        ``certifications`` keys receive a ``cert_`` prefix automatically.

        The update policy is ``restricted`` — only the issuer wallet may push
        subsequent corrections.

        Args:
            address: Cardano address of the signer / payer.
            product: Product passport input.
            sign_tx: Wallet callback; falls back to constructor-level default.

        Returns:
            :class:`DigitalProductPassportResult` with the transaction hash and
            verification URL.
        """
        data_hash = _sha256hex(product.gtin + product.serial_number)
        serial_hash = _sha256hex(product.serial_number)

        metadata: dict = {
            "uverify_template_id": "digitalProductPassport",
            "uverify_update_policy": "restricted",
            "name": product.name,
            "issuer": product.manufacturer,
            "gtin": product.gtin,
            "uv_url_serial": serial_hash,
        }
        optional_fields = {
            "model": product.model,
            "origin": product.origin,
            "manufactured": product.manufactured,
            "contact": product.contact,
            "brand_color": product.brand_color,
            "carbon_footprint": product.carbon_footprint,
            "recycled_content": product.recycled_content,
            "energy_class": product.energy_class,
            "warranty": product.warranty,
            "spare_parts": product.spare_parts,
            "repair_info": product.repair_info,
            "recycling": product.recycling,
        }
        for key, value in optional_fields.items():
            if value is not None:
                metadata[key] = value
        for key, value in (product.materials or {}).items():
            metadata[f"mat_{key}"] = value
        for key, value in (product.certifications or {}).items():
            metadata[f"cert_{key}"] = value

        tx_hash = self._issue(
            address,
            [CertificateData(hash=data_hash, algorithm="SHA-256", metadata=metadata)],
            sign_tx,
        )

        verify_url = (
            f"{self._verify_base_url}/{data_hash}/{tx_hash}"
            f"?{urlencode({'serial': product.serial_number})}"
        )
        return DigitalProductPassportResult(
            tx_hash=tx_hash, hash=data_hash, verify_url=verify_url
        )

    def issue_laboratory_report(
        self,
        address: str,
        reports: List[LaboratoryReportInput],
        sign_tx: Optional[Callable[[str], str]] = None,
    ) -> LaboratoryReportResult:
        """
        Issue one or more laboratory report certificates in a single transaction.

        Each report is identified by ``sha256(report_id)``. Patient name and
        report ID are stored as hashes on-chain and revealed via ``?name=`` and
        ``?report_id=`` URL parameters in the returned verification links.

        ``values`` keys receive an ``a_`` prefix automatically.

        The update policy is ``first`` — the initial issuance is permanent and
        cannot be overwritten.

        Args:
            address: Cardano address of the signer / payer.
            reports: One or more laboratory report inputs.
            sign_tx: Wallet callback; falls back to constructor-level default.

        Returns:
            :class:`LaboratoryReportResult` with the transaction hash and
            per-report verification URLs.
        """
        hashes = [_sha256hex(r.report_id) for r in reports]
        patient_hashes = [_sha256hex(r.patient_name) for r in reports]

        certs = []
        for i, r in enumerate(reports):
            values = {f"a_{k}": v for k, v in r.values.items()}
            metadata: dict = {
                "uverify_template_id": "laboratoryReport",
                "uverify_update_policy": "first",
                "issuer": r.lab_name,
                "uv_url_name": patient_hashes[i],
                "uv_url_report_id": hashes[i],
                "auditable": str(r.auditable).lower(),
                **values,
            }
            if r.contact is not None:
                metadata["contact"] = r.contact
            certs.append(
                CertificateData(hash=hashes[i], algorithm="SHA-256", metadata=metadata)
            )

        tx_hash = self._issue(address, certs, sign_tx)

        certificates = [
            LaboratoryReportCertificateResult(
                report_id=r.report_id,
                patient_name=r.patient_name,
                hash=hashes[i],
                verify_url=(
                    f"{self._verify_base_url}/{hashes[i]}/{tx_hash}"
                    f"?{urlencode({'name': r.patient_name, 'report_id': r.report_id})}"
                ),
            )
            for i, r in enumerate(reports)
        ]
        return LaboratoryReportResult(tx_hash=tx_hash, certificates=certificates)


    def issue_tokenizable_certificate(
        self,
        address: str,
        input: TokenizableCertificateInput,
        sign_tx: Optional[Callable[[str], str]] = None,
    ) -> TokenizableCertificateResult:
        """
        Issue a tokenizable certificate — inserts a node into the on-chain sorted
        linked list and mints a CIP-68 NFT pair for the recipient.

        Requires the ``tokenizable-certificate`` backend extension to be enabled.

        Args:
            address:  Cardano address of the inserter / fee payer.
            input:    Certificate parameters (key, owner, asset name, init UTxO).
            sign_tx:  Wallet callback; falls back to constructor-level default.

        Returns:
            :class:`TokenizableCertificateResult` with the transaction hash, key,
            and verification URL.
        """
        self._require_extension_support("issue_tokenizable_certificate")
        sign = sign_tx or self._default_sign_tx

        body = {
            "inserterAddress": address,
            "key": input.key,
            "ownerPubKeyHash": input.owner_pub_key_hash,
            "assetName": input.asset_name_hex,
            "initUtxoTxHash": input.init_utxo_tx_hash,
            "initUtxoOutputIndex": input.init_utxo_output_index,
        }
        if input.bootstrap_token_name is not None:
            body["bootstrapTokenName"] = input.bootstrap_token_name

        unsigned_tx = self._extension_post(
            "/api/v1/extension/tokenizable-certificate/insert", body
        )
        witness_set = sign(unsigned_tx)
        tx_hash = self._submit_fn(unsigned_tx, witness_set)

        return TokenizableCertificateResult(
            tx_hash=tx_hash,
            key=input.key,
            verify_url=f"{self._verify_base_url}/{input.key}/{tx_hash}",
        )

    def get_tokenizable_certificate_status(
        self,
        key: str,
        init_utxo_tx_hash: str,
        init_utxo_output_index: int,
    ) -> TokenizableCertificateStatus:
        """
        Query the on-chain status of a tokenizable certificate node.

        Requires the ``tokenizable-certificate`` backend extension to be enabled.

        Args:
            key:                    On-chain certificate key (SHA-256 hash).
            init_utxo_tx_hash:      Transaction hash of the Init UTxO.
            init_utxo_output_index: Output index of the Init UTxO.

        Returns:
            :class:`TokenizableCertificateStatus` indicating whether the certificate
            has been claimed and who currently holds the token.
        """
        self._require_extension_support("get_tokenizable_certificate_status")
        path = (
            f"/api/v1/extension/tokenizable-certificate/status/{key}"
            f"?initUtxoTxHash={init_utxo_tx_hash}&initUtxoOutputIndex={init_utxo_output_index}"
        )
        data = self._extension_get(path)
        return TokenizableCertificateStatus(
            key=data["key"],
            claimed=data["claimed"],
            owner=data.get("owner"),
        )

    def redeem_tokenizable_certificate(
        self,
        input: TokenizableCertificateClaimInput,
        sign_tx: Optional[Callable[[str], str]] = None,
    ) -> str:
        """
        Redeem (claim) a tokenizable certificate — the holder of the CIP-68 user NFT
        burns the token and removes the node from the on-chain linked list.

        Requires the ``tokenizable-certificate`` backend extension to be enabled.

        Args:
            input:    Claim parameters (key, claimer address, init UTxO, asset name).
            sign_tx:  Wallet callback; falls back to constructor-level default.

        Returns:
            The Cardano transaction hash of the claim transaction.
        """
        self._require_extension_support("redeem_tokenizable_certificate")
        sign = sign_tx or self._default_sign_tx

        unsigned_tx = self._extension_post(
            "/api/v1/extension/tokenizable-certificate/claim",
            {
                "claimerAddress": input.claimer_address,
                "key": input.key,
                "initUtxoTxHash": input.init_utxo_tx_hash,
                "initUtxoOutputIndex": input.init_utxo_output_index,
                "assetName": input.asset_name_hex,
            },
        )
        witness_set = sign(unsigned_tx)
        return self._submit_fn(unsigned_tx, witness_set)


__all__ = [
    "UVerifyApps",
    "DiplomaInput",
    "DiplomaResult",
    "DiplomaCertificateResult",
    "DigitalProductPassportInput",
    "DigitalProductPassportResult",
    "LaboratoryReportInput",
    "LaboratoryReportResult",
    "LaboratoryReportCertificateResult",
    "TokenizableCertificateInput",
    "TokenizableCertificateResult",
    "TokenizableCertificateStatus",
    "TokenizableCertificateClaimInput",
]
