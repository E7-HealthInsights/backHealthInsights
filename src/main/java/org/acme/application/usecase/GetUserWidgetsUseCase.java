package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.WidgetResponseDto;
import org.acme.domain.models.User;
import org.acme.domain.models.Widget;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.query.QueryExecutor;
import org.acme.infrastructure.security.AuthContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GetUserWidgetsUseCase {

    private final WidgetRepository widgetRepository;
    private final AuthContext authContext;
    private final QueryExecutor queryExecutor;

    @Inject
    public GetUserWidgetsUseCase(WidgetRepository widgetRepository, AuthContext authContext, QueryExecutor queryExecutor){
        this.widgetRepository = widgetRepository;
        this.authContext = authContext;
        this.queryExecutor = queryExecutor;
    }

    public List<WidgetResponseDto> execute() {
        List<Widget> todos = new ArrayList<>();
        todos.addAll(widgetRepository.findDefaultsByRolId(authContext.getUser().getRole().getId()));
        todos.addAll(widgetRepository.findByUserId(authContext.getUser().getId()));
        return todos.stream().map(widget -> {
            WidgetResponseDto dto = new WidgetResponseDto();
            dto.setId(widget.getId());
            dto.setTitulo(widget.getTitulo());
            dto.setOrden(widget.getOrden());
            String tipo = widget.getTipo() != null ? widget.getTipo().getNombre() : "STAT";
            dto.setTipo(tipo);
            dto.setData(queryExecutor.execute(widget.getQuery(), tipo));

            return dto;
        }).toList();
    }
}
