"""Input and result dataclasses for the UVerify high-level apps interface."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, Optional


@dataclass
class DiplomaInput:
    """Input for a single diploma certificate."""

    student_id: str
    """Student or learner identifier — used as the on-chain fingerprint."""
    name: str
    """Recipient's full name (stored as SHA-256 hash on-chain, revealed via URL param)."""
    degree: str
    """Degree or qualification title shown on the certificate."""
    institution: str
    """Name of the issuing institution."""
    graduation_date: str
    """Graduation or completion date (e.g. ``'2024-06-28'``)."""
    honors: Optional[str] = None
    """Optional honours distinction (e.g. ``'Summa Cum Laude'``)."""


@dataclass
class DiplomaCertificateResult:
    student_id: str
    name: str
    hash: str
    """SHA-256 hash of ``student_id`` — the on-chain certificate fingerprint."""
    verify_url: str
    """Verification URL with ``?name=`` pre-populated to reveal the recipient's name."""


@dataclass
class DiplomaResult:
    """Result returned by :meth:`~uverify_sdk.apps.UVerifyApps.issue_diploma`."""

    tx_hash: str
    """Cardano transaction hash of the issuance transaction."""
    certificates: List[DiplomaCertificateResult]


@dataclass
class DigitalProductPassportInput:
    """Input for a Digital Product Passport certificate."""

    name: str
    """Product name shown on the passport."""
    manufacturer: str
    """Manufacturer or issuing organisation."""
    gtin: str
    """Global Trade Item Number (GTIN / barcode)."""
    serial_number: str
    """Unit-level serial number (stored as SHA-256 hash on-chain, revealed via URL param)."""
    model: Optional[str] = None
    """Model number or product line identifier."""
    origin: Optional[str] = None
    """Country or place of manufacture."""
    manufactured: Optional[str] = None
    """Date of manufacture (e.g. ``'2024-08-15'``)."""
    contact: Optional[str] = None
    """Contact e-mail or URL for sustainability enquiries."""
    brand_color: Optional[str] = None
    """Brand accent colour as a hex string (e.g. ``'#1a56db'``)."""
    carbon_footprint: Optional[str] = None
    """Carbon footprint (e.g. ``'1.2 kg CO₂e'``)."""
    recycled_content: Optional[str] = None
    """Percentage of recycled content (e.g. ``'38%'``)."""
    energy_class: Optional[str] = None
    """EU energy class (e.g. ``'A++'``)."""
    warranty: Optional[str] = None
    """Warranty period (e.g. ``'3 years'``)."""
    spare_parts: Optional[str] = None
    """Spare parts availability statement."""
    repair_info: Optional[str] = None
    """URL to repair instructions."""
    recycling: Optional[str] = None
    """End-of-life / recycling instructions."""
    materials: Optional[Dict[str, str]] = None
    """
    Material composition entries.
    Keys are plain material names — the ``mat_`` prefix is added automatically.
    Example: ``{'aluminum': '45%', 'recycled_plastic': '38%'}``
    """
    certifications: Optional[Dict[str, str]] = None
    """
    Certification entries.
    Keys are plain identifiers — the ``cert_`` prefix is added automatically.
    Example: ``{'ce': 'CE Marking', 'rohs': 'RoHS Compliant'}``
    """


@dataclass
class DigitalProductPassportResult:
    """Result returned by :meth:`~uverify_sdk.apps.UVerifyApps.issue_digital_product_passport`."""

    tx_hash: str
    """Cardano transaction hash of the issuance transaction."""
    hash: str
    """SHA-256 hash of ``gtin + serial_number`` — the on-chain certificate fingerprint."""
    verify_url: str
    """Verification URL with ``?serial=`` pre-populated to reveal the serial number."""


@dataclass
class LaboratoryReportInput:
    """Input for a single laboratory report certificate."""

    report_id: str
    """Unique report identifier — used as the on-chain fingerprint."""
    patient_name: str
    """Patient's full name (stored as SHA-256 hash on-chain, revealed via URL param)."""
    lab_name: str
    """Name of the issuing laboratory."""
    values: Dict[str, str]
    """
    Measured parameter values.
    Keys are plain parameter names — the ``a_`` prefix is added automatically.
    Example: ``{'glucose': '5.4 mmol/L', 'hba1c': '5.7%'}``
    """
    contact: Optional[str] = None
    """Contact e-mail or institution for the full report."""
    auditable: bool = False
    """
    Whether the measured values are publicly auditable.
    When ``True``, the certificate page shows a transparency banner.
    """


@dataclass
class LaboratoryReportCertificateResult:
    report_id: str
    patient_name: str
    hash: str
    """SHA-256 hash of ``report_id`` — the on-chain certificate fingerprint."""
    verify_url: str
    """Verification URL with ``?name=`` and ``?report_id=`` pre-populated."""


@dataclass
class LaboratoryReportResult:
    """Result returned by :meth:`~uverify_sdk.apps.UVerifyApps.issue_laboratory_report`."""

    tx_hash: str
    """Cardano transaction hash of the issuance transaction."""
    certificates: List[LaboratoryReportCertificateResult]


