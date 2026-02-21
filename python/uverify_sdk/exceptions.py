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


class UVerifyValidationError(UVerifyError):
    """Raised when a required parameter is missing or has an invalid value."""
