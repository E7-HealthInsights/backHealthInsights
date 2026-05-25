package org.acme.infrastructure.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente delgado para OpenAI Chat Completions con response_format json_schema (strict).
 * Usa java.net.http.HttpClient — sin dependencias nuevas.
 */
@ApplicationScoped
public class OpenAIClient {

    @ConfigProperty(name = "openai.api.key")
    String apiKey;

    @ConfigProperty(name = "openai.model")
    String model;

    @ConfigProperty(name = "openai.endpoint")
    String endpoint;

    @ConfigProperty(name = "openai.timeout-seconds")
    int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient httpClient;

    private HttpClient client() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();
        }
        return httpClient;
    }

    /**
     * Llama a OpenAI con json_schema strict y devuelve el JSON parseado del campo content.
     *
     * @param systemPrompt mensaje system
     * @param userPrompt   mensaje user
     * @param schemaName   identificador del schema (snake_case)
     * @param schema       JsonNode con el schema JSON (sin envoltorio name/strict/schema)
     * @return JsonNode raíz del objeto generado por el modelo
     */
    public JsonNode chatCompletionJsonSchema(String systemPrompt,
                                             String userPrompt,
                                             String schemaName,
                                             JsonNode schema) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new OpenAINotConfiguredException();
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        ArrayNode messages = root.putArray("messages");
        ObjectNode sysMsg = messages.addObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);

        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_schema");
        ObjectNode jsonSchema = responseFormat.putObject("json_schema");
        jsonSchema.put("name", schemaName);
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", schema);

        String body;
        try {
            body = objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new OpenAIInvalidResponseException("No se pudo serializar el request a OpenAI.", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = client().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new OpenAIInvalidResponseException("Error de red al llamar a OpenAI: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new OpenAIInvalidResponseException(
                    "OpenAI devolvió HTTP " + response.statusCode() + ": " + truncate(response.body(), 500));
        }

        try {
            JsonNode parsed = objectMapper.readTree(response.body());
            JsonNode choices = parsed.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new OpenAIInvalidResponseException("Respuesta de OpenAI sin choices.");
            }
            JsonNode message = choices.get(0).get("message");
            if (message == null) {
                throw new OpenAIInvalidResponseException("Respuesta de OpenAI sin message.");
            }
            JsonNode refusal = message.get("refusal");
            if (refusal != null && !refusal.isNull() && !refusal.asText().isBlank()) {
                throw new OpenAIInvalidResponseException("OpenAI rechazó la petición: " + refusal.asText());
            }
            JsonNode content = message.get("content");
            if (content == null || content.isNull()) {
                throw new OpenAIInvalidResponseException("Respuesta de OpenAI sin content.");
            }
            return objectMapper.readTree(content.asText());
        } catch (OpenAIInvalidResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenAIInvalidResponseException("No se pudo parsear la respuesta de OpenAI: " + e.getMessage(), e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
