import type { TransactionSignCallback } from '../UVerifyClient.js';
import type { CertificateData } from '../types/index.js';
import type {
  DiplomaInput,
  DiplomaResult,
  DigitalProductPassportInput,
  DigitalProductPassportResult,
  LaboratoryReportInput,
  LaboratoryReportResult,
} from './types.js';

async function sha256hex(input: string): Promise<string> {
  const data = new TextEncoder().encode(input);
  const buffer = await globalThis.crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(buffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

type IssueFn = (
  address: string,
  certificates: CertificateData[],
  signCallback?: TransactionSignCallback,
) => Promise<string>;

/**
 * High-level application helpers for issuing well-known certificate types.
 *
 * Accessible via {@link UVerifyClient.apps}. Each method handles hashing,
 * metadata construction, and verification URL generation automatically.
 *
 * @example
 * ```ts
 * const { txHash, certificates } = await client.apps.issueDiploma(address, [
 *   {
 *     studentId: 'TUM-2021-0042',
 *     name: 'Maria Müller',
 *     degree: 'Master of Science in Computer Science',
 *     institution: 'Technical University of Munich',
 *     graduationDate: '2024-06-28',
 *     honors: 'Summa Cum Laude',
 *   },
 * ]);
 * ```
 */
export class UVerifyApps {
  private readonly issue: IssueFn;
  private readonly verifyBaseUrl: string;

  constructor(issueFn: IssueFn, verifyBaseUrl: string) {
    this.issue = issueFn;
    this.verifyBaseUrl = verifyBaseUrl;
  }

  /**
   * Issue one or more diploma certificates in a single transaction.
   *
   * Each diploma's `studentId` is hashed with SHA-256 to produce the on-chain
   * fingerprint. The recipient's `name` is stored as a hash and revealed only
   * via the `?name=` URL parameter in the returned verification link.
   *
   * The update policy is set to `first` — the initial issuance is permanent
   * and cannot be overwritten.
   */
  async issueDiploma(
    address: string,
    diplomas: DiplomaInput[],
    signCallback?: TransactionSignCallback,
  ): Promise<DiplomaResult> {
    const [hashes, nameHashes] = await Promise.all([
      Promise.all(diplomas.map((d) => sha256hex(d.studentId))),
      Promise.all(diplomas.map((d) => sha256hex(d.name))),
    ]);

    const certs: CertificateData[] = diplomas.map((d, i) => ({
      hash: hashes[i],
      algorithm: 'SHA-256',
      metadata: {
        uverify_template_id: 'diploma',
        uverify_update_policy: 'first',
        issuer: d.institution,
        uv_url_name: nameHashes[i],
        title: d.degree,
        description:
          `This is to certify that the above-named individual has successfully ` +
          `completed all requirements for the degree of ${d.degree} at ${d.institution}` +
          `${d.honors ? `, awarded with ${d.honors}` : ''}, on ${d.graduationDate}.`,
      },
    }));

    const txHash = await this.issue(address, certs, signCallback);

    return {
      txHash,
      certificates: diplomas.map((d, i) => {
        const params = new URLSearchParams({ name: d.name });
        return {
          studentId: d.studentId,
          name: d.name,
          hash: hashes[i],
          verifyUrl: `${this.verifyBaseUrl}/${hashes[i]}/${txHash}?${params}`,
        };
      }),
    };
  }

  /**
   * Issue a Digital Product Passport for a single product unit.
   *
   * The hash is computed as `sha256(gtin + serialNumber)`, uniquely identifying
   * this product instance. The serial number is stored as a hash on-chain and
   * revealed via `?serial=` in the verification link.
   *
   * `materials` keys receive a `mat_` prefix automatically;
   * `certifications` keys receive a `cert_` prefix automatically.
   *
   * The update policy is set to `restricted` — only the issuer wallet may push
   * subsequent corrections.
   */
  async issueDigitalProductPassport(
    address: string,
    product: DigitalProductPassportInput,
    signCallback?: TransactionSignCallback,
  ): Promise<DigitalProductPassportResult> {
    const [hash, serialHash] = await Promise.all([
      sha256hex(product.gtin + product.serialNumber),
      sha256hex(product.serialNumber),
    ]);

    const metadata: Record<string, string> = {
      uverify_template_id: 'digitalProductPassport',
      uverify_update_policy: 'restricted',
      name: product.name,
      issuer: product.manufacturer,
      gtin: product.gtin,
      uv_url_serial: serialHash,
      ...(product.model !== undefined ? { model: product.model } : {}),
      ...(product.origin !== undefined ? { origin: product.origin } : {}),
      ...(product.manufactured !== undefined ? { manufactured: product.manufactured } : {}),
      ...(product.contact !== undefined ? { contact: product.contact } : {}),
      ...(product.brandColor !== undefined ? { brand_color: product.brandColor } : {}),
      ...(product.carbonFootprint !== undefined ? { carbon_footprint: product.carbonFootprint } : {}),
      ...(product.recycledContent !== undefined ? { recycled_content: product.recycledContent } : {}),
      ...(product.energyClass !== undefined ? { energy_class: product.energyClass } : {}),
      ...(product.warranty !== undefined ? { warranty: product.warranty } : {}),
      ...(product.spareParts !== undefined ? { spare_parts: product.spareParts } : {}),
      ...(product.repairInfo !== undefined ? { repair_info: product.repairInfo } : {}),
      ...(product.recycling !== undefined ? { recycling: product.recycling } : {}),
    };

    for (const [key, value] of Object.entries(product.materials ?? {})) {
      metadata[`mat_${key}`] = value;
    }
    for (const [key, value] of Object.entries(product.certifications ?? {})) {
      metadata[`cert_${key}`] = value;
    }

    const txHash = await this.issue(
      address,
      [{ hash, algorithm: 'SHA-256', metadata }],
      signCallback,
    );

    const params = new URLSearchParams({ serial: product.serialNumber });
    return {
      txHash,
      hash,
      verifyUrl: `${this.verifyBaseUrl}/${hash}/${txHash}?${params}`,
    };
  }

  /**
   * Issue one or more laboratory report certificates in a single transaction.
   *
   * Each report is identified by `sha256(reportId)`. Patient name and report ID
   * are stored as hashes on-chain and revealed via `?name=` and `?report_id=`
   * URL parameters in the returned verification links.
   *
   * `values` keys receive an `a_` prefix automatically.
   *
   * The update policy is set to `first` — the initial issuance is permanent
   * and cannot be overwritten.
   */
  async issueLaboratoryReport(
    address: string,
    reports: LaboratoryReportInput[],
    signCallback?: TransactionSignCallback,
  ): Promise<LaboratoryReportResult> {
    const [hashes, patientHashes] = await Promise.all([
      Promise.all(reports.map((r) => sha256hex(r.reportId))),
      Promise.all(reports.map((r) => sha256hex(r.patientName))),
    ]);

    const certs: CertificateData[] = reports.map((r, i) => {
      const values: Record<string, string> = {};
      for (const [key, value] of Object.entries(r.values)) {
        values[`a_${key}`] = value;
      }
      return {
        hash: hashes[i],
        algorithm: 'SHA-256',
        metadata: {
          uverify_template_id: 'laboratoryReport',
          uverify_update_policy: 'first',
          issuer: r.labName,
          uv_url_name: patientHashes[i],
          uv_url_report_id: hashes[i],
          auditable: String(r.auditable ?? false),
          ...(r.contact !== undefined ? { contact: r.contact } : {}),
          ...values,
        },
      };
    });

    const txHash = await this.issue(address, certs, signCallback);

    return {
      txHash,
      certificates: reports.map((r, i) => {
        const params = new URLSearchParams({ name: r.patientName, report_id: r.reportId });
        return {
          reportId: r.reportId,
          patientName: r.patientName,
          hash: hashes[i],
          verifyUrl: `${this.verifyBaseUrl}/${hashes[i]}/${txHash}?${params}`,
        };
      }),
    };
  }
}

export type {
  DiplomaInput,
  DiplomaResult,
  DigitalProductPassportInput,
  DigitalProductPassportResult,
  LaboratoryReportInput,
  LaboratoryReportResult,
} from './types.js';
