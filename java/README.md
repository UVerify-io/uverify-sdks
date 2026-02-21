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
```

### Verify a certificate

```java
// By data hash
List<CertificateResponse> certs = client.verify("sha256-or-sha512-hex-hash");

// By transaction hash + data hash
CertificateResponse cert = client.verifyByTransaction("cardano-tx-hash", "data-hash");
```

### Issue certificates

Issuing is a two-step process: build an unsigned transaction, sign it
with your wallet, then submit it.

```java
import io.uverify.sdk.model.BuildTransactionRequest;
import io.uverify.sdk.model.BuildTransactionResponse;
import io.uverify.sdk.model.CertificateData;

BuildTransactionResponse response = client.buildTransaction(
    BuildTransactionRequest.defaultRequest(
        "addr1...",
        "your-state-id",
        new CertificateData("sha256-hash-of-document", "SHA-256")
    )
);

String unsignedTx = response.getUnsignedTransaction();
// Sign unsignedTx with your wallet, then:
client.submitTransaction(signedTxCborHex);
```

### User state management

```java
import io.uverify.sdk.model.UserActionRequest;
import io.uverify.sdk.model.UserActionRequest.UserAction;
import io.uverify.sdk.model.UserActionRequestResponse;
import io.uverify.sdk.model.ExecuteUserActionRequest;

// Step 1: get a server-signed challenge
UserActionRequestResponse req = client.requestUserAction(
    new UserActionRequest("addr1...", UserAction.USER_INFO)
);

// Step 2: sign req.getMessage() with your wallet, then execute
Map<String, Object> result = client.executeUserAction(
    new ExecuteUserActionRequest(req, walletSignatureHex, walletPublicKeyHex)
);
```

### Connected Goods

```java
import io.uverify.sdk.model.MintConnectedGoodsRequest;
import io.uverify.sdk.model.MintConnectedGoodsRequest.Item;
import io.uverify.sdk.model.MintConnectedGoodsResponse;
import io.uverify.sdk.model.ClaimUpdateConnectedGoodsRequest;

// Mint a batch
MintConnectedGoodsResponse resp = client.mintConnectedGoodsBatch(
    new MintConnectedGoodsRequest(
        "addr1...", "MY_ITEM",
        new Item("secret1", "ITEM001"),
        new Item("secret2", "ITEM002")
    )
);
String batchId = resp.getBatchId();

// Claim an item
client.claimConnectedGoodsItem(
    new ClaimUpdateConnectedGoodsRequest("secret1", batchId, "addr1...")
        .withSocialHubEntry("name", "Alice")
        .withSocialHubEntry("twitter", "@alice")
);
```

### Statistics

```java
Object fees = client.getTransactionFees();
Object categories = client.getCertificatesByCategory();
```

## Error Handling

```java
import io.uverify.sdk.exception.UVerifyException;

try {
    List<CertificateResponse> certs = client.verify("bad-hash");
} catch (UVerifyException e) {
    System.err.println("API error " + e.getStatusCode() + ": " + e.getMessage());
}
```

## Building from Source

```bash
cd java/
mvn clean package
```

## API Reference

All methods map to the [UVerify REST API](https://api.uverify.io/v1/api-docs).

| Method | Endpoint |
|--------|----------|
| `verify(hash)` | `GET /api/v1/verify/{hash}` |
| `verifyByTransaction(txHash, dataHash)` | `GET /api/v1/verify/by-transaction-hash/{txHash}/{dataHash}` |
| `buildTransaction(request)` | `POST /api/v1/transaction/build` |
| `submitTransaction(tx)` | `POST /api/v1/transaction/submit` |
| `submitTransaction(tx, witnessSet)` | `POST /api/v1/transaction/submit` |
| `requestUserAction(request)` | `POST /api/v1/user/request/action` |
| `executeUserAction(request)` | `POST /api/v1/user/state/action` |
| `mintConnectedGoodsBatch(request)` | `POST /api/v1/extension/connected-goods/mint/batch` |
| `claimConnectedGoodsItem(request)` | `POST /api/v1/extension/connected-goods/claim/item` |
| `updateConnectedGoodsItem(request)` | `POST /api/v1/extension/connected-goods/update/item` |
| `getConnectedGoodsItem(batchIds, itemId)` | `GET /api/v1/extension/connected-goods/{batchIds}/{itemId}` |
| `getTransactionScripts(txHash)` | `GET /api/v1/txs/{txHash}/scripts` |
| `getTransactionRedeemers(txHash)` | `GET /api/v1/txs/{txHash}/redeemers` |
| `getScript(scriptHash)` | `GET /api/v1/scripts/{scriptHash}` |
| `getScriptJson(scriptHash)` | `GET /api/v1/scripts/{scriptHash}/json` |
| `getScriptDetails(scriptHash)` | `GET /api/v1/scripts/{scriptHash}/details` |
| `getScriptCbor(scriptHash)` | `GET /api/v1/scripts/{scriptHash}/cbor` |
| `getDatum(datumHash)` | `GET /api/v1/scripts/datum/{datumHash}` |
| `getDatumCbor(datumHash)` | `GET /api/v1/scripts/datum/{datumHash}/cbor` |
| `getTransactionFees()` | `GET /api/v1/statistic/tx-fees` |
| `getCertificatesByCategory()` | `GET /api/v1/statistic/certificate/by-category` |
