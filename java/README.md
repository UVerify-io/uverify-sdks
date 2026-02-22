# uverify-sdk (Java)

Official Java SDK for the [UVerify](https://uverify.io) API.

## Requirements

- Java 11+
- Maven 3.8+ (for building from source)

## Installation

### Maven

```xml
<dependency>
    <groupId>io.uverify</groupId>
    <artifactId>uverify-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.uverify:uverify-sdk:0.1.0'
```

## Quick Start

```java
import io.uverify.sdk.UVerifyClient;
import io.uverify.sdk.model.CertificateResponse;

UVerifyClient client = new UVerifyClient();

List<CertificateResponse> certs = client.verify("a3b4c5d6...");
certs.forEach(c -> System.out.println(c.getTransactionHash()));
```

## Usage

### Creating the client

```java
// Connect to the public API (default)
UVerifyClient client = new UVerifyClient();

// Connect to a self-hosted instance with a custom timeout
UVerifyClient client = UVerifyClient.builder()
    .baseUrl("https://my-instance.example.com")
    .timeout(Duration.ofSeconds(60))
    .build();

// Add a custom header to every request
UVerifyClient client = UVerifyClient.builder()
    .header("X-Api-Key", "your-key")
    .build();

// Register default signing callbacks once so you don't pass them on every call
UVerifyClient client = UVerifyClient.builder()
    .signMessage(message -> wallet.signData(address, message))
    .signTx(unsignedTx -> wallet.signTx(unsignedTx))
    .build();
```

### Verify a certificate

```java
// By data hash
List<CertificateResponse> certs = client.verify("sha256-or-sha512-hex-hash");

// By transaction hash + data hash
CertificateResponse cert = client.verifyByTransaction("cardano-tx-hash", "data-hash");
```

### Issue certificates

`issueCertificates` handles the full flow — build, sign, submit — in one call.

```java
import io.uverify.sdk.model.CertificateData;
import java.util.List;

client.issueCertificates(
    "addr1...",
    List.of(new CertificateData("sha256-hash-of-document", "SHA-256")),
    unsignedTx -> wallet.signTx(unsignedTx)  // omit if set in constructor
);
```

Optionally pass a `stateId` as the second argument:

```java
client.issueCertificates(
    "addr1...",
    "my-state-id",
    List.of(new CertificateData("sha256-hash"))
    // uses constructor signTx
);
```

### User state management

```java
import io.uverify.sdk.model.ExecuteUserActionResponse;

// Retrieve current state
ExecuteUserActionResponse response = client.getUserInfo("addr1...");
System.out.println("Certificates remaining: " + response.getState().getCountdown());

// Invalidate a state
client.invalidateState("addr1...", "state-id");

// Opt out entirely
client.optOut("addr1...", "state-id");
```

A per-call signing callback can be passed as the last argument to any of these
methods if you didn't register one in the constructor.

### Low-level access via `.core`

For advanced flows (multi-sig, custom submission logic) use the `core` field
to call each step individually:

```java
import io.uverify.sdk.model.BuildTransactionRequest;
import io.uverify.sdk.model.BuildTransactionResponse;

// Build
BuildTransactionResponse response = client.core.buildTransaction(
    BuildTransactionRequest.defaultRequest(
        "addr1...", "your-state-id",
        new CertificateData("sha256-hash", "SHA-256")
    )
);

// Sign with your wallet, then submit
String witnessSet = wallet.signTx(response.getUnsignedTransaction());
client.core.submitTransaction(response.getUnsignedTransaction(), witnessSet);
```

```java
import io.uverify.sdk.model.UserActionRequest;
import io.uverify.sdk.model.UserActionRequest.UserAction;
import io.uverify.sdk.model.UserActionRequestResponse;
import io.uverify.sdk.model.ExecuteUserActionRequest;

// Two-step user state action (manual)
UserActionRequestResponse challenge = client.core.requestUserAction(
    new UserActionRequest("addr1...", UserAction.USER_INFO)
);

DataSignature sig = wallet.signData(address, challenge.getMessage());

ExecuteUserActionResponse result = client.core.executeUserAction(
    new ExecuteUserActionRequest(challenge, sig.getSignature(), sig.getKey())
);
System.out.println(result.getState());
```

## Error Handling

```java
import io.uverify.sdk.exception.UVerifyApiException;
import io.uverify.sdk.exception.UVerifyValidationException;

try {
    client.issueCertificates("addr1...", certs);
} catch (UVerifyApiException e) {
    // HTTP error from the API (status code, response body available)
    System.err.println("API error " + e.getStatusCode() + ": " + e.getMessage());
} catch (UVerifyValidationException e) {
    // Missing sign callback — pass one to the method or set it in the builder
    System.err.println(e.getMessage());
}
```

## Building from Source

```bash
cd java/
mvn clean package
```

## API Reference

### High-level helpers

| Method | Description |
|--------|-------------|
| `verify(hash)` | Look up all on-chain certificates for a data hash |
| `verifyByTransaction(txHash, dataHash)` | Fetch a specific certificate by tx hash + data hash |
| `issueCertificates(address, certificates, signTx?)` | Build, sign, and submit a certificate transaction |
| `issueCertificates(address, stateId, certificates, signTx?)` | Same, scoped to a state |
| `getUserInfo(address, signMessage?)` | Retrieve the current user state |
| `invalidateState(address, stateId, signMessage?)` | Mark a state as invalid |
| `optOut(address, stateId, signMessage?)` | Remove the user's state entirely |

### Low-level core (`.core`)

| Method | Endpoint |
|--------|----------|
| `core.buildTransaction(request)` | `POST /api/v1/transaction/build` |
| `core.submitTransaction(tx, witnessSet?)` | `POST /api/v1/transaction/submit` |
| `core.requestUserAction(request)` | `POST /api/v1/user/request/action` |
| `core.executeUserAction(request)` | `POST /api/v1/user/state/action` |
