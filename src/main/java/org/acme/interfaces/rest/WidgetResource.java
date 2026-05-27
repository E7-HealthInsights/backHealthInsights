package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.CreateWidgetDto;
import org.acme.application.dto.WidgetOrdenDto;
import org.acme.application.dto.WidgetResponseDto;
import org.acme.application.usecase.CreateWidgetUseCase;
import org.acme.application.usecase.GetUserWidgetsUseCase;
import org.acme.application.usecase.UpdateWidgetOrdenUseCase;
import org.acme.domain.models.Widget;
import org.acme.infrastructure.security.AuthContext;

import java.util.List;

@Path("/widgets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WidgetResource {

    @Inject
    CreateWidgetUseCase createWidgetUseCase;
    @Inject
    GetUserWidgetsUseCase getUserWidgetsUseCase;
    @Inject
    UpdateWidgetOrdenUseCase updateWidgetOrdenUseCase;

    @POST
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
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
    public Response getWidgets() {
        List<WidgetResponseDto> response = getUserWidgetsUseCase.execute();
        return Response.ok(response).build();
    }

    @PATCH
    @Path("/orden")
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    public Response updateOrden(@Valid List<WidgetOrdenDto> items) {
        updateWidgetOrdenUseCase.execute(items);
        return Response.noContent().build();
    }
}
