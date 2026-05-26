package org.acme.infrastructure.openai;

public class OpenAINotConfiguredException extends RuntimeException {
    public OpenAINotConfiguredException() {
        super("OpenAI API key no configurada en el servidor (variable OPENAI_API_KEY).");
    }
}
