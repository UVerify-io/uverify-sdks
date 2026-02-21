# uverify-sdk (Python)

Official Python SDK for the [UVerify](https://uverify.io) API.

## Requirements

- Python 3.8+
- [`requests`](https://pypi.org/project/requests/) ≥ 2.28

## Installation

```bash
pip install uverify-sdk
```

## Quick Start

```python
from uverify_sdk import UVerifyClient

client = UVerifyClient()

# Verify a document hash
certificates = client.verify("a3b4c5d6...")
for cert in certificates:
    print(cert.transaction_hash, cert.creation_time)
```

## Usage

### Creating the client

```python
from uverify_sdk import UVerifyClient

# Connect to the public API (default)
client = UVerifyClient()

# Connect to a self-hosted instance
client = UVerifyClient(base_url="https://my-instance.example.com")

# Add custom headers (e.g. an API key) and set a custom timeout
client = UVerifyClient(headers={"X-Api-Key": "your-key"}, timeout=60)
```

### Verify a certificate

```python
# By data hash
certificates = client.verify("sha256-or-sha512-hex-hash")

# By transaction hash + data hash
cert = client.verify_by_transaction("cardano-tx-hash", "data-hash")
```

### Issue certificates

Issuing is a two-step process: build an unsigned transaction, sign it
with your wallet, then submit it.

```python
from uverify_sdk.models import BuildTransactionRequest, CertificateData

response = client.build_transaction(
    BuildTransactionRequest(
        type="default",
        address="addr1...",
        state_id="your-state-id",
        certificates=[
            CertificateData(hash="sha256-hash-of-document", algorithm="SHA-256"),
        ],
    )
)

# Sign response.unsigned_transaction with your wallet, then:
client.submit_transaction(signed_tx_cbor_hex)
```

### User state management

```python
from uverify_sdk.models import UserActionRequest, ExecuteUserActionRequest

# Step 1: get a server-signed challenge
request = client.request_user_action(
    UserActionRequest(address="addr1...", action="USER_INFO")
)

# Step 2: sign request.message with your CIP-30 wallet, then execute
result = client.execute_user_action(
    ExecuteUserActionRequest(
        address=request.address,
        action=request.action,
        message=request.message,
        signature=request.signature,
        timestamp=request.timestamp,
        user_signature=wallet_signature_hex,
        user_public_key=wallet_public_key_hex,
    )
)
print(result.state)
```

### Connected Goods

```python
from uverify_sdk.models import (
    MintConnectedGoodsRequest,
    ConnectedGoodsItemInput,
    ClaimUpdateConnectedGoodsRequest,
    SocialHub,
)

# Mint a batch
response = client.mint_connected_goods_batch(
    MintConnectedGoodsRequest(
        address="addr1...",
        token_name="MY_ITEM",
        items=[
            ConnectedGoodsItemInput(password="secret1", asset_name="ITEM001"),
            ConnectedGoodsItemInput(password="secret2", asset_name="ITEM002"),
        ],
    )
)
batch_id = response.batch_id

# Claim an item
client.claim_connected_goods_item(
    ClaimUpdateConnectedGoodsRequest(
        batch_id=batch_id,
        password="secret1",
        user_address="addr1...",
        social_hub=SocialHub(name="Alice", twitter="@alice"),
    )
)
```

### Statistics

```python
fees = client.get_transaction_fees()
categories = client.get_certificates_by_category()
```

## Error Handling

```python
from uverify_sdk import UVerifyApiError

try:
    certs = client.verify("bad-hash")
except UVerifyApiError as e:
    print(f"API error {e.status_code}: {e}")
```

## API Reference

All methods map to the [UVerify REST API](https://api.uverify.io/v1/api-docs).

| Method | Endpoint |
|--------|----------|
| `verify(hash)` | `GET /api/v1/verify/{hash}` |
| `verify_by_transaction(tx_hash, data_hash)` | `GET /api/v1/verify/by-transaction-hash/{txHash}/{dataHash}` |
| `build_transaction(request)` | `POST /api/v1/transaction/build` |
| `submit_transaction(tx, witness_set?)` | `POST /api/v1/transaction/submit` |
| `request_user_action(request)` | `POST /api/v1/user/request/action` |
| `execute_user_action(request)` | `POST /api/v1/user/state/action` |
| `mint_connected_goods_batch(request)` | `POST /api/v1/extension/connected-goods/mint/batch` |
| `claim_connected_goods_item(request)` | `POST /api/v1/extension/connected-goods/claim/item` |
| `update_connected_goods_item(request)` | `POST /api/v1/extension/connected-goods/update/item` |
| `get_connected_goods_item(batch_ids, item_id)` | `GET /api/v1/extension/connected-goods/{batchIds}/{itemId}` |
| `get_transaction_scripts(tx_hash)` | `GET /api/v1/txs/{txHash}/scripts` |
| `get_transaction_redeemers(tx_hash)` | `GET /api/v1/txs/{txHash}/redeemers` |
| `get_script(script_hash)` | `GET /api/v1/scripts/{scriptHash}` |
| `get_script_json(script_hash)` | `GET /api/v1/scripts/{scriptHash}/json` |
| `get_script_details(script_hash)` | `GET /api/v1/scripts/{scriptHash}/details` |
| `get_script_cbor(script_hash)` | `GET /api/v1/scripts/{scriptHash}/cbor` |
| `get_datum(datum_hash)` | `GET /api/v1/scripts/datum/{datumHash}` |
| `get_datum_cbor(datum_hash)` | `GET /api/v1/scripts/datum/{datumHash}/cbor` |
| `get_transaction_fees()` | `GET /api/v1/statistic/tx-fees` |
| `get_certificates_by_category()` | `GET /api/v1/statistic/certificate/by-category` |
