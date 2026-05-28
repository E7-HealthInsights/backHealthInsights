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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

import java.util.ArrayList;
import java.util.List;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(
        summary     = "Crear usuario",
        description = "Crea un nuevo usuario en Firebase y en la BD. Requiere JWT de Firebase. Solo ADMIN."
    )
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Usuario creado exitosamente",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
        @APIResponse(responseCode = "400", description = "Rol no encontrado o datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo ADMIN puede crear usuarios"),
        @APIResponse(responseCode = "409", description = "Email ya registrado",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
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
    @Operation(
        summary     = "Actualizar usuario",
        description = "Edita los datos de un usuario existente. Requiere JWT de Firebase. Solo ADMIN."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Usuario actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
        @APIResponse(responseCode = "400", description = "Rol inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo ADMIN"),
        @APIResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response updateUser(
        @Parameter(description = "UUID del usuario a actualizar", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id,
        @Valid UpdateUserDto updateUserDto) {
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
    @Operation(
        summary     = "Desactivar usuario",
        description = "Baja lógica del usuario (no lo elimina de la BD). Requiere JWT de Firebase. Solo ADMIN."
    )
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Usuario desactivado exitosamente"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo ADMIN"),
        @APIResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response deactivateUser(
        @Parameter(description = "UUID del usuario a desactivar", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id,
        DeactivateUserDto deactivateUserDto) {
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
    @Operation(
        summary     = "Listar usuarios paginados",
        description = "Devuelve usuarios activos o inactivos con paginación y búsqueda por nombre o correo. Requiere JWT de Firebase. Solo ADMIN."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista paginada de usuarios"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Solo ADMIN")
    })
    public Response listUsers(
        @Parameter(description = "Número de página (mínimo 1)", in = ParameterIn.QUERY, example = "1")
        @QueryParam("page")   @DefaultValue("1")     int     page,

        @Parameter(description = "Tamaño de página (1-100)", in = ParameterIn.QUERY, example = "10")
        @QueryParam("size")   @DefaultValue("10")    int     size,

        @Parameter(description = "Búsqueda por nombre o correo electrónico", in = ParameterIn.QUERY, example = "juan")
        @QueryParam("search") @DefaultValue("")      String  search,

        @Parameter(description = "Filtro de estado: true = activos, false = inactivos", in = ParameterIn.QUERY, example = "true")
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