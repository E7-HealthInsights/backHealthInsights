package org.acme.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.infrastructure.entities.RoleEntity;
import org.acme.infrastructure.entities.UserEntity;
import org.acme.infrastructure.repository.RoleRepositoryImpl;
import org.acme.infrastructure.repository.UserRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Pruebas de integración para GET /auth/me.
 *
 * El filtro real de Firebase se reemplaza por {@link TestFirebaseAuthFilter},
 * que inyecta automáticamente un usuario ADMIN fijo sin necesitar un token JWT real.
 * Por eso, estos tests validan el comportamiento del endpoint cuando el usuario
 * ya está autenticado (todos los casos positivos).
 *
 * Los casos de 401 (sin token / token malformado) se prueban en
 * {FirebaseAuthFilterTest}, que testea el filtro de forma unitaria.
 */
@QuarkusTest
class AuthResourceTest {

    @Inject
    UserRepositoryImpl userRepository;

    @Inject
    RoleRepositoryImpl roleRepository;

    // UUID que debe coincidir con el que inyecta TestFirebaseAuthFilter
    private final UUID AUTH_USER_ID = TestFirebaseAuthFilter.TEST_USER_ID;

    private Byte adminRoleId;

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    @Transactional
    void setUp() {
        RoleEntity admin = new RoleEntity();
        admin.setName("ADMIN");
        roleRepository.persist(admin);
        adminRoleId = admin.getId();

        // Usuario que TestFirebaseAuthFilter inyecta como "autenticado"
        UserEntity user = new UserEntity();
        user.setId(AUTH_USER_ID);
        user.setName("Test");
        user.setLastName("User");
        user.setEmail("test@test.com");
        user.setStatus(true);
        user.setProviderId("test-firebase-uid");
        user.setRole(admin);
        userRepository.persist(user);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        if (adminRoleId != null) {
            userRepository.delete("role.id = ?1", adminRoleId);
            roleRepository.deleteById(adminRoleId);
        }
    }

    // ── GET /auth/me — respuesta exitosa ──────────────────────────────────────

    @Test
    void getMeShouldReturn200WhenAuthenticated() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200);
    }

    @Test
    void getMeShouldReturnCorrectName() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("name", equalTo("Test"));
    }

    @Test
    void getMeShouldReturnCorrectLastName() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("lastName", equalTo("User"));
    }

    @Test
    void getMeShouldReturnCorrectEmail() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("email", equalTo("test@test.com"));
    }

    @Test
    void getMeShouldReturnCorrectRole() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("role", equalTo("ADMIN"));
    }

    @Test
    void getMeShouldReturnStatusTrue() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("status", equalTo(true));
    }

    @Test
    void getMeShouldReturnNonNullId() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Test
    void getMeShouldReturnAllExpectedFields() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("id",       notNullValue())
                .body("name",     notNullValue())
                .body("lastName", notNullValue())
                .body("email",    notNullValue())
                .body("role",     notNullValue())
                .body("status",   notNullValue());
    }

    @Test
    void getMeShouldReturnJsonContentType() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }
}