"""Tests for UVerifyClient."""
import json as _json

import pytest
import responses as responses_lib

from uverify_sdk import (
    DataSignature,
    UVerifyClient,
    UVerifyCore,
)
from uverify_sdk.exceptions import UVerifyApiError, UVerifyValidationError
from uverify_sdk.models import (
    BuildTransactionRequest,
    CertificateData,
    ExecuteUserActionRequest,
    UserActionRequest,
    UserActionRequestResponse,
)

BASE_URL = "https://api.preprod.uverify.io"


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

CERT_DICT = {
    "hash": "abc123",
    "address": "addr1...",
    "blockHash": "bh1",
    "blockNumber": 100,
    "transactionHash": "tx1",
    "slot": 50000,
    "creationTime": "2024-01-01T00:00:00Z",
    "metadata": None,
    "issuer": "issuer1",
}

REQUEST_RESPONSE = {
    "address": "addr1...",
    "action": "USER_INFO",
    "message": "challenge-msg",
    "signature": "server-sig",
    "timestamp": 1700000000,
    "status": 200,
}

EXECUTE_RESPONSE = {"status": 200}


def sign_tx_stub(unsigned_tx: str) -> str:
    return "witness-set"


def sign_message_stub(message: str) -> DataSignature:
    return DataSignature(key="pub-key", signature="user-sig")


# ---------------------------------------------------------------------------
# verify()
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_verify_returns_certificate_list():
    responses_lib.add(responses_lib.GET, f"{BASE_URL}/api/v1/verify/abc123",
                      json=[CERT_DICT], status=200)
    client = UVerifyClient()
    result = client.verify("abc123")
    assert len(result) == 1
    assert result[0].hash == "abc123"
    assert result[0].transaction_hash == "tx1"


@responses_lib.activate
def test_verify_returns_empty_list_on_null_body():
    responses_lib.add(responses_lib.GET, f"{BASE_URL}/api/v1/verify/abc",
                      json=None, status=200)
    client = UVerifyClient()
    assert client.verify("abc") == []


@responses_lib.activate
def test_verify_raises_on_404():
    responses_lib.add(responses_lib.GET, f"{BASE_URL}/api/v1/verify/bad",
                      json={"error": "not found"}, status=404)
    with pytest.raises(UVerifyApiError) as exc_info:
        UVerifyClient().verify("bad")
    assert exc_info.value.status_code == 404


@responses_lib.activate
def test_verify_raises_on_500():
    responses_lib.add(responses_lib.GET, f"{BASE_URL}/api/v1/verify/bad",
                      body="server error", status=500)
    with pytest.raises(UVerifyApiError) as exc_info:
        UVerifyClient().verify("bad")
    assert exc_info.value.status_code == 500


# ---------------------------------------------------------------------------
# verify_by_transaction()
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_verify_by_transaction_returns_certificate():
    responses_lib.add(
        responses_lib.GET,
        f"{BASE_URL}/api/v1/verify/by-transaction-hash/tx1/h1",
        json=CERT_DICT, status=200)
    cert = UVerifyClient().verify_by_transaction("tx1", "h1")
    assert cert.transaction_hash == "tx1"
    assert cert.hash == "abc123"


# ---------------------------------------------------------------------------
# .core object
# ---------------------------------------------------------------------------

def test_core_is_uverify_core_instance():
    assert isinstance(UVerifyClient().core, UVerifyCore)


@responses_lib.activate
def test_core_build_transaction():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/transaction/build",
        json={"unsignedTransaction": "cbor-hex", "type": "bootstrap",
              "status": {"message": "ok", "code": 200}},
        status=200)
    resp = UVerifyClient().core.build_transaction(
        BuildTransactionRequest(
            type="bootstrap",
            address="addr1...",
            certificates=[CertificateData(hash="h1", algorithm="SHA-256")],
        )
    )
    assert resp.unsigned_transaction == "cbor-hex"


@responses_lib.activate
def test_core_submit_transaction():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/transaction/submit",
        status=200)
    UVerifyClient().core.submit_transaction("signed-tx", "witness")


@responses_lib.activate
def test_core_request_user_action():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/request/action",
        json=REQUEST_RESPONSE, status=200)
    resp = UVerifyClient().core.request_user_action(
        UserActionRequest(address="addr1...", action="USER_INFO")
    )
    assert resp.message == "challenge-msg"


@responses_lib.activate
def test_core_execute_user_action():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/state/action",
        json=EXECUTE_RESPONSE, status=200)
    req_resp = UserActionRequestResponse(
        address="addr1...", action="USER_INFO",
        message="msg", signature="sig",
        timestamp=1700000000, status=200,
    )
    resp = UVerifyClient().core.execute_user_action(
        ExecuteUserActionRequest(
            address=req_resp.address, action=req_resp.action,
            message=req_resp.message, signature=req_resp.signature,
            user_signature="user-sig", user_public_key="pub-key",
            timestamp=req_resp.timestamp,
        )
    )
    assert resp.status == 200


