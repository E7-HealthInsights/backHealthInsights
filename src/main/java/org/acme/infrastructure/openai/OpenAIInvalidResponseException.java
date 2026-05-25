package org.acme.infrastructure.openai;

public class OpenAIInvalidResponseException extends RuntimeException {
    public OpenAIInvalidResponseException(String message) {
        super(message);
    }

    public OpenAIInvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
