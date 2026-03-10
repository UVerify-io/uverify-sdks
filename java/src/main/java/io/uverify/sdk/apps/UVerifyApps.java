package io.uverify.sdk.apps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.uverify.sdk.callback.TransactionSignCallback;
import io.uverify.sdk.exception.UVerifyException;
import io.uverify.sdk.model.CertificateData;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * High-level application helpers for issuing well-known certificate types.
 *
 * <p>Accessible via {@link io.uverify.sdk.UVerifyClient#apps}. Each method handles
 * hashing, metadata construction, and verification URL generation automatically.
 *
 * <pre>{@code
 * var result = client.apps.issueDiploma(address, List.of(
 *     new UVerifyApps.DiplomaInput("TUM-2021-0042", "Maria Müller",
 *         "Master of Science in Computer Science",
 *         "Technical University of Munich", "2024-06-28", "Summa Cum Laude")
 * ));
 * result.getCertificates().forEach(c -> System.out.println(c.getVerifyUrl()));
 * }</pre>
 */
public class UVerifyApps {

    /**
     * Functional interface for the bound {@code issueCertificates} method
     * passed in from {@link io.uverify.sdk.UVerifyClient}.
     */
    @FunctionalInterface
    public interface IssueFn {
        String issue(String address, List<CertificateData> certificates, TransactionSignCallback signTx);
    }

    // -------------------------------------------------------------------------
    // Input types
    // -------------------------------------------------------------------------

    /** Input for a single diploma certificate. */
    public static class DiplomaInput {
        private final String studentId;
        private final String name;
        private final String degree;
        private final String institution;
        private final String graduationDate;
        private final String honors;

        /**
         * @param studentId      Student or learner identifier — used as the on-chain fingerprint.
         * @param name           Recipient's full name (stored as SHA-256 hash on-chain).
         * @param degree         Degree or qualification title shown on the certificate.
         * @param institution    Name of the issuing institution.
         * @param graduationDate Graduation or completion date (e.g. {@code "2024-06-28"}).
         * @param honors         Optional honours distinction (e.g. {@code "Summa Cum Laude"});
         *                       pass {@code null} to omit.
         */
        public DiplomaInput(String studentId, String name, String degree,
                            String institution, String graduationDate, String honors) {
            this.studentId = studentId;
            this.name = name;
            this.degree = degree;
            this.institution = institution;
            this.graduationDate = graduationDate;
            this.honors = honors;
        }

        /** Convenience constructor without honors. */
        public DiplomaInput(String studentId, String name, String degree,
                            String institution, String graduationDate) {
            this(studentId, name, degree, institution, graduationDate, null);
        }

        public String getStudentId()      { return studentId; }
        public String getName()           { return name; }
        public String getDegree()         { return degree; }
        public String getInstitution()    { return institution; }
        public String getGraduationDate() { return graduationDate; }
        public String getHonors()         { return honors; }
    }

    /** A single certificate entry in a {@link DiplomaResult}. */
    public static class DiplomaCertificate {
        private final String studentId;
        private final String name;
        private final String hash;
        private final String verifyUrl;

        DiplomaCertificate(String studentId, String name, String hash, String verifyUrl) {
            this.studentId = studentId;
            this.name = name;
            this.hash = hash;
            this.verifyUrl = verifyUrl;
        }

        public String getStudentId() { return studentId; }
        public String getName()      { return name; }
        /** SHA-256 hash of {@code studentId} — the on-chain certificate fingerprint. */
        public String getHash()      { return hash; }
        /** Verification URL with {@code ?name=} pre-populated to reveal the recipient's name. */
        public String getVerifyUrl() { return verifyUrl; }
    }

    /** Result returned by {@link #issueDiploma}. */
    public static class DiplomaResult {
        private final String txHash;
        private final List<DiplomaCertificate> certificates;

        DiplomaResult(String txHash, List<DiplomaCertificate> certificates) {
            this.txHash = txHash;
            this.certificates = certificates;
        }

        /** Cardano transaction hash of the issuance transaction. */
        public String getTxHash()                          { return txHash; }
        public List<DiplomaCertificate> getCertificates()  { return certificates; }
    }

    /** Input for a Digital Product Passport certificate. */
    public static class DigitalProductPassportInput {
        private final String name;
        private final String manufacturer;
        private final String gtin;
        private final String serialNumber;
        private String model;
        private String origin;
        private String manufactured;
        private String contact;
        private String brandColor;
        private String carbonFootprint;
        private String recycledContent;
        private String energyClass;
        private String warranty;
        private String spareParts;
        private String repairInfo;
        private String recycling;
        private Map<String, String> materials;
        private Map<String, String> certifications;

        /**
         * @param name         Product name shown on the passport.
         * @param manufacturer Manufacturer or issuing organisation.
         * @param gtin         Global Trade Item Number (GTIN / barcode).
         * @param serialNumber Unit-level serial number (stored as SHA-256 hash on-chain).
         */
        public DigitalProductPassportInput(String name, String manufacturer,
                                           String gtin, String serialNumber) {
            this.name = name;
            this.manufacturer = manufacturer;
            this.gtin = gtin;
            this.serialNumber = serialNumber;
        }

        public String getName()         { return name; }
        public String getManufacturer() { return manufacturer; }
        public String getGtin()         { return gtin; }
        public String getSerialNumber() { return serialNumber; }
        public String getModel()        { return model; }
        public String getOrigin()       { return origin; }
        public String getManufactured() { return manufactured; }
        public String getContact()      { return contact; }
        public String getBrandColor()   { return brandColor; }
        public String getCarbonFootprint()  { return carbonFootprint; }
        public String getRecycledContent()  { return recycledContent; }
        public String getEnergyClass()      { return energyClass; }
        public String getWarranty()         { return warranty; }
        public String getSpareParts()       { return spareParts; }
        public String getRepairInfo()       { return repairInfo; }
        public String getRecycling()        { return recycling; }
        /** Material composition entries. Keys are plain names — {@code mat_} prefix added automatically. */
        public Map<String, String> getMaterials()       { return materials; }
        /** Certification entries. Keys are plain names — {@code cert_} prefix added automatically. */
        public Map<String, String> getCertifications()  { return certifications; }

        public DigitalProductPassportInput model(String v)         { this.model = v; return this; }
        public DigitalProductPassportInput origin(String v)        { this.origin = v; return this; }
        public DigitalProductPassportInput manufactured(String v)  { this.manufactured = v; return this; }
        public DigitalProductPassportInput contact(String v)       { this.contact = v; return this; }
        public DigitalProductPassportInput brandColor(String v)    { this.brandColor = v; return this; }
        public DigitalProductPassportInput carbonFootprint(String v) { this.carbonFootprint = v; return this; }
        public DigitalProductPassportInput recycledContent(String v) { this.recycledContent = v; return this; }
        public DigitalProductPassportInput energyClass(String v)   { this.energyClass = v; return this; }
        public DigitalProductPassportInput warranty(String v)      { this.warranty = v; return this; }
        public DigitalProductPassportInput spareParts(String v)    { this.spareParts = v; return this; }
        public DigitalProductPassportInput repairInfo(String v)    { this.repairInfo = v; return this; }
        public DigitalProductPassportInput recycling(String v)     { this.recycling = v; return this; }
        public DigitalProductPassportInput materials(Map<String, String> v)      { this.materials = v; return this; }
        public DigitalProductPassportInput certifications(Map<String, String> v) { this.certifications = v; return this; }
    }

    /** Result returned by {@link #issueDigitalProductPassport}. */
    public static class DigitalProductPassportResult {
        private final String txHash;
        private final String hash;
        private final String verifyUrl;

        DigitalProductPassportResult(String txHash, String hash, String verifyUrl) {
            this.txHash = txHash;
            this.hash = hash;
            this.verifyUrl = verifyUrl;
        }

        /** Cardano transaction hash of the issuance transaction. */
        public String getTxHash()    { return txHash; }
        /** SHA-256 hash of {@code gtin + serialNumber} — the on-chain certificate fingerprint. */
        public String getHash()      { return hash; }
        /** Verification URL with {@code ?serial=} pre-populated to reveal the serial number. */
        public String getVerifyUrl() { return verifyUrl; }
    }

    /** Input for a single laboratory report certificate. */
    public static class LaboratoryReportInput {
        private final String reportId;
        private final String patientName;
        private final String labName;
        private final Map<String, String> values;
        private String contact;
        private boolean auditable = false;

        /**
         * @param reportId    Unique report identifier — used as the on-chain fingerprint.
         * @param patientName Patient's full name (stored as SHA-256 hash on-chain).
         * @param labName     Name of the issuing laboratory.
         * @param values      Measured parameter values. Keys are plain names — {@code a_} prefix added automatically.
         */
        public LaboratoryReportInput(String reportId, String patientName,
                                     String labName, Map<String, String> values) {
            this.reportId = reportId;
            this.patientName = patientName;
            this.labName = labName;
            this.values = values;
        }

        public String getReportId()    { return reportId; }
        public String getPatientName() { return patientName; }
        public String getLabName()     { return labName; }
        public Map<String, String> getValues() { return values; }
        public String getContact()     { return contact; }
        public boolean isAuditable()   { return auditable; }

        public LaboratoryReportInput contact(String v)   { this.contact = v; return this; }
        public LaboratoryReportInput auditable(boolean v) { this.auditable = v; return this; }
    }

    /** A single certificate entry in a {@link LaboratoryReportResult}. */
    public static class LaboratoryReportCertificate {
        private final String reportId;
        private final String patientName;
        private final String hash;
        private final String verifyUrl;

        LaboratoryReportCertificate(String reportId, String patientName,
                                    String hash, String verifyUrl) {
            this.reportId = reportId;
            this.patientName = patientName;
            this.hash = hash;
            this.verifyUrl = verifyUrl;
        }

        public String getReportId()    { return reportId; }
        public String getPatientName() { return patientName; }
        /** SHA-256 hash of {@code reportId} — the on-chain certificate fingerprint. */
        public String getHash()        { return hash; }
        /** Verification URL with {@code ?name=} and {@code ?report_id=} pre-populated. */
        public String getVerifyUrl()   { return verifyUrl; }
    }

    /** Result returned by {@link #issueLaboratoryReport}. */
    public static class LaboratoryReportResult {
        private final String txHash;
        private final List<LaboratoryReportCertificate> certificates;

        LaboratoryReportResult(String txHash, List<LaboratoryReportCertificate> certificates) {
            this.txHash = txHash;
            this.certificates = certificates;
        }

        /** Cardano transaction hash of the issuance transaction. */
        public String getTxHash()                                   { return txHash; }
        public List<LaboratoryReportCertificate> getCertificates()  { return certificates; }
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    private final IssueFn issueFn;
    private final String verifyBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UVerifyApps(IssueFn issueFn, String verifyBaseUrl) {
        this.issueFn = issueFn;
        this.verifyBaseUrl = verifyBaseUrl;
    }

    /**
     * Issue one or more diploma certificates in a single transaction.
     *
     * <p>Each diploma's {@code studentId} is hashed with SHA-256 to produce the on-chain
     * fingerprint. The recipient's {@code name} is stored as a hash and revealed only
     * via the {@code ?name=} URL parameter in the returned verification link.
     *
     * <p>The update policy is {@code first} — the initial issuance is permanent and cannot
     * be overwritten.
     *
     * @param address   Cardano address of the signer / payer.
     * @param diplomas  One or more diploma inputs.
     * @param signTx    Wallet callback to sign the transaction; {@code null} uses the
     *                  constructor-level default on {@link io.uverify.sdk.UVerifyClient}.
     * @return {@link DiplomaResult} with the transaction hash and per-diploma verification URLs.
     */
    public DiplomaResult issueDiploma(String address, List<DiplomaInput> diplomas,
                                      TransactionSignCallback signTx) {
        List<String> hashes = new ArrayList<>();
        List<String> nameHashes = new ArrayList<>();
        for (DiplomaInput d : diplomas) {
            hashes.add(sha256hex(d.getStudentId()));
            nameHashes.add(sha256hex(d.getName()));
        }

        List<CertificateData> certs = new ArrayList<>();
        for (int i = 0; i < diplomas.size(); i++) {
            DiplomaInput d = diplomas.get(i);
            String description = "This is to certify that the above-named individual has successfully "
                    + "completed all requirements for the degree of " + d.getDegree()
                    + " at " + d.getInstitution()
                    + (d.getHonors() != null ? ", awarded with " + d.getHonors() : "")
                    + ", on " + d.getGraduationDate() + ".";

            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("uverify_template_id",   "diploma");
            metadata.put("uverify_update_policy",  "first");
            metadata.put("issuer",                 d.getInstitution());
            metadata.put("uv_url_name",            nameHashes.get(i));
            metadata.put("title",                  d.getDegree());
            metadata.put("description",            description);

            certs.add(new CertificateData(hashes.get(i), "SHA-256", toJson(metadata)));
        }

        String txHash = issueFn.issue(address, certs, signTx);

        List<DiplomaCertificate> results = new ArrayList<>();
        for (int i = 0; i < diplomas.size(); i++) {
            DiplomaInput d = diplomas.get(i);
            String url = verifyBaseUrl + "/" + hashes.get(i) + "/" + txHash
                    + "?name=" + urlEncode(d.getName());
            results.add(new DiplomaCertificate(d.getStudentId(), d.getName(), hashes.get(i), url));
        }
        return new DiplomaResult(txHash, results);
    }

    /** Issue diplomas using the constructor-level sign callback. */
    public DiplomaResult issueDiploma(String address, List<DiplomaInput> diplomas) {
        return issueDiploma(address, diplomas, null);
    }

    /**
     * Issue a Digital Product Passport for a single product unit.
     *
     * <p>The hash is computed as {@code sha256(gtin + serialNumber)}, uniquely identifying
     * this product instance. The serial number is stored as a hash on-chain and revealed
     * via {@code ?serial=} in the verification link.
     *
     * <p>{@code materials} keys receive a {@code mat_} prefix automatically;
     * {@code certifications} keys receive a {@code cert_} prefix automatically.
     *
     * <p>The update policy is {@code restricted} — only the issuer wallet may push
     * subsequent corrections.
     *
     * @param address  Cardano address of the signer / payer.
     * @param product  Product passport input.
     * @param signTx   Wallet callback to sign the transaction; {@code null} uses the
     *                 constructor-level default on {@link io.uverify.sdk.UVerifyClient}.
     * @return {@link DigitalProductPassportResult} with the transaction hash and verification URL.
     */
    public DigitalProductPassportResult issueDigitalProductPassport(
            String address, DigitalProductPassportInput product, TransactionSignCallback signTx) {
        String dataHash   = sha256hex(product.getGtin() + product.getSerialNumber());
        String serialHash = sha256hex(product.getSerialNumber());

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("uverify_template_id",   "digitalProductPassport");
        metadata.put("uverify_update_policy", "restricted");
        metadata.put("name",                  product.getName());
        metadata.put("issuer",                product.getManufacturer());
        metadata.put("gtin",                  product.getGtin());
        metadata.put("uv_url_serial",         serialHash);
        putIfNotNull(metadata, "model",           product.getModel());
        putIfNotNull(metadata, "origin",          product.getOrigin());
        putIfNotNull(metadata, "manufactured",    product.getManufactured());
        putIfNotNull(metadata, "contact",         product.getContact());
        putIfNotNull(metadata, "brand_color",     product.getBrandColor());
        putIfNotNull(metadata, "carbon_footprint",product.getCarbonFootprint());
        putIfNotNull(metadata, "recycled_content",product.getRecycledContent());
        putIfNotNull(metadata, "energy_class",    product.getEnergyClass());
        putIfNotNull(metadata, "warranty",        product.getWarranty());
        putIfNotNull(metadata, "spare_parts",     product.getSpareParts());
        putIfNotNull(metadata, "repair_info",     product.getRepairInfo());
        putIfNotNull(metadata, "recycling",       product.getRecycling());
        if (product.getMaterials() != null) {
            product.getMaterials().forEach((k, v) -> metadata.put("mat_" + k, v));
        }
        if (product.getCertifications() != null) {
            product.getCertifications().forEach((k, v) -> metadata.put("cert_" + k, v));
        }

        String txHash = issueFn.issue(
                address,
                List.of(new CertificateData(dataHash, "SHA-256", toJson(metadata))),
                signTx);

        String verifyUrl = verifyBaseUrl + "/" + dataHash + "/" + txHash
                + "?serial=" + urlEncode(product.getSerialNumber());
        return new DigitalProductPassportResult(txHash, dataHash, verifyUrl);
    }

    /** Issue a Digital Product Passport using the constructor-level sign callback. */
    public DigitalProductPassportResult issueDigitalProductPassport(
            String address, DigitalProductPassportInput product) {
        return issueDigitalProductPassport(address, product, null);
    }

    /**
     * Issue one or more laboratory report certificates in a single transaction.
     *
     * <p>Each report is identified by {@code sha256(reportId)}. Patient name and report
     * ID are stored as hashes on-chain and revealed via {@code ?name=} and
     * {@code ?report_id=} URL parameters in the returned verification links.
     *
     * <p>{@code values} keys receive an {@code a_} prefix automatically.
     *
     * <p>The update policy is {@code first} — the initial issuance is permanent and cannot
     * be overwritten.
     *
     * @param address  Cardano address of the signer / payer.
     * @param reports  One or more laboratory report inputs.
     * @param signTx   Wallet callback to sign the transaction; {@code null} uses the
     *                 constructor-level default on {@link io.uverify.sdk.UVerifyClient}.
     * @return {@link LaboratoryReportResult} with the transaction hash and per-report
     *         verification URLs.
     */
    public LaboratoryReportResult issueLaboratoryReport(
            String address, List<LaboratoryReportInput> reports, TransactionSignCallback signTx) {
        List<String> hashes = new ArrayList<>();
        List<String> patientHashes = new ArrayList<>();
        for (LaboratoryReportInput r : reports) {
            hashes.add(sha256hex(r.getReportId()));
            patientHashes.add(sha256hex(r.getPatientName()));
        }

        List<CertificateData> certs = new ArrayList<>();
        for (int i = 0; i < reports.size(); i++) {
            LaboratoryReportInput r = reports.get(i);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("uverify_template_id",   "laboratoryReport");
            metadata.put("uverify_update_policy",  "first");
            metadata.put("issuer",                 r.getLabName());
            metadata.put("uv_url_name",            patientHashes.get(i));
            metadata.put("uv_url_report_id",       hashes.get(i));
            metadata.put("auditable",              String.valueOf(r.isAuditable()));
            putIfNotNull(metadata, "contact", r.getContact());
            r.getValues().forEach((k, v) -> metadata.put("a_" + k, v));
            certs.add(new CertificateData(hashes.get(i), "SHA-256", toJson(metadata)));
        }

        String txHash = issueFn.issue(address, certs, signTx);

        List<LaboratoryReportCertificate> results = new ArrayList<>();
        for (int i = 0; i < reports.size(); i++) {
            LaboratoryReportInput r = reports.get(i);
            String url = verifyBaseUrl + "/" + hashes.get(i) + "/" + txHash
                    + "?name=" + urlEncode(r.getPatientName())
                    + "&report_id=" + urlEncode(r.getReportId());
            results.add(new LaboratoryReportCertificate(
                    r.getReportId(), r.getPatientName(), hashes.get(i), url));
        }
        return new LaboratoryReportResult(txHash, results);
    }

    /** Issue laboratory reports using the constructor-level sign callback. */
    public LaboratoryReportResult issueLaboratoryReport(
            String address, List<LaboratoryReportInput> reports) {
        return issueLaboratoryReport(address, reports, null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String sha256hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new UVerifyException("SHA-256 not available", e);
        }
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new UVerifyException("Failed to serialize metadata", e);
        }
    }

    private static void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) map.put(key, value);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
