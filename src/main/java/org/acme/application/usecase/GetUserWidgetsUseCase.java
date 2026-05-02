package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.models.User;
import org.acme.domain.models.Widget;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GetUserWidgetsUseCase {

    private final WidgetRepository widgetRepository;
    private final AuthContext authContext;

    @Inject
    public GetUserWidgetsUseCase(WidgetRepository widgetRepository, AuthContext authContext){
        this.widgetRepository = widgetRepository;
        this.authContext = authContext;
    }

    public List<Widget> execute() {
        List<Widget> todos = new ArrayList<>();
        todos.addAll(widgetRepository.findDefaultsByRolId(authContext.getUser().getRole().getId()));
        todos.addAll(widgetRepository.findByUserId(authContext.getUser().getId()));
        return todos;
    }
}
