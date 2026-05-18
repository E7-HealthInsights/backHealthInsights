package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.dto.GuardarProyeccionDto;
import org.acme.application.usecase.GetProyeccionesUseCase;
import org.acme.application.usecase.GuardarProyeccionUseCase;

import java.util.UUID;

@Path("/proyecciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProyeccionResource {

    @Inject GuardarProyeccionUseCase guardarProyeccionUseCase;
    @Inject GetProyeccionesUseCase getProyeccionesUseCase;

    // Guarda una proyección calculada por el frontend
    @POST
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    public Response guardar(@Valid GuardarProyeccionDto dto) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(guardarProyeccionUseCase.execute(dto))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponseDto("Error guardando proyección"))
                    .build();
        }
    }

    // Lista las proyecciones guardadas del usuario autenticado
    @GET
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    public Response listar() {
        return Response.ok(getProyeccionesUseCase.execute()).build();
    }

    // Elimina una proyección guardada
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    public Response eliminar(@PathParam("id") UUID id) {
        try {
            // TODO: agregar verificación de ownership
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponseDto("Error eliminando proyección"))
                    .build();
        }
    }
}