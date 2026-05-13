package org.acme.interfaces.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.acme.infrastructure.query.QueryExecutor;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

@QuarkusTest
public class WidgetResourceTest {

    @InjectMock
    QueryExecutor queryExecutor;

    @Test
    void postWidgetShouldReturn403WhenUserIsAdmin() {
        String body = """
            {
                "titulo": "Widget ADMIN",
                "tipoId": 1,
                "queryConfig": "{\\"tabla\\":\\"imss_deteccion_diabetes\\",\\"funcion\\":\\"SUM\\",\\"columna\\":\\"detecciones\\"}",
                "orden": 1
            }
            """;

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer fake-token")
                .body(body)
                .when().post("/widgets")
                .then()
                .statusCode(403);
    }

    @Test
    void getWidgetsShouldReturn403WhenUserIsAdmin() {
        given()
                .header("Authorization", "Bearer fake-token")
                .when().get("/widgets")
                .then()
                .statusCode(403);
    }
}