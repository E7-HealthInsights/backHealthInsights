package org.acme.interfaces.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.UserResponseDto;
import org.acme.domain.models.User;
import org.acme.infrastructure.security.AuthContext;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthContext authContext;

    @GET
    @Path("/me")
    public Response me() {
        User user = authContext.getUser();

        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getName()); // "ADMIN", "DIRECTOR_FINANZAS", etc
        dto.setStatus(user.isStatus());

        return Response.ok(dto).build();
    }
}
