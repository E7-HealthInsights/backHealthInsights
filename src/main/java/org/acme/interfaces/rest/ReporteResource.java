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

import java.util.List;
import java.util.UUID;

@Path("/reportes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
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
    public Response delete(@PathParam("id") UUID id) {
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
