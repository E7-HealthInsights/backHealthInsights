package org.acme.interfaces.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.application.dto.CreateWidgetDto;
import org.acme.application.usecase.CreateWidgetUseCase;
import org.acme.application.usecase.GetUserWidgetsUseCase;
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

    @POST
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    public Response createWidget(@Valid CreateWidgetDto dto) {
        Widget widget = createWidgetUseCase.execute(dto);
        return Response.status(Response.Status.CREATED)
                .entity(widget)
                .build();
    }

    @GET
    @RolesAllowed({"DIRECTOR_GENERAL", "DIRECTOR_FINANZAS", "DIRECTOR_MERCADOTECNIA"})
    public Response getWidgets() {

        List<Widget> widgets = getUserWidgetsUseCase.execute();

        return Response.ok(widgets).build();
    }
}
