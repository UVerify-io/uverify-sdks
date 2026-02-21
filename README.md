# UVerify SDKs

Mono-repository containing official SDKs for the [UVerify](https://uverify.io) API — a certificate verification platform built on the Cardano blockchain.

## Packages

| SDK | Directory | Package |
|-----|-----------|---------|
| TypeScript / JavaScript | [`typescript/`](./typescript/) | `@uverify/sdk` |
| Python | [`python/`](./python/) | `uverify-sdk` |
| Java | [`java/`](./java/) | `io.uverify:uverify-sdk` |

## What is UVerify?

UVerify lets you issue and verify tamper-proof certificates on the Cardano blockchain. A certificate is a SHA-256 or SHA-512 hash of any document or data, stored on-chain alongside optional metadata. Anyone can independently verify that a hash was recorded at a specific point in time.

## Quick Start

All three SDKs expose the same high-level operations:

- **Verify** — check whether a data hash has a certificate on-chain
- **Build** — construct an unsigned transaction to issue certificates
- **Submit** — submit a signed transaction to the blockchain
- **User state** — manage your issuing workspace (invalidate state, opt-out, query info)
- **Connected Goods** — mint, claim and update connected-goods NFT batches
- **Statistics** — query fee and certificate stats

See the README inside each SDK directory for language-specific instructions.

## API

The SDKs wrap the public UVerify REST API at `https://api.uverify.io`. Full interactive documentation is available at [https://api.uverify.io/v1/api-docs](https://api.uverify.io/v1/api-docs).

## License

AGPL-3.0 — see [LICENSE](../uverify-docs/LICENSE).
