package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.WidgetOrdenDto;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.List;

@ApplicationScoped
public class UpdateWidgetOrdenUseCase {

    private final WidgetRepository widgetRepository;
    private final AuthContext authContext;

    @Inject
    public UpdateWidgetOrdenUseCase(WidgetRepository widgetRepository, AuthContext authContext) {
        this.widgetRepository = widgetRepository;
        this.authContext = authContext;
    }

    /**
     * Actualiza el orden de los widgets personales del usuario en batch.
     * Solo se procesan widgets del usuario autenticado — los widgets de rol
     * no se pueden reordenar desde aquí.
     */
    public void execute(List<WidgetOrdenDto> items) {
        // Validamos que todos los IDs pertenezcan al usuario antes de actualizar
        // usando el conjunto de widgets personales del usuario.
        java.util.Set<java.util.UUID> ownedIds = widgetRepository
                .findByUserId(authContext.getUser().getId())
                .stream()
                .map(org.acme.domain.models.Widget::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (WidgetOrdenDto item : items) {
            if (ownedIds.contains(item.getId())) {
                widgetRepository.updateOrden(item.getId(), item.getOrden());
            }
        }
    }
}
