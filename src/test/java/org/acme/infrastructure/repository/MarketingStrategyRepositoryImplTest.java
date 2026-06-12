package org.acme.infrastructure.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.MarketingStrategy;
import org.acme.domain.repository.MarketingStrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MarketingStrategyRepositoryImplTest {

    @Inject MarketingStrategyRepository repository;
    @Inject EntityManager em;

    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    @Transactional
    void setUp() {
        em.createNativeQuery("DELETE FROM Marketing_Strategy").executeUpdate();
        em.createNativeQuery("MERGE INTO Rol (id, nombre) KEY(id) VALUES (1, 'ADMIN')").executeUpdate();
        em.createNativeQuery(
                "MERGE INTO Usuario (id, nombre, apellido, correo, rol_id, estatus, proveedor_id) KEY(id) VALUES " +
                        "('00000000-0000-0000-0000-000000000001', 'Test', 'Admin', 'test@test.com', 1, true, 'test-firebase-uid')"
        ).executeUpdate();
    }

    private MarketingStrategy buildStrategy() {
        return new MarketingStrategy(
                UUID.randomUUID(),
                USER_ID,
                LocalDateTime.now(),
                null,
                "{\"resumen_ejecutivo\":\"Estrategia de prueba\"}"
        );
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void createShouldPersistAndReturnStrategy() {
        MarketingStrategy saved = repository.create(buildStrategy());

        assertNotNull(saved);
        assertEquals(USER_ID, saved.getUsuarioId());
        assertEquals(MarketingStrategy.ESTADO_PROPUESTA, saved.getEstado());
    }

    @Test
    @Transactional
    void createShouldDefaultEstadoPropuesta() {
        MarketingStrategy s = buildStrategy();
        s.setEstado(null);

        MarketingStrategy saved = repository.create(s);

        assertEquals(MarketingStrategy.ESTADO_PROPUESTA, saved.getEstado());
    }

    // ── findByUsuarioId ───────────────────────────────────────────────────────

    @Test
    @Transactional
    void findByUsuarioIdShouldReturnStrategiesOfUser() {
        repository.create(buildStrategy());
        repository.create(buildStrategy());

        List<MarketingStrategy> result = repository.findByUsuarioId(USER_ID);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(s -> USER_ID.equals(s.getUsuarioId())));
    }

    @Test
    @Transactional
    void findByUsuarioIdShouldReturnEmptyForOtherUser() {
        repository.create(buildStrategy());

        List<MarketingStrategy> result = repository.findByUsuarioId(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    // ── findOneById ───────────────────────────────────────────────────────────

    @Test
    @Transactional
    void findOneByIdShouldReturnStrategyWhenExists() {
        MarketingStrategy saved = repository.create(buildStrategy());

        Optional<MarketingStrategy> found = repository.findOneById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findOneByIdShouldReturnEmptyForNonExistentId() {
        Optional<MarketingStrategy> found = repository.findOneById(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    // ── update ────────────────────────────────────────────────────────────────
    // update() solo persiste: estado, notaResultado, fechaRevision, comentariosJson

    @Test
    @Transactional
    void updateShouldChangeEstado() {
        MarketingStrategy saved = repository.create(buildStrategy());

        saved.setEstado(MarketingStrategy.ESTADO_EJECUTADA);
        saved.setNotaResultado("Excelentes resultados");
        saved.setFechaRevision(LocalDateTime.now());
        MarketingStrategy updated = repository.update(saved);

        assertEquals(MarketingStrategy.ESTADO_EJECUTADA, updated.getEstado());
        assertEquals("Excelentes resultados", updated.getNotaResultado());
    }

    @Test
    @Transactional
    void updateShouldPersistComentariosJson() {
        MarketingStrategy saved = repository.create(buildStrategy());

        saved.setComentariosJson("[{\"contenido\":\"Funciona bien\"}]");
        MarketingStrategy updated = repository.update(saved);

        assertEquals("[{\"contenido\":\"Funciona bien\"}]", updated.getComentariosJson());
    }
}