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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class UserResourceTest {

    @Inject
    UserRepositoryImpl userRepository;

    @Inject
    RoleRepositoryImpl roleRepository;

    @InjectMock
    FirebaseUserCreator firebaseUserCreator;

    private static final UUID EXISTING_USER_ID =
            UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    private Byte adminRoleId;
    private UUID createdUserId;

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    @Transactional
    void setUp() {
        RoleEntity adminRole = new RoleEntity();
        adminRole.setName("ADMIN");
        roleRepository.persist(adminRole);
        adminRoleId = adminRole.getId();

        UserEntity existingUser = new UserEntity();
        existingUser.setId(EXISTING_USER_ID);
        existingUser.setName("María");
        existingUser.setLastName("García");
        existingUser.setEmail("existing@test.com");
        existingUser.setRole(adminRole);
        existingUser.setStatus(true);
        existingUser.setProviderId("firebase-uid-existing");
        userRepository.persist(existingUser);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        if (createdUserId != null) {
            userRepository.deleteById(createdUserId);
            createdUserId = null;
        }
        userRepository.deleteById(EXISTING_USER_ID);
        roleRepository.deleteById(adminRoleId);
    }

    // ── GET /users ────────────────────────────────────────────────────────────

    @Test
    void getUsersShouldReturn200WithUsersList() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void getUsersShouldReturnExpectedFields() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("[0].id",       notNullValue())
                .body("[0].name",     notNullValue())
                .body("[0].lastName", notNullValue())
                .body("[0].email",    notNullValue())
                .body("[0].role",     notNullValue())
                .body("[0].status",   notNullValue());
    }

    @Test
    void getUsersShouldContainExistingUser() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("email", hasItem("existing@test.com"))
                .body("name",  hasItem("María"));
    }

    // ── POST /users ───────────────────────────────────────────────────────────

    @Test
    void createUserShouldReturn201WhenValidData() throws FirebaseAuthException {
        UserRecord mockRecord = mock(UserRecord.class);
        when(mockRecord.getUid()).thenReturn("new-firebase-uid");
        when(firebaseUserCreator.create(anyString(), anyString())).thenReturn(mockRecord);

        String body = """
                {
                    "name": "Juan",
                    "lastName": "Pérez",
                    "email": "nuevo@test.com",
                    "password": "Password1",
                    "roleId": %d
                }
                """.formatted(adminRoleId);

        String responseBody = given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .body("id",       notNullValue())
                .body("name",     equalTo("Juan"))
                .body("lastName", equalTo("Pérez"))
                .body("email",    equalTo("nuevo@test.com"))
                .body("status",   equalTo(true))
                .extract().asString();

        String rawId = io.restassured.path.json.JsonPath.from(responseBody).getString("id");
        createdUserId = UUID.fromString(rawId);
    }

    @Test
    void createUserShouldReturn409WhenEmailAlreadyExists() {
        String body = """
                {
                    "name": "Ana",
                    "lastName": "López",
                    "email": "existing@test.com",
                    "password": "Password1",
                    "roleId": %d
                }
                """.formatted(adminRoleId);

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when()
                .post("/users")
                .then()
                .statusCode(409)
                .body("message", notNullValue());
    }

    @Test
    void createUserShouldReturn400WhenRoleNotFound() {
        String body = """
                {
                    "name": "Luis",
                    "lastName": "Torres",
                    "email": "luis@test.com",
                    "password": "Password1",
                    "roleId": 99
                }
                """;

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when()
                .post("/users")
                .then()
                .statusCode(400)
                .body("message", notNullValue());
    }

    @Test
    void createUserShouldReturn400WhenNameIsBlank() {
        String body = """
                {
                    "name": "",
                    "lastName": "Torres",
                    "email": "luis2@test.com",
                    "password": "Password1",
                    "roleId": %d
                }
                """.formatted(adminRoleId);

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when()
                .post("/users")
                .then()
                .statusCode(400);
    }

    @Test
    void createUserShouldReturn400WhenEmailIsInvalid() {
        String body = """
                {
                    "name": "Carlos",
                    "lastName": "Ruiz",
                    "email": "not-an-email",
                    "password": "Password1",
                    "roleId": %d
                }
                """.formatted(adminRoleId);

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when()
                .post("/users")
                .then()
                .statusCode(400);
    }

    @Test
    void createUserShouldReturn400WhenPasswordTooWeak() {
        String body = """
                {
                    "name": "Carlos",
                    "lastName": "Ruiz",
                    "email": "carlos@test.com",
                    "password": "abc",
                    "roleId": %d
                }
                """.formatted(adminRoleId);

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when()
                .post("/users")
                .then()
                .statusCode(400);
    }
}
