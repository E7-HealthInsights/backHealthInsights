package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.CreateUserDto;
import org.acme.application.dto.DeactivateUserDto;
import org.acme.application.dto.UpdateUserDto;
import org.acme.application.dto.UserResponseDto;
import org.acme.application.usecase.CreateUserUseCase;
import org.acme.application.usecase.DeactivateUserUseCase;
import org.acme.application.usecase.GetUsersUseCase;
import org.acme.application.usecase.UpdateUserUseCase;
import org.acme.domain.models.User;
import org.acme.infrastructure.security.AuthContext;
import org.acme.domain.exception.EmailAlreadyExistsException;
import org.acme.domain.exception.RoleNotFoundException;
import org.acme.domain.exception.UserNotFoundException;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.dto.PaginadoResponseDto;

import java.util.UUID;

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
    UpdateUserUseCase updateUserUseCase;
    DeactivateUserUseCase deactivateUserUseCase;

    public UserResource(CreateUserUseCase createUserUseCase, AuthContext authContext, GetUsersUseCase getUsersUseCase, UpdateUserUseCase updateUserUseCase, DeactivateUserUseCase deactivateUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.authContext = authContext;
        this.getUsersUseCase = getUsersUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deactivateUserUseCase = deactivateUserUseCase;
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

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response updateUser(@PathParam("id") UUID id, @Valid UpdateUserDto updateUserDto) {
        try {
            User user = updateUserUseCase.execute(id, updateUserDto);
            UserResponseDto dto = new UserResponseDto();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole().getName());
            dto.setStatus(user.isStatus());
            dto.setModifiedBy(user.getModifiedBy());
            return Response.ok(dto).build();

        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
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

    @PATCH
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deactivateUser(@PathParam("id") UUID id, DeactivateUserDto deactivateUserDto) {
        try {
            deactivateUserUseCase.execute(id, deactivateUserDto);
            return Response.noContent().build();

        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
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
    public Response listUsers(
        @QueryParam("page")   @DefaultValue("1")     int     page,
        @QueryParam("size")   @DefaultValue("10")    int     size,
        @QueryParam("search") @DefaultValue("")      String  search,
        @QueryParam("status") @DefaultValue("true")  boolean status
    ){

        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;

        PaginadoResponseDto<User> paginado =
                getUsersUseCase.execute(page, size, search.trim(), status);

        List<UserResponseDto> dtos = paginado.getData().stream().map(user -> {
            UserResponseDto dto = new UserResponseDto();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole().getName());
            dto.setStatus(user.isStatus());
            dto.setModifiedBy(user.getModifiedBy());
            return dto;
        }).toList();

        return Response.ok(new PaginadoResponseDto<>(dtos, paginado.getTotalElementos(), page, size)).build();

    }

}