@dataclass
class CertificateOfInsuranceInput:
    """Input for a Certificate of Insurance (COI)."""

    policy_number: str
    """Unique policy reference number — used as the on-chain fingerprint."""
    insurer: str
    """Name of the issuing insurance company."""
    insured: str
    """Name of the insured business or individual (stored as SHA-256 hash on-chain)."""
    effective_date: str
    """Policy start date in ISO format (e.g. ``'2025-01-01'``)."""
    expiration_date: str
    """Policy end date in ISO format (e.g. ``'2026-01-01'``) — drives VALID/EXPIRED badge."""
    coverages: Dict[str, str]
    """Coverage limits keyed by coverage type. Keys get a ``cov_`` prefix automatically.
    Example: ``{'general_liability': '1,000,000', 'workers_compensation': '500,000'}``"""
    producer: Optional[str] = None
    """Name of the insurance broker or agent."""
    insured_address: Optional[str] = None
    """Address of the insured (stored as SHA-256 hash on-chain)."""
    certificate_holder: Optional[str] = None
    """Name of the party requiring proof of insurance (stored as SHA-256 hash on-chain)."""
    certificate_holder_address: Optional[str] = None
    """Address of the certificate holder (stored as SHA-256 hash on-chain)."""
    additional_insured: Optional[bool] = None
    """Whether the certificate holder is named as an additional insured."""
    waiver_of_subrogation: Optional[bool] = None
    """Whether a waiver of subrogation applies in favour of the certificate holder."""


@dataclass
class CertificateOfInsuranceResult:
    """Result returned by :meth:`~uverify_sdk.apps.UVerifyApps.issue_certificate_of_insurance`."""

    tx_hash: str
    """Cardano transaction hash of the issuance transaction."""
    hash: str
    """SHA-256 hash of ``policy_number`` — the on-chain certificate fingerprint."""
    verify_url: str
    """Verification URL for this certificate."""


@dataclass
class TokenizableConfig:
    """Configuration embedded in the HEAD datum on the very first Init transaction.

    Only required when no linked list exists yet for the given init UTxO.
    The ``uverify_validator_hash`` is filled in by the backend; all other fields
    come from the caller.
    """

    deployer: str
    """Payment key hash of the deployer wallet."""
    allowed_inserters: Optional[List[str]] = None
    """Payment key hashes of wallets allowed to insert nodes. Empty list means anyone can insert."""
    cip68_script_address: Optional[str] = None
    """Hex-encoded CIP-68 script hash. ``None`` disables CIP-68 minting."""


@dataclass
class TokenizableCertificateData:
    """Certificate fields passed to the tokenizable-certificate extension."""

    hash: str
    """SHA-256 hash of the certified content — becomes the on-chain node key."""
    metadata: Optional[str] = None
    """Optional JSON string of caller-supplied metadata merged on-chain."""


@dataclass
class TokenizableCertificateInput:
    """Input for issuing a tokenizable certificate (NFT-backed on-chain linked list node).

    Requires the ``tokenizable-certificate`` backend extension to be enabled.
    """

    certificate: TokenizableCertificateData
    """Certificate to register on-chain. ``hash`` becomes the node key; ``metadata``
    (JSON string) is merged with backend-generated fields and stored on-chain."""
    owner_pub_key_hash: str
    """Public key hash of the token owner (the recipient of the CIP-68 user NFT)."""
    asset_name_hex: str
    """Hex-encoded asset name for the minted CIP-68 label-222 user token."""
    init_utxo_tx_hash: str
    """Transaction hash of the Init UTxO that bootstrapped the on-chain linked list."""
    init_utxo_output_index: int
    """Output index of the Init UTxO."""
    bootstrap_token_name: Optional[str] = None
    """Bootstrap token name, if this linked list is whitelist-gated."""
    config: Optional[TokenizableConfig] = None
    """Config for the HEAD datum — only needed on the very first issuance (Init path).
    The backend will auto-fill ``uverify_validator_hash``; the caller provides the rest."""


@dataclass
class TokenizableCertificateResult:
    """Result returned by :meth:`~uverify_sdk.apps.UVerifyApps.issue_tokenizable_certificate`."""

    tx_hash: str
    """Cardano transaction hash of the issuance transaction."""
    key: str
    """The on-chain key (SHA-256 hash of the certified content)."""
    verify_url: str
    """Verification URL for this certificate."""


@dataclass
class TokenizableCertificateStatus:
    """On-chain status of a tokenizable certificate node."""

    key: str
    """The on-chain certificate key."""
    claimed: bool
    """Whether the certificate has been claimed (user token redeemed from the contract)."""
    owner: Optional[str] = None
    """Address of the current token holder, if claimed."""


@dataclass
class TokenizableCertificateClaimInput:
    """Input for claiming (redeeming) a tokenizable certificate."""

    key: str
    """The on-chain certificate key to claim."""
    claimer_address: str
    """Cardano address of the claimer (must hold the CIP-68 user token)."""
    init_utxo_tx_hash: str
    """Transaction hash of the Init UTxO."""
    init_utxo_output_index: int
    """Output index of the Init UTxO."""
    asset_name_hex: str
    """Hex-encoded asset name of the user token to redeem."""
