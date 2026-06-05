package org.acme.interfaces.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.acme.application.dto.DashboardStatsResponseDto;
import org.acme.application.usecase.GetDashboardStatsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
// Prueba de integración HTTP
// TestFirebaseAuthFilter inyecta ADMIN → debe tener acceso (200)
// @InjectMock evita que el use case llame a stored functions que no existen en H2
public class AdminResourceTest {

    @InjectMock
    GetDashboardStatsUseCase getDashboardStatsUseCase;

    @BeforeEach
    void setUp() {
        DashboardStatsResponseDto mockStats = new DashboardStatsResponseDto();
        mockStats.setUsuariosActivos(5);
        mockStats.setUsuariosInactivos(1);
        mockStats.setUsuariosPorRolAdmin(1);
        mockStats.setUsuariosPorRolDG(1);
        mockStats.setUsuariosPorRolDF(1);
        mockStats.setUsuariosPorRolDM(2);
        mockStats.setDatasetsActivos(3);
        mockStats.setDatasetsInactivos(1);

        when(getDashboardStatsUseCase.execute()).thenReturn(mockStats);
    }

    // ─── GET /admin/stats ─────────────────────────────────────────────────────

    @Test
    void getStatsShouldReturn200WhenUserIsAdmin() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/admin/stats")
                .then()
                .statusCode(200);
    }

    @Test
    void getStatsShouldReturnUsuariosActivosField() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/admin/stats")
                .then()
                .statusCode(200)
                .body("usuariosActivos", equalTo(5));
    }

    @Test
    void getStatsShouldReturnDatasetsReadyField() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/admin/stats")
                .then()
                .statusCode(200)
                .body("datasetsActivos", equalTo(3));
    }

    @Test
    void getStatsShouldReturnAllUsuariosPorRol() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/admin/stats")
                .then()
                .statusCode(200)
                .body("usuariosPorRolAdmin", equalTo(1))
                .body("usuariosPorRolDG",    equalTo(1))
                .body("usuariosPorRolDF",    equalTo(1))
                .body("usuariosPorRolDM",    equalTo(2));
    }


    @Test
    void getStatsShouldCallUseCaseExactlyOnce() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/admin/stats")
                .then()
                .statusCode(200);

        verify(getDashboardStatsUseCase, times(1)).execute();
    }
}