# ---------------------------------------------------------------------------
# issue_certificates()
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_issue_certificates_bootstrap_flow():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/transaction/build",
        json={"unsignedTransaction": "unsigned-cbor", "type": "bootstrap",
              "status": {"message": "ok", "code": 200}},
        status=200)
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/transaction/submit",
        status=200)

    UVerifyClient().issue_certificates(
        address="addr1...",
        certificates=[CertificateData(hash="h1", algorithm="SHA-256")],
        sign_tx=sign_tx_stub,
    )
    assert len(responses_lib.calls) == 2


@responses_lib.activate
def test_issue_certificates_with_state_id():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/transaction/build",
        json={"unsignedTransaction": "unsigned-cbor", "type": "default",
              "status": {"message": "ok", "code": 200}},
        status=200)
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/transaction/submit",
        status=200)

    UVerifyClient().issue_certificates(
        address="addr1...",
        certificates=[CertificateData(hash="h1")],
        sign_tx=sign_tx_stub,
        state_id="state-123",
    )
    body = _json.loads(responses_lib.calls[0].request.body)
    assert body["type"] == "default"
    assert body["stateId"] == "state-123"


@responses_lib.activate
def test_issue_certificates_uses_constructor_level_callback():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/transaction/build",
        json={"unsignedTransaction": "unsigned-cbor", "type": "bootstrap",
              "status": {"message": "ok", "code": 200}},
        status=200)
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/transaction/submit",
        status=200)

    client = UVerifyClient(sign_tx=sign_tx_stub)
    client.issue_certificates(
        address="addr1...",
        certificates=[CertificateData(hash="h1")],
    )


def test_issue_certificates_raises_when_no_callback():
    with pytest.raises(UVerifyValidationError):
        UVerifyClient().issue_certificates(
            address="addr1...",
            certificates=[CertificateData(hash="h1")],
        )


# ---------------------------------------------------------------------------
# get_user_info()
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_get_user_info_two_step_flow():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/request/action",
        json=REQUEST_RESPONSE, status=200)
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/state/action",
        json=EXECUTE_RESPONSE, status=200)

    resp = UVerifyClient().get_user_info("addr1...", sign_message=sign_message_stub)

    assert resp.status == 200
    assert len(responses_lib.calls) == 2
    # Verify action in step-1 body
    step1_body = _json.loads(responses_lib.calls[0].request.body)
    assert step1_body["action"] == "USER_INFO"


@responses_lib.activate
def test_get_user_info_uses_constructor_level_callback():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/request/action",
        json=REQUEST_RESPONSE, status=200)
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/state/action",
        json=EXECUTE_RESPONSE, status=200)

    client = UVerifyClient(sign_message=sign_message_stub)
    resp = client.get_user_info("addr1...")
    assert resp.status == 200


def test_get_user_info_raises_when_no_callback():
    with pytest.raises(UVerifyValidationError):
        UVerifyClient().get_user_info("addr1...")


# ---------------------------------------------------------------------------
# invalidate_state() / opt_out()
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_invalidate_state_sends_state_id():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/request/action",
        json={**REQUEST_RESPONSE, "action": "INVALIDATE_STATE"}, status=200)
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/state/action",
        json=EXECUTE_RESPONSE, status=200)

    resp = UVerifyClient().invalidate_state(
        "addr1...", "state-123", sign_message=sign_message_stub
    )
    assert resp.status == 200
    step1_body = _json.loads(responses_lib.calls[0].request.body)
    assert step1_body["action"] == "INVALIDATE_STATE"
    assert step1_body["stateId"] == "state-123"


@responses_lib.activate
def test_opt_out_sends_state_id():
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/request/action",
        json={**REQUEST_RESPONSE, "action": "OPT_OUT"}, status=200)
    responses_lib.add(
        responses_lib.POST, f"{BASE_URL}/api/v1/user/state/action",
        json=EXECUTE_RESPONSE, status=200)

    resp = UVerifyClient().opt_out(
        "addr1...", "state-123", sign_message=sign_message_stub
    )
    assert resp.status == 200
    step1_body = _json.loads(responses_lib.calls[0].request.body)
    assert step1_body["action"] == "OPT_OUT"


# ---------------------------------------------------------------------------
# Constructor options
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_custom_base_url():
    responses_lib.add(
        responses_lib.GET, "https://custom.example.com/api/v1/verify/h1",
        json=[CERT_DICT], status=200)
    client = UVerifyClient(base_url="https://custom.example.com")
    result = client.verify("h1")
    assert len(result) == 1


@responses_lib.activate
def test_custom_headers_sent():
    responses_lib.add(
        responses_lib.GET, f"{BASE_URL}/api/v1/verify/h1",
        json=[CERT_DICT], status=200)
    UVerifyClient(headers={"X-Api-Key": "secret"}).verify("h1")
    assert responses_lib.calls[0].request.headers.get("X-Api-Key") == "secret"
