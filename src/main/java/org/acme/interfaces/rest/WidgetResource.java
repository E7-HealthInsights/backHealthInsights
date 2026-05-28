package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.acme.application.dto.CreateWidgetDto;
import org.acme.application.dto.ErrorResponseDto;
import org.acme.application.dto.WidgetOrdenDto;
import org.acme.application.dto.WidgetResponseDto;
import org.acme.application.usecase.CreateWidgetUseCase;
import org.acme.application.usecase.DeleteWidgetUseCase;
import org.acme.application.usecase.GetUserWidgetsUseCase;
import org.acme.application.usecase.UpdateWidgetOrdenUseCase;
import org.acme.domain.models.Widget;
import org.acme.infrastructure.security.AuthContext;
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
import java.util.UUID;

@Path("/widgets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Widgets", description = "Gestión de widgets del dashboard del usuario")
@SecurityRequirement(name = "bearerAuth")
public class WidgetResource {

    @Inject
    CreateWidgetUseCase createWidgetUseCase;
    @Inject
    DeleteWidgetUseCase deleteWidgetUseCase;
    @Inject
    GetUserWidgetsUseCase getUserWidgetsUseCase;
    @Inject
    UpdateWidgetOrdenUseCase updateWidgetOrdenUseCase;

    @POST
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Crear widget",
        description = "Crea un nuevo widget en el dashboard del usuario autenticado. Requiere JWT de Firebase. Roles: DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Widget creado exitosamente",
            content = @Content(schema = @Schema(implementation = WidgetResponseDto.class))),
        @APIResponse(responseCode = "400", description = "Datos inválidos (titulo, tipoId o queryConfig ausente)"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Rol no permitido")
    })
    public Response createWidget(@Valid CreateWidgetDto requestdto) {
        Widget widget = createWidgetUseCase.execute(requestdto);
        WidgetResponseDto dto = new WidgetResponseDto();
        dto.setId(widget.getId());
        dto.setTitulo(widget.getTitulo());
        dto.setOrden(widget.getOrden());
        dto.setTipo(widget.getTipo() != null ? widget.getTipo().getNombre() : null);
        dto.setTipoSemantico(widget.getTipoSemantico());
        dto.setNivelGeografico(widget.getNivelGeografico());

        return Response.status(Response.Status.CREATED)
                .entity(dto)
                .build();
    }

    @GET
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Listar widgets del usuario",
        description = "Devuelve todos los widgets del dashboard del usuario autenticado, ordenados por su campo orden. Requiere JWT de Firebase. Roles: DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Lista de widgets del usuario",
            content = @Content(schema = @Schema(implementation = WidgetResponseDto.class))),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Rol no permitido")
    })
    public Response getWidgets() {
        List<WidgetResponseDto> response = getUserWidgetsUseCase.execute();
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Eliminar widget",
        description = "Elimina un widget del dashboard. Solo el dueño del widget puede eliminarlo. Requiere JWT de Firebase. Roles: DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Widget eliminado exitosamente"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "No es el dueño del widget",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
        @APIResponse(responseCode = "404", description = "Widget no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public Response deleteWidget(
        @Parameter(description = "UUID del widget", in = ParameterIn.PATH, required = true)
        @PathParam("id") UUID id) {
        try {
            deleteWidgetUseCase.execute(id);
            return Response.noContent().build();
        } catch (ForbiddenException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponseDto(e.getMessage()))
                    .build();
        }
    }

    @PATCH
    @Path("/orden")
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    @Operation(
        summary     = "Actualizar orden de widgets",
        description = "Reordena múltiples widgets en una sola operación. Recibe un arreglo de objetos { id: UUID, orden: int } (mínimo orden = 1). Requiere JWT de Firebase. Roles: DIRECTOR_GENERAL, DIRECTOR_FINANZAS, DIRECTOR_MERCADOTECNIA."
    )
    @APIResponses({
        @APIResponse(responseCode = "204", description = "Orden actualizado exitosamente"),
        @APIResponse(responseCode = "400", description = "Lista inválida o campos requeridos ausentes"),
        @APIResponse(responseCode = "401", description = "Sin autenticación"),
        @APIResponse(responseCode = "403", description = "Rol no permitido")
    })
    public Response updateOrden(@Valid List<WidgetOrdenDto> items) {
        updateWidgetOrdenUseCase.execute(items);
        return Response.noContent().build();
    }
}
