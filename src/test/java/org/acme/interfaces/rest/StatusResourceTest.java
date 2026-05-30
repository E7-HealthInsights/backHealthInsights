package org.acme.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Prueba de integración para StatusResource.
 *
 * Es el único endpoint público del sistema — no requiere autenticación.
 * Verifica que GET /status responde 200 con los campos esperados.
 */
@QuarkusTest
class StatusResourceTest {

    // ── GET /status ───────────────────────────────────────────────────────────

    @Test
    void getShouldReturn200WithoutAuthentication() {
        given()
                .when().get("/status")
                .then()
                .statusCode(200);
    }

    @Test
    void getShouldReturn200WithAuthHeader() {
        // El endpoint es público — un token presente no debe afectar el resultado
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/status")
                .then()
                .statusCode(200);
    }

    @Test
    void getShouldReturnStatusUp() {
        given()
                .when().get("/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void getShouldReturnNameField() {
        given()
                .when().get("/status")
                .then()
                .statusCode(200)
                .body("name", notNullValue());
    }

    @Test
    void getShouldReturnVersionField() {
        given()
                .when().get("/status")
                .then()
                .statusCode(200)
                .body("version", notNullValue());
    }

    @Test
    void getShouldReturnTimestampField() {
        given()
                .when().get("/status")
                .then()
                .statusCode(200)
                .body("timestamp", notNullValue());
    }

    @Test
    void getShouldReturnJsonContentType() {
        given()
                .when().get("/status")
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"));
    }

    @Test
    void getShouldReturnAllExpectedFields() {
        given()
                .when().get("/status")
                .then()
                .statusCode(200)
                .body("status",    notNullValue())
                .body("name",      notNullValue())
                .body("version",   notNullValue())
                .body("timestamp", notNullValue());
    }
}
