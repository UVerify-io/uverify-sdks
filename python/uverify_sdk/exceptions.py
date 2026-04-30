"""Exceptions raised by the UVerify SDK."""


class UVerifyError(Exception):
    """Base class for all UVerify SDK errors."""


class UVerifyApiError(UVerifyError):
    """Raised when the UVerify API returns a non-2xx HTTP response."""

    def __init__(self, message: str, status_code: int, response_body: object = None) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.response_body = response_body

    def __repr__(self) -> str:
        return f"UVerifyApiError(status_code={self.status_code}, message={str(self)!r})"


class NotFoundError(UVerifyApiError):
    """Raised when the API returns HTTP 404 (resource not found).

    When thrown from :meth:`~uverify_sdk.UVerifyClient.fund_wallet`, this indicates
    the faucet endpoint is not available on this backend deployment.
    """

    def __init__(self, message: str, response_body: object = None) -> None:
        super().__init__(message, 404, response_body)


class RateLimitError(UVerifyApiError):
    """Raised when the API returns HTTP 429 (rate limit / cooldown active).

    Example::

        try:
            client.fund_wallet(address, sign_message)
        except RateLimitError:
            print("Faucet cooldown active, try again later.")
    """

    def __init__(self, message: str, response_body: object = None) -> None:
        super().__init__(message, 429, response_body)


class InsufficientFundsError(UVerifyApiError):
    """Raised when a transaction cannot be built because the wallet has no spendable UTXOs
    (HTTP 400, "No UTXOs found for user address").

    Catch this specifically to trigger a faucet top-up flow rather than retrying::

        try:
            client.issue_certificates(address, certs, sign_tx)
        except InsufficientFundsError:
            client.fund_wallet(address, sign_message)
    """

    def __init__(self, message: str, response_body: object = None) -> None:
        super().__init__(message, 400, response_body)


class UVerifyValidationError(UVerifyError):
    """Raised when a required parameter is missing or has an invalid value."""


class UVerifyTimeoutError(UVerifyError):
    """Raised by :func:`~uverify_sdk.wait_for` when the condition is not met within the timeout.

    Catch this specifically to distinguish a timeout from other errors::

        from uverify_sdk import wait_for, UVerifyTimeoutError

        try:
            wait_for(lambda: len(client.verify(hash)) > 0, timeout_ms=300_000)
        except UVerifyTimeoutError as e:
            print(e)  # advises the user to re-run
    """

    def __init__(self, timeout_ms: int) -> None:
        super().__init__(
            f"Transaction not confirmed within {timeout_ms // 1000} seconds. "
            "The transaction may still be processing — please re-run the script. "
            "If this happens repeatedly, increase the timeout value."
        )
        self.timeout_ms = timeout_ms
