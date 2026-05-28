package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.dto.GuardarProyeccionDto;
import org.acme.application.dto.ProyeccionResponseDto;
import org.acme.application.dto.SimularProyeccionFinanzasResponseDto;
import org.acme.application.dto.SimularProyeccionGeneralResponseDto;
import org.acme.application.usecase.ActualizarProyeccionUseCase;
import org.acme.application.usecase.EliminarProyeccionUseCase;
import org.acme.application.usecase.GetProyeccionesUseCase;
import org.acme.application.usecase.GuardarProyeccionUseCase;
import org.acme.application.usecase.SimularProyeccionFinanzasUseCase;
import org.acme.application.usecase.SimularProyeccionGeneralUseCase;
import org.acme.domain.exception.ProyeccionNotFoundException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.UnauthorizedException;

import java.util.UUID;

@Path("/proyecciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Proyecciones", description = "Gestión y simulación de proyecciones epidemiológicas")
@SecurityRequirement(name = "bearerAuth")
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
                              SimularProyeccionFinanzasUseCase simularFinanzasUseCase,
                              SimularProyeccionGeneralUseCase simularGeneralUseCase) {
        this.guardarProyeccionUseCase = guardarProyeccionUseCase;
        this.getProyeccionesUseCase = getProyeccionesUseCase;
        this.actualizarProyeccionUseCase = actualizarProyeccionUseCase;
        this.eliminarProyeccionUseCase = eliminarProyeccionUseCase;
        this.simularFinanzasUseCase = simularFinanzasUseCase;
        this.simularGeneralUseCase = simularGeneralUseCase;
    }

    @POST
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Guardar proyección",
        description = "Persiste una proyección calculada por el frontend. Requiere JWT de Firebase. Roles: DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Proyección guardada exitosamente",
            content = @Content(schema = @Schema(implementation = ProyeccionResponseDto.class))),
        @APIResponse(responseCode = "400", description = "Datos inválidos (titulo requerido)"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Rol no permitido"),
        @APIResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
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

    @GET
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Listar proyecciones guardadas",
        description = "Devuelve todas las proyecciones guardadas del usuario autenticado. Requiere JWT de Firebase. Roles: DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de proyecciones del usuario",
            content = @Content(schema = @Schema(implementation = ProyeccionResponseDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Rol no permitido")
    })
    public Response listar() {
        return Response.ok(getProyeccionesUseCase.execute()).build();
    }

    @PATCH
    @Path("/{id}")
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Actualizar proyección",
        description = "Actualiza los datos de una proyección guardada. Solo el creador puede modificarla. Requiere JWT de Firebase. Roles: DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Proyección actualizada exitosamente",
            content = @Content(schema = @Schema(implementation = ProyeccionResponseDto.class))),
        @APIResponse(responseCode = "400", description = "Datos inválidos"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "No es el creador de la proyección",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "404", description = "Proyección no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response actualizar(
        @Parameter(description = "UUID de la proyección", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id,
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

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Eliminar proyección",
        description = "Elimina una proyección guardada. Solo el creador puede eliminarla. Requiere JWT de Firebase. Roles: DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Proyección eliminada exitosamente"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "No es el creador de la proyección",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "404", description = "Proyección no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response eliminar(
        @Parameter(description = "UUID de la proyección", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id) {
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
    @Operation(
        summary     = "Simular proyección financiera",
        description = "Calcula una proyección financiera con intervención según la distribución del presupuesto en pilares de salud. Los datos no se persisten. presupuesto y hasta son obligatorios; el resto se distribuye equitativamente (25% c/u) si no se indica. Requiere JWT de Firebase. Solo DIRECTOR_FINANZAS."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Simulación calculada exitosamente",
            content = @Content(schema = @Schema(implementation = SimularProyeccionFinanzasResponseDto.class))),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos (presupuesto requerido, hasta entre 2026 y 2050)",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo DIRECTOR_FINANZAS")
    })
    public Response simularFinanzas(
        @Parameter(description = "Presupuesto total en MXN (requerido, > 0)", in = ParameterIn.QUERY, required = true, example = "5000000")
        @QueryParam("presupuesto") Double presupuesto,

        @Parameter(description = "% asignado a nutrición (opcional, default 25)", in = ParameterIn.QUERY, example = "30")
        @QueryParam("nutricion") Double nutricion,

        @Parameter(description = "% asignado a medicamentos (opcional, default 25)", in = ParameterIn.QUERY, example = "25")
        @QueryParam("medicamentos") Double medicamentos,

        @Parameter(description = "% asignado a detección temprana (opcional, default 25)", in = ParameterIn.QUERY, example = "25")
        @QueryParam("deteccion") Double deteccion,

        @Parameter(description = "% asignado a atención médica (opcional, default 25)", in = ParameterIn.QUERY, example = "20")
        @QueryParam("atencion") Double atencion,

        @Parameter(description = "Año final de la proyección (requerido, 2026-2050)", in = ParameterIn.QUERY, required = true, example = "2040")
        @QueryParam("hasta") Integer hasta
    ) {
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
    @Operation(
        summary     = "Simular proyección general",
        description = "Calcula la proyección general de casos con y sin intervención de política pública. Los datos no se persisten. Requiere JWT de Firebase. Roles: DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Simulación calculada exitosamente",
            content = @Content(schema = @Schema(implementation = SimularProyeccionGeneralResponseDto.class))),
        @APIResponse(responseCode = "400", description = "Parámetros inválidos (período o intensidad fuera de rango)",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Rol no permitido")
    })
    public Response simularGeneral(
        @Parameter(description = "Tasa de crecimiento anual en % (default 2.1)", in = ParameterIn.QUERY, example = "2.1")
        @QueryParam("tasaCrecimiento") @DefaultValue("2.1") Double tasaCrecimiento,

        @Parameter(description = "Intensidad de la política pública 0-100 (default 0 = sin intervención)", in = ParameterIn.QUERY, example = "50")
        @QueryParam("intensidadPolitica") @DefaultValue("0") Double intensidadPolitica,

        @Parameter(description = "Año de inicio de la proyección (default 2025)", in = ParameterIn.QUERY, example = "2025")
        @QueryParam("inicio") @DefaultValue("2025") Integer inicio,

        @Parameter(description = "Año final de la proyección (default 2050, máx 2050)", in = ParameterIn.QUERY, example = "2050")
        @QueryParam("hasta") @DefaultValue("2050") Integer hasta
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
