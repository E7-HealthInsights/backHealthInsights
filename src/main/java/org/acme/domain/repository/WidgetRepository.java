package org.acme.domain.repository;

import org.acme.domain.models.Widget;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WidgetRepository {
    Widget create(Widget widget);
    List<Widget> findByUserId(UUID userId);
    List<Widget> findDefaultsByRolId(Byte rolId);
    void updateOrden(UUID widgetId, int orden);
    void removeById(UUID widgetId);
}