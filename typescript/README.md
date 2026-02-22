# @uverify/sdk

Official TypeScript / JavaScript SDK for the [UVerify](https://app.uverify.io) API.

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
const client = new UVerifyClient({ baseUrl: 'http://localhost:9090' });

// Register default signing callbacks so you don't pass them on every call
const client = new UVerifyClient({
  signMessage: async (message) => wallet.signData(address, message),
  signTx: (unsignedTx) => wallet.signTx(unsignedTx, true),
});
```

### Verify a certificate

```ts
// By data hash
const certs = await client.verify('sha256-or-sha512-hex-hash');

// By transaction hash + data hash
const cert = await client.verifyByTransaction('cardano-tx-hash', 'data-hash');
```

### Issue certificates

`issueCertificates` handles the full flow — build, sign, submit — in one call and
returns the Cardano transaction hash on success.

`metadata` can be a plain object; the SDK serialises it to JSON automatically.

The example below uses [mesh.js](https://meshjs.dev) with a headless wallet, which
is a good fit for scripts and backends. In a browser you would pass `api.signTx`
from a CIP-30 wallet instead.

```ts
import { UVerifyClient } from '@uverify/sdk';
import { MeshCardanoHeadlessWallet, AddressType } from '@meshsdk/wallet';
import { KoiosProvider } from '@meshsdk/core';
import { sha256 } from 'js-sha256';

const provider = new KoiosProvider('preprod');
const wallet = await MeshCardanoHeadlessWallet.fromMnemonic({
  networkId: 0,
  walletAddressType: AddressType.Base,
  fetcher: provider,
  submitter: provider,
  mnemonic: ['word1', 'word2', /* ... */],
});
const address = await wallet.getChangeAddressBech32();

const client = new UVerifyClient({
  signMessage: (message) => wallet.signData(address, message),
  signTx: (unsignedTx) => wallet.signTx(unsignedTx, true),
});

const myData = 'Hello, UVerify!';
const hash = sha256(myData);

const txHash = await client.issueCertificates(address, [
  {
    hash,
    algorithm: 'SHA-256',
    metadata: {
      issuer: 'Acme Corp',
      description: 'Proof of existence for myData',
      date: new Date().toISOString(),
    },
  },
]);
console.log('Certified at tx:', txHash);
```

Optionally pass a `stateId` as the last argument to issue under a specific state:

```ts
const txHash = await client.issueCertificates(
  address,
  [{ hash: 'sha256-hash' }],
  undefined,       // use constructor signTx
  'my-state-id',
);
```

### User state management

```ts
// Retrieve current state
const state = await client.getUserInfo(address);
console.log('Certificates remaining:', state?.countdown);

// Invalidate a state (destructive)
if (state?.id) {
  await client.invalidateState(address, state.id);
}

// Opt out entirely (destructive)
if (state?.id) {
  await client.optOut(address, state.id);
}
```

A per-call signing callback can be passed as the last argument to any of these
methods if you didn't register one in the constructor.

### Low-level access via `.core`

For advanced flows and custom submission logic use the `.core` submodule
to call each step individually:

```ts
// Build
const { unsignedTransaction } = await client.core.buildTransaction({
  type: 'default',
  address: 'addr1...',
  stateId: 'your-state-id',
  certificates: [{ hash: 'sha256-hash', algorithm: 'SHA-256' }],
});

// Sign with your wallet, then submit
const witnessSet = await wallet.signTx(unsignedTransaction, true);
const txHash = await client.core.submitTransaction(unsignedTransaction, witnessSet);
```

```ts
// Two-step user state action (manual)
const challenge = await client.core.requestUserAction({
  address: 'addr1...',
  action: 'USER_INFO',
});

const { key, signature } = await wallet.signData(address, challenge.message);

const result = await client.core.executeUserAction({
  ...challenge,
  userSignature: signature,
  userPublicKey: key,
});
console.log(result.state);
```

## Error handling

```ts
import { UVerifyApiError, UVerifyValidationError } from '@uverify/sdk';

try {
  const txHash = await client.issueCertificates(address, certs);
} catch (err) {
  if (err instanceof UVerifyApiError) {
    // HTTP error from the API
    console.error(`API error ${err.statusCode}:`, err.responseBody);
  }
  if (err instanceof UVerifyValidationError) {
    // Missing sign callback — pass one to the method or set it in the constructor
    console.error(err.message);
  }
}
```

## API Reference

### High-level helpers

| Method | Description |
|--------|-------------|
| `verify(hash)` | Look up all on-chain certificates for a data hash |
| `verifyByTransaction(txHash, dataHash)` | Fetch a specific certificate by tx hash + data hash |
| `issueCertificates(address, certificates, signTx?, stateId?)` | Build, sign, and submit; returns tx hash |
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
