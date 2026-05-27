package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.DatasetResponseDto;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.dto.UploadDatasetDto;
import org.acme.application.usecase.GetDatasetsUseCase;
import org.acme.application.usecase.GetMetricasByDatasetUseCase;
import org.acme.application.usecase.GetValoresDistintosUseCase;
import org.acme.application.usecase.UploadDatasetUseCase;
import org.acme.domain.exception.TableAlreadyExistsException;
import org.acme.domain.models.Dataset;
import org.acme.domain.repository.DatasetRepository;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/datasets")
@Produces(MediaType.APPLICATION_JSON)
public class DatasetResource {

    private static final Logger LOG = Logger.getLogger(DatasetResource.class);

    @Inject GetDatasetsUseCase getDatasetsUseCase;
    @Inject GetMetricasByDatasetUseCase getMetricasByDatasetUseCase;
    @Inject GetValoresDistintosUseCase getValoresDistintosUseCase;
    @Inject UploadDatasetUseCase uploadDatasetUseCase;
    @Inject DatasetRepository datasetRepository;

    /**
     * GET /datasets
     * Devuelve los datasets en estado READY.
     */
    @GET
    public Response getDatasets() {
        return Response.ok(getDatasetsUseCase.execute()).build();
    }

    /**
     * GET /datasets/{id}/metricas
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
     * GET /datasets/{id}/metricas/{columna}/valores-distintos
     *
     * Devuelve hasta 50 valores únicos de la columna indicada.
     * El front usa esto para decidir si muestra un dropdown (≤ 50 valores)
     * o un input libre (> 50 valores, cubierto enviando todos los que haya).
     *
     * Respuesta: { "valores": ["val1", "val2", ...], "total": 12 }
     */
    @GET
    @Path("/{id}/metricas/{columna}/valores-distintos")
    public Response getValoresDistintos(
            @PathParam("id")      UUID   id,
            @PathParam("columna") String columna) {
        try {
            List<String> valores = getValoresDistintosUseCase.execute(id, columna);
            return Response.ok(Map.of(
                    "valores", valores,
                    "total",   valores.size()
            )).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();
        }
    }
     /*
     * Endpoint de polling para que el frontend sepa en qué estado está
     * el ingest de un dataset recién subido.
     *
     * Respuesta:
     * {
     *   "id":           "uuid",
     *   "estado":       "PENDING" | "PROCESSING" | "READY" | "ERROR",
     *   "errorMensaje": "descripción del error" | null
     * }
     */
    @GET
    @Path("/{id}/status")
    public Response getStatus(@PathParam("id") UUID id) {
        return datasetRepository.findDatasetById(id)
                .map(d -> Response.ok(Map.of(
                        "id",           d.getId().toString(),
                        "estado",       d.getEstado().name(),
                        "errorMensaje", d.getErrorMensaje() != null ? d.getErrorMensaje() : ""
                )).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponseDto("Dataset no encontrado: " + id))
                        .build());
    }

    /**
     * POST /datasets/upload
     *
     * Registra el dataset, sube el CSV a GCS y publica el evento Kafka.
     * Responde 202 Accepted inmediatamente — el ingest ocurre en background.
     *
     * Solo accesible por ADMIN.
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response uploadDataset(@Valid UploadDatasetDto dto) {
        try {
            Dataset created = uploadDatasetUseCase.execute(dto);

            // 202 Accepted: el dataset existe pero su ingest está en cola
            return Response.accepted()
                    .entity(Map.of(
                            "id",      created.getId().toString(),
                            "estado",  created.getEstado().name(),
                            "nombre",  created.getNombre(),
                            "message", "Dataset registrado. El procesamiento del CSV está en curso."
                    ))
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
            LOG.errorf("Error inesperado al registrar dataset: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponseDto(
                            e.getMessage() != null ? e.getMessage() : "Error interno del servidor"))
                    .build();
        }
    }
}
