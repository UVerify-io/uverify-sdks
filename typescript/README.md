# @uverify/sdk

Official TypeScript / JavaScript SDK for the [UVerify](https://uverify.io) API.

## Requirements

- Node.js 18+ (uses native `fetch`)
- Works in modern browsers without any polyfills

## Installation

```bash
npm install @uverify/sdk
# or
yarn add @uverify/sdk
# or
pnpm add @uverify/sdk
```

## Quick Start

```ts
import { UVerifyClient } from '@uverify/sdk';

const client = new UVerifyClient();

// Verify a document hash
const certificates = await client.verify('a3b4c5d6...');
console.log(certificates);
```

## Usage

### Creating the client

```ts
import { UVerifyClient } from '@uverify/sdk';

// Connect to the public API (default)
const client = new UVerifyClient();

// Connect to a self-hosted instance
const client = new UVerifyClient({ baseUrl: 'https://my-instance.example.com' });

// Add custom headers (e.g. an API key)
const client = new UVerifyClient({ headers: { 'X-Api-Key': 'your-key' } });
```

### Verify a certificate

```ts
// By data hash
const certs = await client.verify('sha256-or-sha512-hex-hash');

// By transaction hash + data hash
const cert = await client.verifyByTransaction('cardano-tx-hash', 'data-hash');
```

### Issue certificates

Issuing a certificate is a two-step process: build an unsigned transaction,
sign it with your wallet, then submit it.

```ts
// Step 1: build
const { unsignedTransaction } = await client.buildTransaction({
  type: 'default',
  address: 'addr1...',
  stateId: 'your-state-id',
  certificates: [
    { hash: 'sha256-hash-of-document', algorithm: 'SHA-256' },
  ],
});

// Step 2: sign with your CIP-30 wallet (browser) or cardano-cli, then submit
await client.submitTransaction(signedTransactionCborHex);
```

### User state management

```ts
// Step 1: request a signed challenge from the server
const request = await client.requestUserAction({
  address: 'addr1...',
  action: 'USER_INFO',
});

// Step 2: sign request.message with your wallet, then execute
const result = await client.executeUserAction({
  ...request,
  userSignature: walletSignatureHex,
  userPublicKey: walletPublicKeyHex,
});
console.log(result.state);
```

### Connected Goods

```ts
// Mint a batch
const { batch_id, unsigned_transaction } = await client.mintConnectedGoodsBatch({
  address: 'addr1...',
  token_name: 'MY_ITEM',
  items: [
    { password: 'secret1', asset_name: 'ITEM001' },
    { password: 'secret2', asset_name: 'ITEM002' },
  ],
});

// Claim an item
await client.claimConnectedGoodsItem({
  batch_id,
  password: 'secret1',
  user_address: 'addr1...',
  social_hub: { name: 'Alice', twitter: '@alice' },
});
```

### Statistics

```ts
const fees = await client.getTransactionFees();
const categories = await client.getCertificatesByCategory();
```

## Error handling

```ts
import { UVerifyApiError } from '@uverify/sdk';

try {
  const certs = await client.verify('bad-hash');
} catch (err) {
  if (err instanceof UVerifyApiError) {
    console.error(`API error ${err.statusCode}:`, err.message);
  }
}
```

## API Reference

All methods map directly to the [UVerify REST API](https://api.uverify.io/v1/api-docs).
Full type definitions are exported from the package.

| Method | Endpoint |
|--------|----------|
| `verify(hash)` | `GET /api/v1/verify/{hash}` |
| `verifyByTransaction(txHash, dataHash)` | `GET /api/v1/verify/by-transaction-hash/{txHash}/{dataHash}` |
| `buildTransaction(request)` | `POST /api/v1/transaction/build` |
| `submitTransaction(tx, witnessSet?)` | `POST /api/v1/transaction/submit` |
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
