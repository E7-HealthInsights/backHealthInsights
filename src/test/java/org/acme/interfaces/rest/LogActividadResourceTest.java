package org.acme.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.infrastructure.entities.RoleEntity;
import org.acme.infrastructure.entities.UserEntity;
import org.acme.infrastructure.repository.RoleRepositoryImpl;
import org.acme.infrastructure.repository.UserRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LogActividadResourceTest {

    @Inject
    RoleRepositoryImpl roleRepository;

    @Inject
    UserRepositoryImpl userRepository;

    @Inject
    EntityManager em;

    private final UUID USER_ID         = UUID.fromString("ff000000-0000-0000-0000-000000000001");
    private final UUID LOG_CON_USER_ID = UUID.fromString("ff000000-0000-0000-0000-000000000010");
    private final UUID LOG_SIN_USER_ID = UUID.fromString("ff000000-0000-0000-0000-000000000011");

    private Byte roleId;

    @BeforeEach
    @Transactional
    void setUp() {
        RoleEntity role = new RoleEntity();
        role.setName("ADMIN_LOG_TEST");
        roleRepository.persist(role);
        roleId = role.getId();

        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setName("Laura");
        user.setLastName("Gómez");
        user.setEmail("laura@actividad-test.com");
        user.setStatus(true);
        user.setProviderId("firebase-actividad-test-uid");
        user.setRole(role);
        userRepository.persist(user);

        LocalDateTime ahora = LocalDateTime.now();

        // Log vinculado a un usuario — debe devolver adminNombre = "Laura Gómez"
        em.createNativeQuery(
                "INSERT INTO LogActividad (id, usuario_id, accion, detalle, entidad_tipo, entidad_id, fecha) " +
                "VALUES (:id, :usuarioId, :accion, :detalle, :entidadTipo, :entidadId, :fecha)")
                .setParameter("id",          LOG_CON_USER_ID.toString())
                .setParameter("usuarioId",   USER_ID.toString())
                .setParameter("accion",      "CREAR_USUARIO")
                .setParameter("detalle",     "Alta justificada por auditoría")
                .setParameter("entidadTipo", "USUARIO")
                .setParameter("entidadId",   UUID.randomUUID().toString())
                .setParameter("fecha",       ahora)
                .executeUpdate();

        // Log sin usuario_id — debe devolver adminNombre = null (LEFT JOIN)
        em.createNativeQuery(
                "INSERT INTO LogActividad (id, usuario_id, accion, detalle, entidad_tipo, entidad_id, fecha) " +
                "VALUES (:id, :usuarioId, :accion, :detalle, :entidadTipo, :entidadId, :fecha)")
                .setParameter("id",          LOG_SIN_USER_ID.toString())
                .setParameter("usuarioId",   null)
                .setParameter("accion",      "EDITAR_DATASET")
                .setParameter("detalle",     null)
                .setParameter("entidadTipo", "DATASET")
                .setParameter("entidadId",   UUID.randomUUID().toString())
                .setParameter("fecha",       ahora.minusHours(1))
                .executeUpdate();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        em.createNativeQuery(
                "DELETE FROM LogActividad WHERE id IN (:id1, :id2)")
                .setParameter("id1", LOG_CON_USER_ID.toString())
                .setParameter("id2", LOG_SIN_USER_ID.toString())
                .executeUpdate();

        userRepository.deleteById(USER_ID);
        if (roleId != null) roleRepository.deleteById(roleId);
    }

    // ── GET /actividad ────────────────────────────────────────────────────────

    @Test
    void getAllShouldReturn200() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/actividad")
                .then()
                .statusCode(200);
    }

    @Test
    void getAllShouldReturnNonEmptyList() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/actividad")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void getAllShouldReturnExpectedFields() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/actividad")
                .then()
                .statusCode(200)
                .body("[0].id",          notNullValue())
                .body("[0].accion",      notNullValue())
                .body("[0].entidadTipo", notNullValue())
                .body("[0].entidadId",   notNullValue())
                .body("[0].fecha",       notNullValue());
    }

    @Test
    void getAllShouldReturnAdminNombreWhenUserLinked() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/actividad")
                .then()
                .statusCode(200)
                .body("adminNombre", hasItem("Laura Gómez"));
    }

    @Test
    void getAllShouldReturnNullAdminNombreWhenUserNotLinked() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/actividad")
                .then()
                .statusCode(200)
                .body("adminNombre", hasItem(nullValue()));
    }

    @Test
    void getAllShouldReturnDetalleWhenPresent() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/actividad")
                .then()
                .statusCode(200)
                .body("detalle", hasItem("Alta justificada por auditoría"));
    }

    @Test
    void getAllShouldReturnBothInsertedLogs() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/actividad")
                .then()
                .statusCode(200)
                .body("accion", hasItem("CREAR_USUARIO"))
                .body("accion", hasItem("EDITAR_DATASET"));
    }
}
