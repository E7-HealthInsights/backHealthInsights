package org.acme.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.infrastructure.entities.DatasetEntity;
import org.acme.infrastructure.entities.MetricaEntity;
import org.acme.infrastructure.repository.DatasetRepositoryImpl;
import org.acme.infrastructure.repository.MetricaRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DatasetResourceTest {

    @Inject
    DatasetRepositoryImpl datasetRepository;

    @Inject
    MetricaRepositoryImpl metricaRepository;

    // IDs fijos para poder hacer cleanup exacto después de cada test
    private final UUID DATASET_ACTIVO_ID   = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private final UUID DATASET_INACTIVO_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private final UUID METRICA_1_ID        = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private final UUID METRICA_2_ID        = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    @Transactional
    void setUp() {
        // Dataset activo con dos métricas
        DatasetEntity activo = new DatasetEntity();
        activo.setId(DATASET_ACTIVO_ID);
        activo.setNombre("Diabetes México 2023");
        activo.setNombreTabla("diabetes_mexico_2023");
        activo.setDescripcion("Casos de diabetes por entidad");
        activo.setFuente("SINAVE");
        activo.setEstado(true);
        activo.setFechaActualizacion(LocalDateTime.now());
        datasetRepository.persist(activo);

        MetricaEntity m1 = new MetricaEntity();
        m1.setId(METRICA_1_ID);
        m1.setNombre("Estado");
        m1.setColumnaCsv("estado");
        m1.setUnidad(null);
        m1.setDataset(activo);
        metricaRepository.persist(m1);

        MetricaEntity m2 = new MetricaEntity();
        m2.setId(METRICA_2_ID);
        m2.setNombre("Casos");
        m2.setColumnaCsv("casos");
        m2.setUnidad(null);
        m2.setDataset(activo);
        metricaRepository.persist(m2);

        // Dataset inactivo — no debe aparecer en GET /datasets
        DatasetEntity inactivo = new DatasetEntity();
        inactivo.setId(DATASET_INACTIVO_ID);
        inactivo.setNombre("Dataset Inactivo");
        inactivo.setNombreTabla("dataset_inactivo");
        inactivo.setEstado(false);
        inactivo.setFechaActualizacion(LocalDateTime.now());
        datasetRepository.persist(inactivo);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Borrar métricas antes que datasets (FK)
        metricaRepository.deleteById(METRICA_1_ID);
        metricaRepository.deleteById(METRICA_2_ID);
        datasetRepository.deleteById(DATASET_ACTIVO_ID);
        datasetRepository.deleteById(DATASET_INACTIVO_ID);
    }

    // ── GET /datasets ─────────────────────────────────────────────────────────

    @Test
    void getDatasetsShouldReturn200WithActiveDatasets() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/datasets")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("nombre", hasItem("Diabetes México 2023"));
    }

    @Test
    void getDatasetsShouldNotReturnInactiveDatasets() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/datasets")
                .then()
                .statusCode(200)
                .body("nombre", not(hasItem("Dataset Inactivo")));
    }

    @Test
    void getDatasetsShouldReturnExpectedFields() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/datasets")
                .then()
                .statusCode(200)
                .body("[0].id",          notNullValue())
                .body("[0].nombre",      notNullValue())
                .body("[0].fuente",      notNullValue());
    }

    // ── GET /datasets/{id}/metricas ───────────────────────────────────────────

    @Test
    void getMetricasShouldReturn200WithMetricasForActiveDataset() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/datasets/{id}/metricas", DATASET_ACTIVO_ID)
                .then()
                .statusCode(200)
                .body("$",      hasSize(2))
                .body("nombre", hasItems("Estado", "Casos"));
    }

    @Test
    void getMetricasShouldReturnExpectedFields() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/datasets/{id}/metricas", DATASET_ACTIVO_ID)
                .then()
                .statusCode(200)
                .body("[0].id",         notNullValue())
                .body("[0].nombre",     notNullValue())
                .body("[0].columnaCsv", notNullValue());
    }

    @Test
    void getMetricasShouldReturn404ForNonExistentDataset() {
        UUID unknownId = UUID.randomUUID();

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/datasets/{id}/metricas", unknownId)
                .then()
                .statusCode(404)
                .body("message", notNullValue());
    }

    @Test
    void getMetricasShouldReturn404ForInactiveDataset() {
        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .when()
                .get("/datasets/{id}/metricas", DATASET_INACTIVO_ID)
                .then()
                .statusCode(404)
                .body("message", notNullValue());
    }

    // ── POST /datasets/upload ─────────────────────────────────────────────────

    @Test
    void uploadDatasetShouldReturn409WhenTableAlreadyExists() {
        // "diabetes_mexico_2023" ya existe en H2 por el setUp
        String csvBase64 = java.util.Base64.getEncoder()
                .encodeToString("estado,casos\nJalisco,100\n".getBytes());

        String body = """
            {
              "nombre": "Otro dataset",
              "descripcion": "Test",
              "fuente": "Test",
              "archivoNombre": "diabetes_mexico_2023.csv",
              "archivoCsvBase64": "%s",
              "columnas": [
                { "originalName": "estado", "displayName": "Estado", "sqlType": "VARCHAR(255)" },
                { "originalName": "casos",  "displayName": "Casos",  "sqlType": "INT" }
              ]
            }
            """.formatted(csvBase64);

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when()
                .post("/datasets/upload")
                .then()
                .statusCode(409)
                .body("message", containsString("diabetes_mexico_2023"));
    }

    @Test
    void uploadDatasetShouldReturn400WhenColumnasIsEmpty() {
        String csvBase64 = java.util.Base64.getEncoder()
                .encodeToString("col\nval\n".getBytes());

        String body = """
            {
              "nombre": "Test",
              "archivoNombre": "test_vacio.csv",
              "archivoCsvBase64": "%s",
              "columnas": []
            }
            """.formatted(csvBase64);

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when()
                .post("/datasets/upload")
                .then()
                .statusCode(400);
    }
}