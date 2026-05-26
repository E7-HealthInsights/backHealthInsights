package org.acme.infrastructure.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Proyeccion;
import org.acme.domain.models.User;
import org.acme.domain.repository.ProyeccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
// Prueba de integración — levanta Quarkus con H2
// Rol y Usuario ya existen desde import.sql de test
class ProyeccionRepositoryImplTest {

    @Inject ProyeccionRepository proyeccionRepository;
    @Inject EntityManager em;

    private static final UUID TEST_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

            @BeforeEach
            @Transactional
            void setUp() {
                // Limpia proyecciones del test anterior
                em.createQuery("DELETE FROM ProyeccionEntity p WHERE p.usuario.id = :uid")
                        .setParameter("uid", TEST_USER_ID)
                        .executeUpdate();
            
                // MERGE INTO — inserta si no existe, no falla si ya existe
                // Bypasea el entity tracking de Hibernate → sin OptimisticLock
                em.createNativeQuery(
                    "MERGE INTO Role (id, name) KEY(id) VALUES (1, 'ADMIN')"
                ).executeUpdate();
            
                em.createNativeQuery(
                    "MERGE INTO Tipo_de_Grafica (id, nombre) KEY(id) VALUES (1, 'STAT')"
                ).executeUpdate();
            
                em.createNativeQuery(
                    "MERGE INTO Users (id, name, last_name, email, role_id, status, provider_id) " +
                    "KEY(id) VALUES " +
                    "('00000000-0000-0000-0000-000000000001', 'Test', 'Admin', 'test@test.com', 1, true, 'test-firebase-uid')"
                ).executeUpdate();
            }

    private Proyeccion buildProyeccion(String titulo) {
        User usuario = new User();
        usuario.setId(TEST_USER_ID);

        Proyeccion p = new Proyeccion();
        p.setId(UUID.randomUUID());
        p.setTitulo(titulo);
        p.setDescripcion("Descripción de " + titulo);
        p.setUsuario(usuario);
        p.setParametros("{\"params\":{},\"puntos\":[],\"kpis\":{}}");
        p.setFechaCreacion(java.time.LocalDateTime.now());
        p.setFechaActualizacion(java.time.LocalDateTime.now());
        return p;
    }

    @Test
    void saveShouldPersistProyeccionAndReturnIt() {
        Proyeccion p = buildProyeccion("Escenario test H2");

        Proyeccion saved = proyeccionRepository.save(p);

        assertNotNull(saved);
        assertEquals("Escenario test H2", saved.getTitulo());
        assertEquals(p.getId(), saved.getId());
    }

    @Test
    void findByUsuarioIdShouldReturnProyeccionesOfUser() {
        proyeccionRepository.save(buildProyeccion("Escenario A"));
        proyeccionRepository.save(buildProyeccion("Escenario B"));

        List<Proyeccion> result = proyeccionRepository.findByUsuarioId(TEST_USER_ID);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> p.getTitulo().equals("Escenario A")));
        assertTrue(result.stream().anyMatch(p -> p.getTitulo().equals("Escenario B")));
    }

    @Test
    void findByUsuarioIdShouldReturnEmptyForUserWithNoProyecciones() {
        List<Proyeccion> result = proyeccionRepository.findByUsuarioId(UUID.randomUUID());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findByIdShouldReturnProyeccionWithUsuarioLoaded() {
        Proyeccion saved = proyeccionRepository.save(buildProyeccion("Test findById"));

        Optional<Proyeccion> found = proyeccionRepository.findProyeccionById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Test findById", found.get().getTitulo());
        // EntityGraph cargó el usuario
        assertNotNull(found.get().getUsuario());
    }

    @Test
    void findByIdShouldReturnEmptyForNonExistentId() {
        Optional<Proyeccion> found = proyeccionRepository.findProyeccionById(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    @Test
    void updateShouldModifyTituloAndResultado() {
        Proyeccion saved = proyeccionRepository.save(buildProyeccion("Original"));

        saved.setTitulo("Actualizado");
        saved.setParametros("{\"new\":true}");
        Proyeccion updated = proyeccionRepository.update(saved);

        assertEquals("Actualizado", updated.getTitulo());
        assertEquals("{\"new\":true}", updated.getParametros());
    }

    @Test
    void deleteShouldRemoveProyeccion() {
        Proyeccion saved = proyeccionRepository.save(buildProyeccion("A eliminar"));

        proyeccionRepository.delete(saved.getId());

        Optional<Proyeccion> found = proyeccionRepository.findProyeccionById(saved.getId());
        assertTrue(found.isEmpty());
    }
}