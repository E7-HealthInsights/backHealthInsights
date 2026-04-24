package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.CreateUserDto;
import org.acme.application.dto.UserResponseDto;
import org.acme.application.usecase.CreateUserUseCase;
import org.acme.application.usecase.GetUsersUseCase;
import org.acme.domain.models.User;
import org.acme.infrastructure.security.AuthContext;
import org.acme.domain.exception.EmailAlreadyExistsException;
import org.acme.domain.exception.RoleNotFoundException;
import org.acme.application.dto.ErrorResponseDto;

import java.util.ArrayList;
import java.util.List;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    CreateUserUseCase createUserUseCase;
    AuthContext authContext;
    GetUsersUseCase getUsersUseCase;

    public UserResource(CreateUserUseCase createUserUseCase, AuthContext authContext, GetUsersUseCase getUsersUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.authContext = authContext;
        this.getUsersUseCase = getUsersUseCase;
    }

    @POST
    @RolesAllowed("ADMIN")
    public Response createUser(@Valid CreateUserDto createUserDto) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(createUserUseCase.execute(createUserDto))
                    .build();

        } catch (EmailAlreadyExistsException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();

        } catch (RoleNotFoundException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponseDto("Error interno del servidor"))
                    .build();
        }
    }

    @GET
    @RolesAllowed("ADMIN")
    public Response listUsers(){
        ArrayList<User> users = getUsersUseCase.execute();

        List<UserResponseDto> response = users.stream().map(user -> {
            UserResponseDto dto = new UserResponseDto();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole().getName());
            dto.setStatus(user.isStatus());
            return dto;
        }).toList();

        return Response.ok(response).build();
    }

}