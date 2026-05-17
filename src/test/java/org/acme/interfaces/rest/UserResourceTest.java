package org.acme.interfaces.rest;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.infrastructure.entities.RoleEntity;
import org.acme.infrastructure.entities.UserEntity;
import org.acme.infrastructure.firebase.FirebaseUserCreator;
import org.acme.infrastructure.repository.RoleRepositoryImpl;
import org.acme.infrastructure.repository.UserRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class UserResourceTest {

    @Inject
    UserRepositoryImpl userRepository;

    @Inject
    RoleRepositoryImpl roleRepository;

    @InjectMock
    FirebaseUserCreator firebaseUserCreator;

    private final UUID TEST_USER_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    private Byte adminRoleId;
    private Byte otherRoleId;

    @BeforeEach
    @Transactional
    void setUp() throws FirebaseAuthException {
        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn("firebase-mock-uid");
        when(firebaseUserCreator.create(anyString(), anyString())).thenReturn(mockRecord);
        RoleEntity admin = new RoleEntity();
        admin.setName("ADMIN");
        roleRepository.persist(admin);
        adminRoleId = admin.getId();

        RoleEntity finanzas = new RoleEntity();
        finanzas.setName("FINANZAS");
        roleRepository.persist(finanzas);
        otherRoleId = finanzas.getId();

        UserEntity user = new UserEntity();
        user.setId(TEST_USER_ID);
        user.setName("Juan");
        user.setLastName("Pérez");
        user.setEmail("juan@integration-test.com");
        user.setStatus(true);
        user.setProviderId("firebase-integration-uid");
        user.setRole(admin);
        userRepository.persist(user);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Borra todos los usuarios que referencien los roles de prueba
        // (cubre tanto el usuario del setUp como cualquier usuario creado en los POST tests)
        if (adminRoleId != null) userRepository.delete("role.id = ?1", adminRoleId);
        if (otherRoleId != null) userRepository.delete("role.id = ?1", otherRoleId);
        if (adminRoleId != null) roleRepository.deleteById(adminRoleId);
        if (otherRoleId != null) roleRepository.deleteById(otherRoleId);
    }

    // ── PUT /users/{id} ───────────────────────────────────────────────────────

    @Test
    void updateUserShouldReturn200WhenChangingRole() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("{\"roleId\": " + otherRoleId + "}")
                .when()
                .put("/users/{id}", TEST_USER_ID)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("Juan"))
                .body("email", equalTo("juan@integration-test.com"))
                .body("role", equalTo("FINANZAS"));
    }

    @Test
    void updateUserShouldReturn200WhenChangingNameAndLastName() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("{\"name\": \"Carlos\", \"lastName\": \"López\"}")
                .when()
                .put("/users/{id}", TEST_USER_ID)
                .then()
                .statusCode(200)
                .body("name", equalTo("Carlos"))
                .body("lastName", equalTo("López"))
                .body("role", equalTo("ADMIN"));
    }

    @Test
    void updateUserShouldReturn200WhenDeactivatingUser() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("{\"status\": false}")
                .when()
                .put("/users/{id}", TEST_USER_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo(false));
    }

    @Test
    void updateUserShouldReturn200WithAllFieldsUpdated() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("""
                        {
                            "name": "Carlos",
                            "lastName": "López",
                            "roleId": %d,
                            "status": false
                        }
                        """.formatted(otherRoleId))
                .when()
                .put("/users/{id}", TEST_USER_ID)
                .then()
                .statusCode(200)
                .body("name", equalTo("Carlos"))
                .body("lastName", equalTo("López"))
                .body("role", equalTo("FINANZAS"))
                .body("status", equalTo(false))
                .body("email", equalTo("juan@integration-test.com"));
    }

    @Test
    void updateUserShouldReturn404WhenUserNotFound() {
        UUID unknownId = UUID.randomUUID();

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("{\"name\": \"Test\"}")
                .when()
                .put("/users/{id}", unknownId)
                .then()
                .statusCode(404)
                .body("message", notNullValue());
    }

    @Test
    void updateUserShouldReturn400WhenRoleNotFound() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("{\"roleId\": 99}")
                .when()
                .put("/users/{id}", TEST_USER_ID)
                .then()
                .statusCode(400)
                .body("message", notNullValue());
    }

    @Test
    void updateUserShouldReturnAllExpectedResponseFields() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("{\"name\": \"Test\"}")
                .when()
                .put("/users/{id}", TEST_USER_ID)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", notNullValue())
                .body("lastName", notNullValue())
                .body("email", notNullValue())
                .body("role", notNullValue())
                .body("status", notNullValue());
    }

    // ── GET /users ────────────────────────────────────────────────────────────

    @Test
    void getUsersShouldReturn200WithUserList() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("email", hasItem("juan@integration-test.com"));
    }

    @Test
    void getUsersShouldReturnExpectedFields() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue())
                .body("[0].lastName", notNullValue())
                .body("[0].email", notNullValue())
                .body("[0].role", notNullValue())
                .body("[0].status", notNullValue());
    }

    // ── POST /users ───────────────────────────────────────────────────────────

    @Test
    void createUserShouldReturn201WhenValidData() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("""
                        {
                            "name": "Ana",
                            "lastName": "García",
                            "email": "ana@test-create.com",
                            "password": "Password1",
                            "roleId": %d
                        }
                        """.formatted(adminRoleId))
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Ana"))
                .body("email", equalTo("ana@test-create.com"));
    }

    @Test
    void createUserShouldReturn409WhenEmailAlreadyExists() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("""
                        {
                            "name": "Otro",
                            "lastName": "Usuario",
                            "email": "juan@integration-test.com",
                            "password": "Password1",
                            "roleId": %d
                        }
                        """.formatted(adminRoleId))
                .when()
                .post("/users")
                .then()
                .statusCode(409)
                .body("message", notNullValue());
    }

    @Test
    void createUserShouldReturn400WhenRoleNotFound() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("""
                        {
                            "name": "Luis",
                            "lastName": "Martínez",
                            "email": "luis@test-create.com",
                            "password": "Password1",
                            "roleId": 99
                        }
                        """)
                .when()
                .post("/users")
                .then()
                .statusCode(400)
                .body("message", notNullValue());
    }

    @Test
    void createUserShouldReturn400WhenRequiredFieldsMissing() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("{}")
                .when()
                .post("/users")
                .then()
                .statusCode(400);
    }

    @Test
    void createUserShouldReturn400WhenPasswordTooWeak() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body("""
                        {
                            "name": "Luis",
                            "lastName": "Martínez",
                            "email": "luis@test-create.com",
                            "password": "1234",
                            "roleId": %d
                        }
                        """.formatted(adminRoleId))
                .when()
                .post("/users")
                .then()
                .statusCode(400);
    }
}
