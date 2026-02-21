package io.uverify.sdk.exception;

/**
 * Thrown when required parameters are missing or invalid before an API call is made,
 * e.g. when a sign callback is required but was not provided.
 */
public class UVerifyValidationException extends RuntimeException {

    public UVerifyValidationException(String message) {
        super(message);
    }
}
