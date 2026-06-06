package org.acme.interfaces.rest;

import org.acme.application.dto.DashboardStatsResponseDto;
import org.acme.application.usecase.GetDashboardStatsUseCase;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/stats")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {
    @Inject
    GetDashboardStatsUseCase getDashboardStatsUseCase;

    @GET
    @RolesAllowed("ADMIN")
    @Operation(summary = "Stats del dashboard admin",
               description = "Retorna conteos de usuarios y datasets usando stored functions")
    @APIResponse(responseCode = "200",
                 content = @Content(schema = @Schema(implementation = DashboardStatsResponseDto.class)))
    public Response getStats() {
        return Response.ok(getDashboardStatsUseCase.execute()).build();
    }
}
