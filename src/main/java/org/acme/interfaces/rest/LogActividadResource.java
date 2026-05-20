package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.LogActividadResponseDto;
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
    public Response getAll() {
        List<LogActividadResponseDto> response = getLogActividadUseCase.execute()
                .stream()
                .map(log -> {
                    LogActividadResponseDto dto = new LogActividadResponseDto();
                    dto.setId(log.getId());
                    dto.setUsuarioId(log.getUsuarioId());
                    dto.setAccion(log.getAccion());
                    dto.setDetalle(log.getDetalle());
                    dto.setEntidadTipo(log.getEntidadTipo());
                    dto.setEntidadId(log.getEntidadId());
                    dto.setFecha(log.getFecha());
                    return dto;
                })
                .toList();
        return Response.ok(response).build();
    }
}
