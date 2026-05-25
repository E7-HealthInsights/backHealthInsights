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

import java.util.List;

@Path("/actividad")
@Produces(MediaType.APPLICATION_JSON)
public class LogActividadResource {

    private final GetLogActividadUseCase getLogActividadUseCase;

    @Inject
    public LogActividadResource(GetLogActividadUseCase getLogActividadUseCase) {
        this.getLogActividadUseCase = getLogActividadUseCase;
    }

    @GET
    @RolesAllowed("ADMIN")
    public Response getAll(
        @QueryParam("page") @DefaultValue("1")  int page,
        @QueryParam("size") @DefaultValue("10") int size,
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
