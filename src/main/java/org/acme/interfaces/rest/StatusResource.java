package org.acme.interfaces.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.util.Map;

@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Estado", description = "Health check público de la aplicación")
public class StatusResource {

    @ConfigProperty(name = "app.name", defaultValue = "health-insights")
    String appName;

    @ConfigProperty(name = "app.version", defaultValue = "dev")
    String appVersion;

    @GET
    @Operation(
        summary     = "Estado de la aplicación",
        description = "Endpoint público que devuelve el estado operativo, nombre y versión de la aplicación. No requiere autenticación."
    )
    @APIResponse(responseCode = "200", description = "Aplicación en funcionamiento. Devuelve: status, name, version, timestamp")
    public Response status() {
        return Response.ok(Map.of(
                "status", "UP",
                "name", appName,
                "version", appVersion,
                "timestamp", Instant.now().toString()
        )).build();
    }
}
