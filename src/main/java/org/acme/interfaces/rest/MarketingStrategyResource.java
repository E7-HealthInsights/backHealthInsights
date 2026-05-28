package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.AddCommentDto;
import org.acme.application.dto.GenerateStrategyRequestDto;
import org.acme.application.dto.MarketingStrategyDto;
import org.acme.application.dto.UpdateStrategyStateDto;
import org.acme.application.usecase.AddMarketingStrategyCommentUseCase;
import org.acme.application.usecase.GenerateMarketingStrategyUseCase;
import org.acme.application.usecase.GetMarketingStrategiesUseCase;
import org.acme.application.usecase.GetMarketingStrategyByIdUseCase;
import org.acme.application.usecase.UpdateMarketingStrategyStateUseCase;
import org.acme.infrastructure.openai.OpenAIInvalidResponseException;
import org.acme.infrastructure.openai.OpenAINotConfiguredException;
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
import java.util.Map;
import java.util.UUID;

@Path("/marketing/strategies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Estrategias de Mercadotecnia", description = "Generación y gestión de estrategias de marketing con IA (OpenAI)")
@SecurityRequirement(name = "bearerAuth")
public class MarketingStrategyResource {

    @Inject GenerateMarketingStrategyUseCase generateUseCase;
    @Inject GetMarketingStrategiesUseCase listUseCase;
    @Inject GetMarketingStrategyByIdUseCase getByIdUseCase;
    @Inject UpdateMarketingStrategyStateUseCase updateStateUseCase;
    @Inject AddMarketingStrategyCommentUseCase addCommentUseCase;

    @POST
    @RolesAllowed({"DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Generar estrategia de marketing",
        description = "Genera una estrategia de marketing personalizada consultando la API de OpenAI de forma síncrona. La respuesta puede tardar varios segundos. Requiere JWT de Firebase. Solo DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Estrategia generada y guardada exitosamente",
            content = @Content(schema = @Schema(implementation = MarketingStrategyDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo DIRECTOR_MERCADOTECNIA"),
        @APIResponse(responseCode = "502", description = "La IA devolvió una respuesta inválida o inesperada"),
        @APIResponse(responseCode = "503", description = "OpenAI no está configurado en el servidor")
    })
    public Response generate(GenerateStrategyRequestDto request) {
        try {
            MarketingStrategyDto dto = generateUseCase.execute(request);
            return Response.status(Response.Status.CREATED).entity(dto).build();
        } catch (OpenAINotConfiguredException e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of(
                            "error", "OPENAI_NOT_CONFIGURED",
                            "message", e.getMessage()
                    )).build();
        } catch (OpenAIInvalidResponseException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(Map.of(
                            "error", "AI_INVALID_RESPONSE",
                            "message", e.getMessage()
                    )).build();
        }
    }

    @GET
    @RolesAllowed({"DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Listar estrategias",
        description = "Devuelve todas las estrategias de marketing del usuario autenticado. Requiere JWT de Firebase. Solo DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de estrategias",
            content = @Content(schema = @Schema(implementation = MarketingStrategyDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo DIRECTOR_MERCADOTECNIA")
    })
    public Response list() {
        List<MarketingStrategyDto> strategies = listUseCase.execute();
        return Response.ok(strategies).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Obtener estrategia por ID",
        description = "Devuelve el detalle completo de una estrategia de marketing por su UUID. Requiere JWT de Firebase. Solo DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Estrategia encontrada",
            content = @Content(schema = @Schema(implementation = MarketingStrategyDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo DIRECTOR_MERCADOTECNIA"),
        @APIResponse(responseCode = "404", description = "Estrategia no encontrada")
    })
    public Response getById(
        @Parameter(description = "UUID de la estrategia", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id) {
        MarketingStrategyDto dto = getByIdUseCase.execute(id);
        return Response.ok(dto).build();
    }

    @PATCH
    @Path("/{id}/estado")
    @RolesAllowed({"DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Actualizar estado de una estrategia",
        description = "Cambia el estado de una estrategia. Valores válidos del campo estado: propuesta, ejecutada, descartada. Requiere JWT de Firebase. Solo DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Estado actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = MarketingStrategyDto.class))),
        @APIResponse(responseCode = "400", description = "Estado inválido (valores permitidos: propuesta, ejecutada, descartada)"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo DIRECTOR_MERCADOTECNIA"),
        @APIResponse(responseCode = "404", description = "Estrategia no encontrada")
    })
    public Response updateEstado(
        @Parameter(description = "UUID de la estrategia", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id,
        @Valid UpdateStrategyStateDto request) {
        MarketingStrategyDto dto = updateStateUseCase.execute(id, request);
        return Response.ok(dto).build();
    }

    @POST
    @Path("/{id}/comentarios")
    @RolesAllowed({"DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Agregar comentario a una estrategia",
        description = "Agrega un comentario de texto a una estrategia existente. Devuelve la estrategia actualizada con el comentario incluido. Requiere JWT de Firebase. Solo DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Comentario agregado, devuelve la estrategia actualizada",
            content = @Content(schema = @Schema(implementation = MarketingStrategyDto.class))),
        @APIResponse(responseCode = "400", description = "Contenido del comentario inválido o vacío"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo DIRECTOR_MERCADOTECNIA"),
        @APIResponse(responseCode = "404", description = "Estrategia no encontrada")
    })
    public Response addComentario(
        @Parameter(description = "UUID de la estrategia", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id,
        @Valid AddCommentDto request) {
        MarketingStrategyDto dto = addCommentUseCase.execute(id, request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }
}
