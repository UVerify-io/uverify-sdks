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
class TokenizableCertificateInput:
    """Input for issuing a tokenizable certificate (NFT-backed on-chain linked list node).

    Requires the ``tokenizable-certificate`` backend extension to be enabled.
    """

    key: str
    """SHA-256 hash of the content being certified — used as the on-chain key."""
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
