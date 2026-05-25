package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.CreateWidgetDto;
import org.acme.domain.models.TipoWidget;
import org.acme.domain.models.User;
import org.acme.domain.models.Widget;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.security.AuthContext;

import java.util.UUID;

@ApplicationScoped
public class CreateWidgetUseCase {

    private final WidgetRepository widgetRepository;
    private final AuthContext authContext;

    @Inject
    public CreateWidgetUseCase(WidgetRepository widgetRepository, AuthContext authContext){
        this.widgetRepository = widgetRepository;
        this.authContext = authContext;
    }

    public Widget execute(CreateWidgetDto dto){
        TipoWidget tipoWidget = new TipoWidget();
        tipoWidget.setId(dto.getTipoId());

        Widget widget = new Widget();
        widget.setId(UUID.randomUUID());
        widget.setTitulo(dto.getTitulo());
        widget.setQuery(dto.getQueryConfig());  //ahorita como string
        widget.setOrden(dto.getOrden());
        widget.setUsuario(authContext.getUser());
        widget.setTipo(tipoWidget);
        widget.setRolId(null);   //pues es personalizado, no asociado al rol
        widget.setTipoSemantico(emptyToNull(dto.getTipoSemantico()));
        widget.setNivelGeografico(emptyToNull(dto.getNivelGeografico()));

        return widgetRepository.create(widget);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
