package org.acme.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ReporteResourceTest {

    @Inject
    EntityManager em;

    private final UUID TEST_USER_ID  = TestFirebaseAuthFilter.TEST_USER_ID;
    private final UUID OTHER_USER_ID = UUID.fromString("ee000000-0000-0000-0000-000000000001");
    private final UUID REPORTE_PROPIO = UUID.fromString("dd000000-0000-0000-0000-000000000001");
    private final UUID REPORTE_AJENO  = UUID.fromString("dd000000-0000-0000-0000-000000000002");

    @BeforeEach
    @Transactional
    void setUp() {
        LocalDateTime ahora = LocalDateTime.now();

        em.createNativeQuery(
                "INSERT INTO Reporte (id, usuario_id, titulo, tipo, referencia_id, fecha_creacion) " +
                "VALUES (:id, :usuarioId, :titulo, :tipo, :referenciaId, :fechaCreacion)")
                .setParameter("id",            REPORTE_PROPIO.toString())
                .setParameter("usuarioId",     TEST_USER_ID.toString())
                .setParameter("titulo",        "Reporte propio")
                .setParameter("tipo",          "DASHBOARD")
                .setParameter("referenciaId",  "dash-001")
                .setParameter("fechaCreacion", ahora)
                .executeUpdate();

        em.createNativeQuery(
                "INSERT INTO Reporte (id, usuario_id, titulo, tipo, referencia_id, fecha_creacion) " +
                "VALUES (:id, :usuarioId, :titulo, :tipo, :referenciaId, :fechaCreacion)")
                .setParameter("id",            REPORTE_AJENO.toString())
                .setParameter("usuarioId",     OTHER_USER_ID.toString())
                .setParameter("titulo",        "Reporte ajeno")
                .setParameter("tipo",          "PROYECCION")
                .setParameter("referenciaId",  null)
                .setParameter("fechaCreacion", ahora.minusHours(1))
                .executeUpdate();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        em.createNativeQuery("DELETE FROM Reporte WHERE usuario_id IN (:uid1, :uid2)")
                .setParameter("uid1", TEST_USER_ID.toString())
                .setParameter("uid2", OTHER_USER_ID.toString())
                .executeUpdate();
    }

    // ── POST /reportes ────────────────────────────────────────────────────────

    @Test
    void postShouldReturn201WhenValidData() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("""
                        {
                            "titulo": "Nuevo reporte",
                            "tipo": "ACTIVIDAD",
                            "referenciaId": "act-001"
                        }
                        """)
                .when()
                .post("/reportes")
                .then()
                .statusCode(201);
    }

    @Test
    void postShouldReturnExpectedFields() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("""
                        {
                            "titulo": "Reporte con campos",
                            "tipo": "DASHBOARD"
                        }
                        """)
                .when()
                .post("/reportes")
                .then()
                .statusCode(201)
                .body("id",            notNullValue())
                .body("titulo",        equalTo("Reporte con campos"))
                .body("tipo",          equalTo("DASHBOARD"))
                .body("fechaCreacion", notNullValue());
    }

    @Test
    void postShouldReturn400WhenTituloIsMissing() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("""
                        {
                            "tipo": "DASHBOARD"
                        }
                        """)
                .when()
                .post("/reportes")
                .then()
                .statusCode(400);
    }

    @Test
    void postShouldReturn400WhenTipoIsMissing() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("""
                        {
                            "titulo": "Reporte sin tipo"
                        }
                        """)
                .when()
                .post("/reportes")
                .then()
                .statusCode(400);
    }

    // ── GET /reportes ─────────────────────────────────────────────────────────

    @Test
    void getShouldReturn200() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/reportes")
                .then()
                .statusCode(200);
    }

    @Test
    void getShouldReturnOwnReportes() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/reportes")
                .then()
                .statusCode(200)
                .body("titulo", hasItem("Reporte propio"));
    }

    @Test
    void getShouldReturnExpectedFields() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/reportes")
                .then()
                .statusCode(200)
                .body("[0].id",            notNullValue())
                .body("[0].titulo",        notNullValue())
                .body("[0].tipo",          notNullValue())
                .body("[0].fechaCreacion", notNullValue());
    }

    @Test
    void getShouldNotReturnOtherUsersReportes() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/reportes")
                .then()
                .statusCode(200)
                .body("titulo", not(hasItem("Reporte ajeno")));
    }

    // ── DELETE /reportes/{id} ─────────────────────────────────────────────────

    @Test
    void deleteShouldReturn204WhenOwner() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .delete("/reportes/{id}", REPORTE_PROPIO)
                .then()
                .statusCode(204);
    }

    @Test
    void deleteShouldReturn404WhenNotFound() {
        UUID unknownId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .delete("/reportes/{id}", unknownId)
                .then()
                .statusCode(404)
                .body("message", notNullValue());
    }

    @Test
    void deleteShouldReturn403WhenNotOwner() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .delete("/reportes/{id}", REPORTE_AJENO)
                .then()
                .statusCode(403)
                .body("message", notNullValue());
    }
}
