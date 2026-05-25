package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.dto.GuardarProyeccionDto;
import org.acme.application.usecase.ActualizarProyeccionUseCase;
import org.acme.application.usecase.EliminarProyeccionUseCase;
import org.acme.application.usecase.GetProyeccionesUseCase;
import org.acme.application.usecase.GuardarProyeccionUseCase;
import org.acme.application.usecase.SimularProyeccionFinanzasUseCase;
import org.acme.application.usecase.SimularProyeccionGeneralUseCase;
import org.acme.domain.exception.ProyeccionNotFoundException;

import io.quarkus.security.UnauthorizedException;

import java.util.UUID;

@Path("/proyecciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProyeccionResource {

    @Inject GuardarProyeccionUseCase guardarProyeccionUseCase;
    @Inject GetProyeccionesUseCase getProyeccionesUseCase;
    @Inject ActualizarProyeccionUseCase actualizarProyeccionUseCase;
    @Inject EliminarProyeccionUseCase eliminarProyeccionUseCase;
    @Inject SimularProyeccionFinanzasUseCase simularFinanzasUseCase;
    @Inject SimularProyeccionGeneralUseCase simularGeneralUseCase;

    public ProyeccionResource(GuardarProyeccionUseCase guardarProyeccionUseCase,
                              GetProyeccionesUseCase getProyeccionesUseCase,
                              ActualizarProyeccionUseCase actualizarProyeccionUseCase,
                              EliminarProyeccionUseCase eliminarProyeccionUseCase,
                            SimularProyeccionFinanzasUseCase simularFinanzasUseCase, SimularProyeccionGeneralUseCase simularGeneralUseCase) {
        this.guardarProyeccionUseCase = guardarProyeccionUseCase;
        this.getProyeccionesUseCase = getProyeccionesUseCase;
        this.actualizarProyeccionUseCase = actualizarProyeccionUseCase;
        this.eliminarProyeccionUseCase = eliminarProyeccionUseCase;
        this.simularFinanzasUseCase = simularFinanzasUseCase;
        this.simularGeneralUseCase = simularGeneralUseCase;
    }

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

    @PATCH
    @Path("/{id}")
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    public Response actualizar(@PathParam("id") UUID id,
                            @Valid GuardarProyeccionDto dto) {
        try {
            return Response.ok(actualizarProyeccionUseCase.execute(id, dto)).build();
        } catch (ProyeccionNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponseDto(e.getMessage())).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponseDto(e.getMessage())).build();
        }
    }

    // Elimina una proyección guardada
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    public Response eliminar(@PathParam("id") UUID id) {
        try {
            eliminarProyeccionUseCase.execute(id);
            return Response.noContent().build();
        } catch (ProyeccionNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponseDto(e.getMessage())).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponseDto(e.getMessage())).build();
        }
    }

    @GET
    @Path("/simular/finanzas")
    @RolesAllowed({"DIRECTOR_FINANZAS"})
    public Response simularFinanzas(
            @QueryParam("presupuesto")   Double presupuesto,
            @QueryParam("nutricion")     Double nutricion,
            @QueryParam("medicamentos")  Double medicamentos,
            @QueryParam("deteccion")     Double deteccion,
            @QueryParam("atencion")      Double atencion,
            @QueryParam("hasta")         Integer hasta
    ) {
        // Validaciones básicas
        if (presupuesto == null || presupuesto <= 0)
            return Response.status(400)
                    .entity(new ErrorResponseDto("El presupuesto es obligatorio")).build();

        if (hasta == null || hasta <= 2025 || hasta > 2050)
            return Response.status(400)
                    .entity(new ErrorResponseDto("El período debe estar entre 2026 y 2050")).build();

        double n = nutricion != null ? nutricion : 25.0;
        double m = medicamentos != null ? medicamentos : 25.0;
        double d = deteccion != null ? deteccion : 25.0;
        double a = atencion != null ? atencion : 25.0;

        return Response.ok(
            simularFinanzasUseCase.execute(presupuesto, n, m, d, a, hasta)
        ).build();
    }

    @GET
    @Path("/simular/general")
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    public Response simularGeneral(
            @QueryParam("tasaCrecimiento")    @DefaultValue("2.1")  Double tasaCrecimiento,
            @QueryParam("intensidadPolitica") @DefaultValue("0")    Double intensidadPolitica,
            @QueryParam("inicio")             @DefaultValue("2025") Integer inicio,
            @QueryParam("hasta")              @DefaultValue("2050") Integer hasta
    ) {
        if (hasta <= inicio || hasta > 2050)
            return Response.status(400)
                    .entity(new ErrorResponseDto("Período inválido")).build();

        if (intensidadPolitica < 0 || intensidadPolitica > 100)
            return Response.status(400)
                    .entity(new ErrorResponseDto("La intensidad debe estar entre 0 y 100")).build();

        return Response.ok(
            simularGeneralUseCase.execute(tasaCrecimiento, intensidadPolitica, inicio, hasta)
        ).build();
    }
}