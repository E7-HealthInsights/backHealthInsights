package org.acme.interfaces.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.CreateUserDto;
import org.acme.application.usecase.CreateUserUseCase;
import org.acme.infrastructure.security.AuthContext;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    CreateUserUseCase createUserUseCase;
    AuthContext authContext;

    public UserResource(CreateUserUseCase createUserUseCase, AuthContext authContext) {
        this.createUserUseCase = createUserUseCase;
        this.authContext = authContext;
    }

    @POST
    public Response createUser(@Valid CreateUserDto createUserDto){
        try{
            return Response.ok(createUserUseCase.execute(createUserDto)).build();
        } catch (Exception e){
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}