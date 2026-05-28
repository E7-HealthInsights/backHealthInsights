package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.CreateReporteDto;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.dto.ReporteResponseDto;
import org.acme.application.usecase.CreateReporteUseCase;
import org.acme.application.usecase.DeleteReporteUseCase;
import org.acme.application.usecase.GetReportesUseCase;
import org.acme.domain.exception.ReporteNotFoundException;
import org.acme.domain.exception.UnauthorizedException;
import org.acme.domain.models.Reporte;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/reportes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Reportes", description = "Gestión de reportes vinculados a proyecciones, dashboards y actividades")
@SecurityRequirement(name = "bearerAuth")
public class ReporteResource {

    private final CreateReporteUseCase createReporteUseCase;
    private final GetReportesUseCase getReportesUseCase;
    private final DeleteReporteUseCase deleteReporteUseCase;

    @Inject
    public ReporteResource(CreateReporteUseCase createReporteUseCase,
                           GetReportesUseCase getReportesUseCase,
                           DeleteReporteUseCase deleteReporteUseCase) {
        this.createReporteUseCase = createReporteUseCase;
        this.getReportesUseCase   = getReportesUseCase;
        this.deleteReporteUseCase = deleteReporteUseCase;
    }

    @POST
    @RolesAllowed({"ADMIN", "DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Crear reporte",
        description = "Registra un nuevo reporte asociado a una entidad del sistema. El campo tipo acepta: DASHBOARD, PROYECCION, ACTIVIDAD. referenciaId es el UUID de la entidad referenciada (opcional). Requiere JWT de Firebase."
    )
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Reporte creado exitosamente",
            content = @Content(schema = @Schema(implementation = ReporteResponseDto.class))),
        @APIResponse(responseCode = "400", description = "Datos inválidos (titulo o tipo ausente)"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Rol no permitido"),
        @APIResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response create(@Valid CreateReporteDto dto) {
        try {
            Reporte reporte = createReporteUseCase.execute(dto);
            return Response.status(Response.Status.CREATED)
                    .entity(toDto(reporte))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponseDto("Error interno del servidor"))
                    .build();
        }
    }

    @GET
    @RolesAllowed({"ADMIN", "DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Listar reportes",
        description = "Devuelve todos los reportes del usuario autenticado. Requiere JWT de Firebase."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de reportes del usuario",
            content = @Content(schema = @Schema(implementation = ReporteResponseDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Rol no permitido")
    })
    public Response getAll() {
        List<ReporteResponseDto> response = getReportesUseCase.execute()
                .stream()
                .map(this::toDto)
                .toList();
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Eliminar reporte",
        description = "Elimina un reporte por su UUID. Solo el creador del reporte puede eliminarlo. Requiere JWT de Firebase."
    )
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Reporte eliminado exitosamente"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "No es el creador del reporte",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "404", description = "Reporte no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response delete(
        @Parameter(description = "UUID del reporte", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id) {
        try {
            deleteReporteUseCase.execute(id);
            return Response.noContent().build();

        } catch (ReporteNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();

        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponseDto("Error interno del servidor"))
                    .build();
        }
    }

    private ReporteResponseDto toDto(Reporte reporte) {
        ReporteResponseDto dto = new ReporteResponseDto();
        dto.setId(reporte.getId());
        dto.setTitulo(reporte.getTitulo());
        dto.setTipo(reporte.getTipo());
        dto.setReferenciaId(reporte.getReferenciaId());
        dto.setFechaCreacion(reporte.getFechaCreacion());
        return dto;
    }
}
