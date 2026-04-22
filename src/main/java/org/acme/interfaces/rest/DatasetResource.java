package org.acme.interfaces.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.usecase.GetDatasetsUseCase;
import org.acme.application.usecase.GetMetricasByDatasetUseCase;

import java.util.UUID;

@Path("/datasets")
@Produces(MediaType.APPLICATION_JSON)
public class DatasetResource {

    @Inject
    GetDatasetsUseCase getDatasetsUseCase;

    @Inject
    GetMetricasByDatasetUseCase getMetricasByDatasetUseCase;

    /**
     * GET /datasets
     * Devuelve todos los datasets activos disponibles para el usuario autenticado.
     */
    @GET
    public Response getDatasets() {
        return Response.ok(getDatasetsUseCase.execute()).build();
    }

    /**
     * GET /datasets/{id}/metricas
     * Devuelve las métricas (columnas disponibles) de un dataset específico.
     */
    @GET
    @Path("/{id}/metricas")
    public Response getMetricas(@PathParam("id") UUID id) {
        try {
            return Response.ok(getMetricasByDatasetUseCase.execute(id)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();
        }
    }
}