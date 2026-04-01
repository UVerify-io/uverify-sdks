/**
 * Input for a single diploma certificate.
 */
export interface DiplomaInput {
  /** Student or learner identifier — used as the on-chain fingerprint. */
  studentId: string;
  /** Recipient's full name (stored as SHA-256 hash on-chain, revealed via URL param). */
  name: string;
  /** Degree or qualification title shown on the certificate. */
  degree: string;
  /** Name of the issuing institution. */
  institution: string;
  /** Graduation or completion date (e.g. `'2024-06-28'`). */
  graduationDate: string;
  /** Optional honours distinction (e.g. `'Summa Cum Laude'`). */
  honors?: string;
}

/**
 * Result returned by {@link UVerifyApps.issueDiploma}.
 */
export interface DiplomaResult {
  /** Cardano transaction hash of the issuance transaction. */
  txHash: string;
  certificates: Array<{
    studentId: string;
    name: string;
    /** SHA-256 hash of `studentId` — the on-chain certificate fingerprint. */
    hash: string;
    /** Verification URL with `?name=` pre-populated to reveal the recipient's name. */
    verifyUrl: string;
  }>;
}

/**
 * Input for a Digital Product Passport certificate.
 */
export interface DigitalProductPassportInput {
  /** Product name shown on the passport. */
  name: string;
  /** Manufacturer or issuing organisation. */
  manufacturer: string;
  /** Global Trade Item Number (GTIN / barcode). */
  gtin: string;
  /** Unit-level serial number (stored as SHA-256 hash on-chain, revealed via URL param). */
  serialNumber: string;
  /** Model number or product line identifier. */
  model?: string;
  /** Country or place of manufacture. */
  origin?: string;
  /** Date of manufacture (e.g. `'2024-08-15'`). */
  manufactured?: string;
  /** Contact e-mail or URL for sustainability enquiries. */
  contact?: string;
  /** Brand accent colour as a hex string (e.g. `'#1a56db'`). */
  brandColor?: string;
  /** Carbon footprint (e.g. `'1.2 kg CO₂e'`). */
  carbonFootprint?: string;
  /** Percentage of recycled content (e.g. `'38%'`). */
  recycledContent?: string;
  /** EU energy class (e.g. `'A++'`). */
  energyClass?: string;
  /** Warranty period (e.g. `'3 years'`). */
  warranty?: string;
  /** Spare parts availability statement. */
  spareParts?: string;
  /** URL to repair instructions. */
  repairInfo?: string;
  /** End-of-life / recycling instructions. */
  recycling?: string;
  /**
   * Material composition entries.
   * Keys are plain material names — the `mat_` prefix is added automatically.
   * @example `{ aluminum: '45%', recycled_plastic: '38%' }`
   */
  materials?: Record<string, string>;
  /**
   * Certification entries.
   * Keys are plain identifiers — the `cert_` prefix is added automatically.
   * @example `{ ce: 'CE Marking', rohs: 'RoHS Compliant' }`
   */
  certifications?: Record<string, string>;
}

/**
 * Result returned by {@link UVerifyApps.issueDigitalProductPassport}.
 */
export interface DigitalProductPassportResult {
  /** Cardano transaction hash of the issuance transaction. */
  txHash: string;
  /** SHA-256 hash of `gtin + serialNumber` — the on-chain certificate fingerprint. */
  hash: string;
  /** Verification URL with `?serial=` pre-populated to reveal the serial number. */
  verifyUrl: string;
}

/**
 * Input for a single laboratory report certificate.
 */
export interface LaboratoryReportInput {
  /** Unique report identifier — used as the on-chain fingerprint. */
  reportId: string;
  /** Patient's full name (stored as SHA-256 hash on-chain, revealed via URL param). */
  patientName: string;
  /** Name of the issuing laboratory. */
  labName: string;
  /** Contact e-mail or institution for the full report. */
  contact?: string;
  /**
   * Whether the measured values are publicly auditable.
   * When `true`, the certificate page shows a transparency banner.
   * Defaults to `false`.
   */
  auditable?: boolean;
  /**
   * Measured parameter values.
   * Keys are plain parameter names — the `a_` prefix is added automatically.
   * @example `{ glucose: '5.4 mmol/L', hba1c: '5.7%' }`
   */
  values: Record<string, string>;
}

