package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.UserResponseDto;
import org.acme.domain.models.User;
import org.acme.infrastructure.security.AuthContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Autenticación", description = "Información del usuario autenticado vía Firebase")
@SecurityRequirement(name = "bearerAuth")
public class AuthResource {

    @Inject
    AuthContext authContext;

    @GET
    @Path("/me")
    @RolesAllowed({"ADMIN", "DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Obtener usuario autenticado",
        description = "Devuelve los datos del usuario asociado al JWT de Firebase enviado en el header Authorization. Accesible por todos los roles."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Datos del usuario autenticado",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación o token inválido"),
        @APIResponse(responseCode = "403", description = "Rol no permitido")
    })
    public Response me() {
        User user = authContext.getUser();

        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getName());
        dto.setStatus(user.isStatus());

        return Response.ok(dto).build();
    }
}
