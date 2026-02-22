# uverify-sdk (Python)

Official Python SDK for the [UVerify](https://app.uverify.io) API.

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
client = UVerifyClient(base_url="http://localhost:9090")

# Add custom headers and set a custom timeout
client = UVerifyClient(headers={"X-Custom-Header": "value"}, timeout=60)

# Register default signing callbacks so you don't pass them on every call
client = UVerifyClient(
    sign_message=lambda msg: wallet.sign_data(address, msg),
    sign_tx=lambda tx: wallet.sign_tx(tx),
)
```

### Verify a certificate

```python
# By data hash
certificates = client.verify("sha256-or-sha512-hex-hash")

# By transaction hash + data hash
cert = client.verify_by_transaction("cardano-tx-hash", "data-hash")
```

### Issue certificates

`issue_certificates` handles the full flow — build, sign, submit — in one call
and returns the Cardano transaction hash on success.

`metadata` can be a plain `dict`; the SDK serialises it to JSON automatically.

```python
from uverify_sdk.models import CertificateData

tx_hash = client.issue_certificates(
    address="addr1...",
    certificates=[
        CertificateData(
            hash="sha256-hash-of-document",
            algorithm="SHA-256",
            metadata={"issuer": "Acme Corp", "date": "2024-01-01"},
        )
    ],
    sign_tx=lambda tx: wallet.sign_tx(tx),  # omit if set in constructor
)
print("Certified at tx:", tx_hash)
```

Optionally pass a `state_id` to issue under a specific state:

```python
tx_hash = client.issue_certificates(
    address="addr1...",
    certificates=[CertificateData(hash="sha256-hash")],
    state_id="my-state-id",
)
```

### User state management

```python
# Retrieve current state
state = client.get_user_info("addr1...")
print("Certificates remaining:", state.countdown if state else None)

# Invalidate a state
client.invalidate_state("addr1...", "state-id")

# Opt out entirely
client.opt_out("addr1...", "state-id")
```

A per-call signing callback can be passed as the `sign_message` keyword argument
to any of these methods if you didn't register one in the constructor.

### Low-level access via `.core`

For advanced flows (multi-sig, custom submission logic) use the `.core` attribute
to call each step individually:

```python
from uverify_sdk.models import BuildTransactionRequest, CertificateData

# Build
response = client.core.build_transaction(
    BuildTransactionRequest(
        type="default",
        address="addr1...",
        state_id="your-state-id",
        certificates=[CertificateData(hash="sha256-hash", algorithm="SHA-256")],
    )
)

# Sign with your wallet, then submit
witness_set = wallet.sign_tx(response.unsigned_transaction)
tx_hash = client.core.submit_transaction(response.unsigned_transaction, witness_set)
```

```python
from uverify_sdk.models import UserActionRequest, ExecuteUserActionRequest

# Two-step user state action (manual)
challenge = client.core.request_user_action(
    UserActionRequest(address="addr1...", action="USER_INFO")
)

sig = wallet.sign_data(address, challenge.message)

result = client.core.execute_user_action(
    ExecuteUserActionRequest(
        address=challenge.address,
        action=challenge.action,
        message=challenge.message,
        signature=challenge.signature,
        timestamp=challenge.timestamp,
        user_signature=sig.signature,
        user_public_key=sig.key,
    )
)
print(result.state)
```

## Error Handling

```python
from uverify_sdk import UVerifyApiError, UVerifyValidationError

try:
    tx_hash = client.issue_certificates("addr1...", certs)
except UVerifyApiError as e:
    # HTTP error from the API (status code, response body available)
    print(f"API error {e.status_code}: {e}")
except UVerifyValidationError as e:
    # Missing sign callback — pass one to the method or set it in the constructor
    print(e)
```

## API Reference

### High-level helpers

| Method | Description |
|--------|-------------|
| `verify(hash)` | Look up all on-chain certificates for a data hash |
| `verify_by_transaction(tx_hash, data_hash)` | Fetch a specific certificate by tx hash + data hash |
| `issue_certificates(address, certificates, sign_tx?, state_id?)` | Build, sign, and submit a certificate transaction; returns tx hash |
| `get_user_info(address, sign_message?)` | Retrieve the current user state |
| `invalidate_state(address, state_id, sign_message?)` | Mark a state as invalid |
| `opt_out(address, state_id, sign_message?)` | Remove the user's state entirely |

### Low-level core (`.core`)

| Method | Endpoint |
|--------|----------|
| `core.build_transaction(request)` | `POST /api/v1/transaction/build` |
| `core.submit_transaction(tx, witness_set?)` | `POST /api/v1/transaction/submit` |
| `core.request_user_action(request)` | `POST /api/v1/user/request/action` |
| `core.execute_user_action(request)` | `POST /api/v1/user/state/action` |
