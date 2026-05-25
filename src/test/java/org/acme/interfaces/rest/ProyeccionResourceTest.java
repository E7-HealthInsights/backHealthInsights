package org.acme.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@QuarkusTest
// Prueba de integración — levanta Quarkus con H2 y HTTP real
// TestFirebaseAuthFilter inyecta ADMIN
// ADMIN no tiene acceso a /proyecciones → verifica seguridad por rol
public class ProyeccionResourceTest {

    // ─── POST /proyecciones ───────────────────────────────────────────────────

    @Test
    void postProyeccionShouldReturn403WhenUserIsAdmin() {
        String body = """
            {
              "titulo": "Escenario test",
              "descripcion": "Test",
              "resultado": "{\\"params\\":{},\\"puntos\\":[],\\"kpis\\":{}}"
            }
            """;

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when().post("/proyecciones")
                .then()
                .statusCode(403);
    }

    @Test
    void postProyeccionShouldReturn403WhenNoToken() {
        given()
                .contentType(JSON)
                .body("{\"titulo\":\"test\",\"resultado\":\"{}\"}")
                .when().post("/proyecciones")
                .then()
                .statusCode(403);
    }

    // ─── GET /proyecciones ────────────────────────────────────────────────────

    @Test
    void getProyeccionesShouldReturn403WhenUserIsAdmin() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/proyecciones")
                .then()
                .statusCode(403);
    }

    @Test
    void getProyeccionesShouldReturn403WhenNoToken() {
        given()
                .when().get("/proyecciones")
                .then()
                .statusCode(403);
    }

    // ─── PATCH /proyecciones/{id} ─────────────────────────────────────────────

    @Test
    void patchProyeccionShouldReturn403WhenUserIsAdmin() {
        String body = """
            {
              "titulo": "Actualizado",
              "resultado": "{}"
            }
            """;

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when().patch("/proyecciones/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    // ─── DELETE /proyecciones/{id} ────────────────────────────────────────────

    @Test
    void deleteProyeccionShouldReturn403WhenUserIsAdmin() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().delete("/proyecciones/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    // ─── GET /proyecciones/simular/finanzas ───────────────────────────────────

    @Test
    void simularFinanzasShouldReturn403WhenUserIsAdmin() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/proyecciones/simular/finanzas" +
                            "?presupuesto=2000&nutricion=25&medicamentos=25" +
                            "&deteccion=25&atencion=25&hasta=2040")
                .then()
                .statusCode(403);
    }

    @Test
    void simularFinanzasShouldReturn403WhenNoToken() {
        given()
                .when().get("/proyecciones/simular/finanzas?presupuesto=2000&hasta=2040")
                .then()
                .statusCode(403);
    }

    // ─── GET /proyecciones/simular/general ────────────────────────────────────

    @Test
    void simularGeneralShouldReturn403WhenUserIsAdmin() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/proyecciones/simular/general" +
                            "?tasaCrecimiento=2.1&intensidadPolitica=20&hasta=2040")
                .then()
                .statusCode(403);
    }

    @Test
    void simularGeneralShouldReturn403WhenNoToken() {
        given()
                .when().get("/proyecciones/simular/general?hasta=2040")
                .then()
                .statusCode(403);
    }
}