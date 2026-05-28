package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.LogActividadResponseDto;
import org.acme.application.dto.PaginadoResponseDto;
import org.acme.application.usecase.GetLogActividadUseCase;
import org.acme.domain.models.LogActividad;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/actividad")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Log de Actividad", description = "Registro paginado de acciones administrativas del sistema")
@SecurityRequirement(name = "bearerAuth")
public class LogActividadResource {

    private final GetLogActividadUseCase getLogActividadUseCase;

    @Inject
    public LogActividadResource(GetLogActividadUseCase getLogActividadUseCase) {
        this.getLogActividadUseCase = getLogActividadUseCase;
    }

    @GET
    @RolesAllowed("ADMIN")
    @Operation(
        summary     = "Listar log de actividad paginado",
        description = "Devuelve el historial de acciones administrativas con paginación y búsqueda por nombre de administrador. Requiere JWT de Firebase. Solo ADMIN."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista paginada de entradas de actividad"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo ADMIN")
    })
    public Response getAll(
        @Parameter(description = "Número de página (mínimo 1)", in = ParameterIn.QUERY, example = "1")
        @QueryParam("page") @DefaultValue("1") int page,

        @Parameter(description = "Tamaño de página (1-100)", in = ParameterIn.QUERY, example = "10")
        @QueryParam("size") @DefaultValue("10") int size,

        @Parameter(description = "Búsqueda por nombre de administrador", in = ParameterIn.QUERY, example = "Juan")
        @QueryParam("search") @DefaultValue("") String search
    ) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        PaginadoResponseDto<LogActividad> paginado = getLogActividadUseCase.execute(page, size, search.trim());

        List<LogActividadResponseDto> dtos = paginado.getData()
                .stream()
                .map(log -> {
                    LogActividadResponseDto dto = new LogActividadResponseDto();
                    dto.setId(log.getId());
                    dto.setAdminNombre(log.getAdminNombre());
                    dto.setAccion(log.getAccion());
                    dto.setDetalle(log.getDetalle());
                    dto.setEntidadTipo(log.getEntidadTipo());
                    dto.setEntidadId(log.getEntidadId());
                    dto.setFecha(log.getFecha());
                    return dto;
                })
                .toList();

        PaginadoResponseDto<LogActividadResponseDto> response = new PaginadoResponseDto<>(
                dtos, paginado.getTotalElementos(), page, size);

        return Response.ok(response).build();
    }
}
