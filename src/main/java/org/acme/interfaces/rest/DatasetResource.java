package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.dto.UploadDatasetDto;
import org.acme.application.usecase.GetDatasetsUseCase;
import org.acme.application.usecase.GetMetricasByDatasetUseCase;
import org.acme.application.usecase.UploadDatasetUseCase;
import org.acme.domain.exception.TableAlreadyExistsException;
import org.acme.domain.models.Dataset;
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/datasets")
@Produces(MediaType.APPLICATION_JSON)
public class DatasetResource {

    private static final Logger LOG = Logger.getLogger(DatasetResource.class);

    @Inject
    GetDatasetsUseCase getDatasetsUseCase;

    @Inject
    GetMetricasByDatasetUseCase getMetricasByDatasetUseCase;

    @Inject
    UploadDatasetUseCase uploadDatasetUseCase;

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

    /**
     * POST /datasets/upload
     * Recibe metadata + CSV en base64 + definición de columnas como JSON puro.
     * Crea el registro en Dataset, las Métricas y la tabla dinámica en BD.
     * Solo accesible por ADMIN.
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response uploadDataset(@Valid UploadDatasetDto dto) {
        try {
            Dataset created = uploadDatasetUseCase.execute(dto);
            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();

        } catch (TableAlreadyExistsException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();

        } catch (Exception e) {
            LOG.errorf("Error inesperado al procesar dataset: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponseDto(
                            e.getMessage() != null ? e.getMessage() : "Error interno del servidor"))
                    .build();
        }
    }
}