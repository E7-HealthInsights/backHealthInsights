package org.acme.interfaces.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
        return Response.ok(user).build();
    }
}
