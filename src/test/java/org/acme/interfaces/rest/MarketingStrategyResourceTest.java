package org.acme.interfaces.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.acme.infrastructure.openai.OpenAIClient;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

/**
 * Prueba de integración para MarketingStrategyResource.
 *
 * El TestFirebaseAuthFilter inyecta un usuario con rol ADMIN, que no tiene
 * acceso a ningún endpoint de /marketing/strategies (todos requieren
 * DIRECTOR_MERCADOTECNIA). Todos los tests verifican que la capa de seguridad
 * rechaza correctamente las solicitudes de roles no autorizados.
 *
 * OpenAIClient se mockea para evitar llamadas reales a la API de OpenAI
 * en cualquier escenario donde la autenticación pudiera pasar.
 */
@QuarkusTest
class MarketingStrategyResourceTest {

    @InjectMock
    OpenAIClient openAIClient;

    // ── POST /marketing/strategies ────────────────────────────────────────────

    @Test
    void postStrategyShouldReturn403WhenUserIsAdmin() {
        String body = """
                {
                    "contextoExtra": "Enfoque en zonas rurales",
                    "horizonteMeses": 6,
                    "tono": "educativo"
                }
                """;

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when().post("/marketing/strategies")
                .then()
                .statusCode(403);
    }

    @Test
    void postStrategyShouldReturn403WhenNoToken() {
        given()
                .contentType(JSON)
                .body("{}")
                .when().post("/marketing/strategies")
                .then()
                .statusCode(403);
    }

    // ── GET /marketing/strategies ─────────────────────────────────────────────

    @Test
    void listStrategiesShouldReturn403WhenUserIsAdmin() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/marketing/strategies")
                .then()
                .statusCode(403);
    }

    @Test
    void listStrategiesShouldReturn403WhenNoToken() {
        given()
                .when().get("/marketing/strategies")
                .then()
                .statusCode(403);
    }

    // ── GET /marketing/strategies/{id} ────────────────────────────────────────

    @Test
    void getByIdShouldReturn403WhenUserIsAdmin() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/marketing/strategies/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    @Test
    void getByIdShouldReturn403WhenNoToken() {
        given()
                .when().get("/marketing/strategies/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    // ── PATCH /marketing/strategies/{id}/estado ───────────────────────────────

    @Test
    void updateEstadoShouldReturn403WhenUserIsAdmin() {
        String body = """
                {
                    "estado": "ejecutada"
                }
                """;

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when().patch("/marketing/strategies/" + UUID.randomUUID() + "/estado")
                .then()
                .statusCode(403);
    }

    @Test
    void updateEstadoShouldReturn403WhenNoToken() {
        given()
                .contentType(JSON)
                .body("{\"estado\":\"ejecutada\"}")
                .when().patch("/marketing/strategies/" + UUID.randomUUID() + "/estado")
                .then()
                .statusCode(403);
    }

    // ── POST /marketing/strategies/{id}/comentarios ───────────────────────────

    @Test
    void addComentarioShouldReturn403WhenUserIsAdmin() {
        String body = """
                {
                    "contenido": "Buen resultado en Jalisco"
                }
                """;

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when().post("/marketing/strategies/" + UUID.randomUUID() + "/comentarios")
                .then()
                .statusCode(403);
    }

    @Test
    void addComentarioShouldReturn403WhenNoToken() {
        given()
                .contentType(JSON)
                .body("{\"contenido\":\"Comentario\"}")
                .when().post("/marketing/strategies/" + UUID.randomUUID() + "/comentarios")
                .then()
                .statusCode(403);
    }
}
