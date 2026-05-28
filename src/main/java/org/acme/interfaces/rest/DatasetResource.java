package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.DatasetResponseDto;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.dto.MetricaResponseDto;
import org.acme.application.dto.UploadDatasetDto;
import org.acme.application.usecase.GetDatasetsUseCase;
import org.acme.application.usecase.GetMetricasByDatasetUseCase;
import org.acme.application.usecase.GetValoresDistintosUseCase;
import org.acme.application.usecase.UploadDatasetUseCase;
import org.acme.domain.exception.TableAlreadyExistsException;
import org.acme.domain.models.Dataset;
import org.acme.domain.repository.DatasetRepository;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/datasets")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Datasets", description = "Gestión y consulta de datasets del sistema")
@SecurityRequirement(name = "bearerAuth")
public class DatasetResource {

    private static final Logger LOG = Logger.getLogger(DatasetResource.class);

    @Inject GetDatasetsUseCase getDatasetsUseCase;
    @Inject GetMetricasByDatasetUseCase getMetricasByDatasetUseCase;
    @Inject GetValoresDistintosUseCase getValoresDistintosUseCase;
    @Inject UploadDatasetUseCase uploadDatasetUseCase;
    @Inject DatasetRepository datasetRepository;

    @GET
    @Operation(
        summary     = "Listar datasets disponibles",
        description = "Devuelve todos los datasets en estado READY disponibles para consulta."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de datasets disponibles",
            content = @Content(schema = @Schema(implementation = DatasetResponseDto.class)))
    })
    public Response getDatasets() {
        return Response.ok(getDatasetsUseCase.execute()).build();
    }

    @GET
    @Path("/{id}/metricas")
    @Operation(
        summary     = "Obtener métricas de un dataset",
        description = "Devuelve las métricas (columnas configuradas) de un dataset específico."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de métricas del dataset",
            content = @Content(schema = @Schema(implementation = MetricaResponseDto.class))),
        @APIResponse(responseCode = "404", description = "Dataset no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response getMetricas(
        @Parameter(description = "UUID del dataset", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id) {
        try {
            return Response.ok(getMetricasByDatasetUseCase.execute(id)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/metricas/{columna}/valores-distintos")
    @Operation(
        summary     = "Obtener valores distintos de una columna",
        description = "Devuelve hasta 50 valores únicos de la columna indicada. El frontend usa esto para decidir si muestra un dropdown (≤50 valores) o un input libre (>50 valores). Respuesta: { valores: string[], total: number }."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de valores distintos con su total"),
        @APIResponse(responseCode = "400", description = "Columna inválida o no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "404", description = "Dataset no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response getValoresDistintos(
        @Parameter(description = "UUID del dataset", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id,
        @Parameter(description = "Nombre de la columna CSV", in = ParameterIn.PATH, required = true)
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

    @GET
    @Path("/{id}/status")
    @Operation(
        summary     = "Consultar estado de procesamiento del dataset",
        description = "Endpoint de polling para conocer el estado del ingest de un dataset. Estados posibles: PENDING (registrado), PROCESSING (procesando CSV), READY (listo), ERROR (falló el ingest). Respuesta: { id, estado, errorMensaje }."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Estado actual del dataset"),
        @APIResponse(responseCode = "404", description = "Dataset no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response getStatus(
        @Parameter(description = "UUID del dataset", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id) {
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

    @POST
    @Path("/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    @Operation(
        summary     = "Subir dataset",
        description = "Registra un nuevo dataset, sube el CSV (enviado como Base64 en el campo archivoCsvBase64 del JSON) a GCS y publica el evento de ingest a Kafka. Responde 202 Accepted inmediatamente — el procesamiento ocurre en background. Usar GET /{id}/status para hacer polling del estado. Requiere JWT de Firebase. Solo ADMIN."
    )
    @APIResponses({
        @APIResponse(responseCode = "202", description = "Dataset registrado, procesamiento en curso. Devuelve: { id, estado, nombre, message }"),
        @APIResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo ADMIN"),
        @APIResponse(responseCode = "409", description = "Ya existe un dataset con ese nombre",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response uploadDataset(@Valid UploadDatasetDto dto) {
        try {
            Dataset created = uploadDatasetUseCase.execute(dto);

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