/**
 * Result returned by {@link UVerifyApps.issueLaboratoryReport}.
 */
export interface LaboratoryReportResult {
  /** Cardano transaction hash of the issuance transaction. */
  txHash: string;
  certificates: Array<{
    reportId: string;
    patientName: string;
    /** SHA-256 hash of `reportId` — the on-chain certificate fingerprint. */
    hash: string;
    /** Verification URL with `?name=` and `?report_id=` pre-populated. */
    verifyUrl: string;
  }>;
}

/**
 * Input for a Certificate of Insurance.
 */
export interface CertificateOfInsuranceInput {
  /** Unique policy reference number — used as the on-chain fingerprint. */
  policyNumber: string;
  /** Name of the issuing insurance company. */
  insurer: string;
  /** Name of the insurance broker or agent (optional). */
  producer?: string;
  /** Name of the insured business or individual. */
  insured: string;
  /** Address of the insured (optional). */
  insuredAddress?: string;
  /** Policy start date in ISO format (e.g. `'2025-01-01'`). */
  effectiveDate: string;
  /** Policy end date in ISO format (e.g. `'2026-01-01'`) — drives the VALID/EXPIRED badge on the certificate. */
  expirationDate: string;
  /** Name of the party requiring proof of insurance (optional). */
  certificateHolder?: string;
  /** Address of the certificate holder (optional). */
  certificateHolderAddress?: string;
  /** Whether the certificate holder is named as an additional insured (optional). */
  additionalInsured?: boolean;
  /** Whether a waiver of subrogation applies in favour of the certificate holder (optional). */
  waiverOfSubrogation?: boolean;
  /**
   * Coverage limits keyed by coverage type.
   * Keys are plain names — the `cov_` prefix is added automatically.
   * @example `{ general_liability: '1,000,000', workers_compensation: '500,000' }`
   */
  coverages: Record<string, string>;
}

/**
 * Result returned by {@link UVerifyApps.issueCertificateOfInsurance}.
 */
export interface CertificateOfInsuranceResult {
  /** Cardano transaction hash of the issuance transaction. */
  txHash: string;
  /** SHA-256 hash of `policyNumber` — the on-chain certificate fingerprint. */
  hash: string;
  /** Verification URL for this certificate. */
  verifyUrl: string;
}

/**
 * Input for issuing a tokenizable certificate (NFT-backed on-chain linked list node).
 *
 * Requires the `tokenizable-certificate` backend extension to be enabled.
 */
export interface TokenizableCertificateInput {
  /** SHA-256 hash of the content being certified — used as the on-chain key. */
  key: string;
  /** Public key hash of the token owner (the recipient of the CIP-68 user NFT). */
  ownerPubKeyHash: string;
  /** Hex-encoded asset name for the minted CIP-68 label-222 user token. */
  assetNameHex: string;
  /** Transaction hash of the Init UTxO that bootstrapped the on-chain linked list. */
  initUtxoTxHash: string;
  /** Output index of the Init UTxO. */
  initUtxoOutputIndex: number;
  /** Bootstrap token name, if this linked list is whitelist-gated. */
  bootstrapTokenName?: string;
}

/**
 * Result returned by {@link UVerifyApps.issueTokenizableCertificate}.
 */
export interface TokenizableCertificateResult {
  /** Cardano transaction hash of the issuance transaction. */
  txHash: string;
  /** The on-chain key (SHA-256 hash of the certified content). */
  key: string;
  /** Verification URL for this certificate. */
  verifyUrl: string;
}

/**
 * On-chain status of a tokenizable certificate node.
 */
export interface TokenizableCertificateStatus {
  /** The on-chain certificate key. */
  key: string;
  /** Whether the certificate has been claimed (user token redeemed from the contract). */
  claimed: boolean;
  /** Address of the current token holder, if claimed. */
  owner?: string;
}

/**
 * Input for claiming (redeeming) a tokenizable certificate.
 */
export interface TokenizableCertificateClaimInput {
  /** The on-chain certificate key to claim. */
  key: string;
  /** Cardano address of the claimer (must hold the CIP-68 user token). */
  claimerAddress: string;
  /** Transaction hash of the Init UTxO. */
  initUtxoTxHash: string;
  /** Output index of the Init UTxO. */
  initUtxoOutputIndex: number;
  /** Hex-encoded asset name of the user token to redeem. */
  assetNameHex: string;
}
