package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.UUID;

@ApplicationScoped
public class DeleteWidgetUseCase {

    private final WidgetRepository widgetRepository;
    private final AuthContext      authContext;

    @Inject
    public DeleteWidgetUseCase(WidgetRepository widgetRepository, AuthContext authContext) {
        this.widgetRepository = widgetRepository;
        this.authContext      = authContext;
    }

    /**
     * Elimina un widget personal del usuario autenticado.
     * Lanza 403 si el widget pertenece al rol (esDefault) — esos no se pueden eliminar.
     * Lanza 404 si el ID no existe en los widgets del usuario.
     */
    public void execute(UUID widgetId) {
        boolean owned = widgetRepository
                .findByUserId(authContext.getUser().getId())
                .stream()
                .anyMatch(w -> w.getId().equals(widgetId));

        if (!owned) {
            // Puede ser un widget del rol o simplemente no existir — en ambos casos no procede
            boolean isDefault = widgetRepository
                    .findDefaultsByRolId(authContext.getUser().getRole().getId())
                    .stream()
                    .anyMatch(w -> w.getId().equals(widgetId));

            if (isDefault) {
                throw new ForbiddenException("Los widgets del rol no pueden eliminarse");
            }
            throw new NotFoundException("Widget no encontrado: " + widgetId);
        }

        widgetRepository.removeById(widgetId);
    }
}