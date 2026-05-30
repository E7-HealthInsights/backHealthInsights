package org.acme.infrastructure.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.TipoWidget;
import org.acme.domain.models.User;
import org.acme.domain.models.Widget;
import org.acme.domain.repository.WidgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WidgetRepositoryImplTest {

    @Inject WidgetRepository widgetRepository;
    @Inject EntityManager em;

    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Byte ROL_ID = (byte) 1;

    @BeforeEach
    @Transactional
    void setUp() {
        em.createQuery("DELETE FROM WidgetEntity").executeUpdate();
        em.createNativeQuery("MERGE INTO Role (id, name) KEY(id) VALUES (1, 'ADMIN')").executeUpdate();
        em.createNativeQuery("MERGE INTO Tipo_de_Grafica (id, nombre) KEY(id) VALUES (1, 'STAT')").executeUpdate();
        em.createNativeQuery(
                "MERGE INTO Users (id, name, last_name, email, role_id, status, provider_id) KEY(id) VALUES " +
                        "('00000000-0000-0000-0000-000000000001', 'Test', 'Admin', 'test@test.com', 1, true, 'test-firebase-uid')"
        ).executeUpdate();
    }

    private Widget buildPersonalWidget(String titulo) {
        User user = new User();
        user.setId(USER_ID);
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        return new Widget(UUID.randomUUID(), titulo, user, tipo, "{\"tabla\":\"test\"}", 1, null);
    }

    private Widget buildDefaultWidget(String titulo, Byte rolId) {
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        return new Widget(UUID.randomUUID(), titulo, null, tipo, "{\"tabla\":\"test\"}", 1, rolId);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void createShouldPersistPersonalWidget() {
        Widget saved = widgetRepository.create(buildPersonalWidget("Widget personal"));

        assertNotNull(saved);
        assertEquals("Widget personal", saved.getTitulo());
        assertNotNull(saved.getId());
    }

    @Test
    void createShouldPersistDefaultWidget() {
        Widget saved = widgetRepository.create(buildDefaultWidget("Widget default", ROL_ID));

        assertNotNull(saved);
        assertEquals(ROL_ID, saved.getRolId());
    }

    // ── findByUserId ──────────────────────────────────────────────────────────

    @Test
    void findByUserIdShouldReturnWidgetsOfUser() {
        widgetRepository.create(buildPersonalWidget("Widget A"));
        widgetRepository.create(buildPersonalWidget("Widget B"));

        List<Widget> result = widgetRepository.findByUserId(USER_ID);

        assertEquals(2, result.size());
    }

    @Test
    void findByUserIdShouldReturnEmptyForUserWithNoWidgets() {
        List<Widget> result = widgetRepository.findByUserId(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdShouldNotReturnDefaultWidgets() {
        widgetRepository.create(buildDefaultWidget("Widget rol", ROL_ID));

        List<Widget> result = widgetRepository.findByUserId(USER_ID);

        assertTrue(result.isEmpty());
    }

    // ── findDefaultsByRolId ───────────────────────────────────────────────────

    @Test
    void findDefaultsByRolIdShouldReturnDefaultWidgets() {
        widgetRepository.create(buildDefaultWidget("Default 1", ROL_ID));
        widgetRepository.create(buildDefaultWidget("Default 2", ROL_ID));

        List<Widget> result = widgetRepository.findDefaultsByRolId(ROL_ID);

        assertEquals(2, result.size());
    }

    @Test
    void findDefaultsByRolIdShouldReturnEmptyForOtherRole() {
        widgetRepository.create(buildDefaultWidget("Default rol 1", ROL_ID));

        List<Widget> result = widgetRepository.findDefaultsByRolId((byte) 99);

        assertTrue(result.isEmpty());
    }

    @Test
    void findDefaultsByRolIdShouldNotReturnPersonalWidgets() {
        widgetRepository.create(buildPersonalWidget("Personal"));

        List<Widget> result = widgetRepository.findDefaultsByRolId(ROL_ID);

        assertTrue(result.isEmpty());
    }

    // ── updateOrden ───────────────────────────────────────────────────────────

    @Test
    void updateOrdenShouldChangeWidgetOrder() {
        Widget saved = widgetRepository.create(buildPersonalWidget("Widget orden"));

        widgetRepository.updateOrden(saved.getId(), 99);

        List<Widget> widgets = widgetRepository.findByUserId(USER_ID);
        Widget found = widgets.stream()
                .filter(w -> w.getId().equals(saved.getId()))
                .findFirst().orElseThrow();

        assertEquals(99, found.getOrden());
    }

    // ── removeById ────────────────────────────────────────────────────────────

    @Test
    void removeByIdShouldDeleteWidget() {
        Widget saved = widgetRepository.create(buildPersonalWidget("Widget a eliminar"));

        widgetRepository.removeById(saved.getId());

        List<Widget> result = widgetRepository.findByUserId(USER_ID);
        assertTrue(result.stream().noneMatch(w -> w.getId().equals(saved.getId())));
    }

    @Test
    void removeByIdShouldOnlyDeleteTargetWidget() {
        Widget toKeep   = widgetRepository.create(buildPersonalWidget("Widget que queda"));
        Widget toDelete = widgetRepository.create(buildPersonalWidget("Widget a borrar"));

        widgetRepository.removeById(toDelete.getId());

        List<Widget> remaining = widgetRepository.findByUserId(USER_ID);
        assertEquals(1, remaining.size());
        assertEquals(toKeep.getId(), remaining.get(0).getId());
    }
}
