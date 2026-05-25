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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/marketing/strategies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MarketingStrategyResource {

    @Inject GenerateMarketingStrategyUseCase generateUseCase;
    @Inject GetMarketingStrategiesUseCase listUseCase;
    @Inject GetMarketingStrategyByIdUseCase getByIdUseCase;
    @Inject UpdateMarketingStrategyStateUseCase updateStateUseCase;
    @Inject AddMarketingStrategyCommentUseCase addCommentUseCase;

    @POST
    @RolesAllowed({"DIRECTOR_MERCADOTECNIA"})
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
    public Response list() {
        List<MarketingStrategyDto> strategies = listUseCase.execute();
        return Response.ok(strategies).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"DIRECTOR_MERCADOTECNIA"})
    public Response getById(@PathParam("id") UUID id) {
        MarketingStrategyDto dto = getByIdUseCase.execute(id);
        return Response.ok(dto).build();
    }

    @PATCH
    @Path("/{id}/estado")
    @RolesAllowed({"DIRECTOR_MERCADOTECNIA"})
    public Response updateEstado(@PathParam("id") UUID id, @Valid UpdateStrategyStateDto request) {
        MarketingStrategyDto dto = updateStateUseCase.execute(id, request);
        return Response.ok(dto).build();
    }

    @POST
    @Path("/{id}/comentarios")
    @RolesAllowed({"DIRECTOR_MERCADOTECNIA"})
    public Response addComentario(@PathParam("id") UUID id, @Valid AddCommentDto request) {
        MarketingStrategyDto dto = addCommentUseCase.execute(id, request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }
}
