package org.acme.infrastructure.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UserRepositoryImplTest {

    // NOTAS DE IMPLEMENTACIÓN:
    // - findPaginated y countUsers SIEMPRE filtran por status (WHERE u.status = :status).
    //   Pasar null devuelve 0 resultados. Los tests usan status=true explícitamente.
    // - findPaginated y countUsers usan paginación base-1: page=1 es la primera página.
    // - El import.sql falla al arrancar porque Role no existe aún cuando Hibernate
    //   lo ejecuta. El @BeforeEach recrea el seed vía MERGE dentro de su propia
    //   transacción, garantizando que los datos existen para cada test.

    @Inject UserRepository userRepository;
    @Inject EntityManager em;

    private static final UUID TEST_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    @Transactional
    void setUp() {
        // Limpia primero para evitar conflictos entre tests
        em.createQuery("DELETE FROM UserEntity u WHERE u.id <> :seed")
                .setParameter("seed", TEST_USER_ID)
                .executeUpdate();
        em.createNativeQuery("MERGE INTO Rol (id, nombre) KEY(id) VALUES (1, 'ADMIN')").executeUpdate();
        em.createNativeQuery(
                "MERGE INTO Usuario (id, nombre, apellido, correo, rol_id, estatus, proveedor_id) KEY(id) VALUES " +
                        "('00000000-0000-0000-0000-000000000001', 'Test', 'Admin', 'test@test.com', 1, true, 'test-firebase-uid')"
        ).executeUpdate();
    }

    private User buildUser(String email, String firebaseUid) {
        Role role = new Role((byte) 1, "ADMIN");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setStatus(true);
        user.setProviderId(firebaseUid);
        user.setRole(role);
        return user;
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void createShouldPersistAndReturnUser() {
        User saved = userRepository.create(buildUser("nuevo@test.com", "uid-nuevo"));

        assertNotNull(saved);
        assertEquals("nuevo@test.com", saved.getEmail());
    }

    // ── findByFirebaseUuid ────────────────────────────────────────────────────

    @Test
    void findByFirebaseUuidShouldReturnUserWhenExists() {
        Optional<User> found = userRepository.findByFirebaseUuid("test-firebase-uid");

        assertTrue(found.isPresent());
        assertEquals("test@test.com", found.get().getEmail());
    }

    @Test
    void findByFirebaseUuidShouldReturnEmptyWhenNotFound() {
        Optional<User> found = userRepository.findByFirebaseUuid("uid-inexistente");
        assertTrue(found.isEmpty());
    }

    // ── existsByEmail ─────────────────────────────────────────────────────────

    @Test
    void existsByEmailShouldReturnTrueWhenEmailExists() {
        assertTrue(userRepository.existsByEmail("test@test.com"));
    }

    @Test
    void existsByEmailShouldReturnFalseWhenEmailNotFound() {
        assertFalse(userRepository.existsByEmail("noexiste@test.com"));
    }

    // ── findAllUsers ──────────────────────────────────────────────────────────

    @Test
    void findAllUsersShouldReturnAtLeastSeedUser() {
        List<User> users = userRepository.findAllUsers();

        assertFalse(users.isEmpty());
        assertTrue(users.stream().anyMatch(u -> "test@test.com".equals(u.getEmail())));
    }

    // ── findUserById ──────────────────────────────────────────────────────────

    @Test
    void findUserByIdShouldReturnUserWithRoleLoaded() {
        Optional<User> found = userRepository.findUserById(TEST_USER_ID);

        assertTrue(found.isPresent());
        assertNotNull(found.get().getRole());
        assertEquals("ADMIN", found.get().getRole().getName());
    }

    @Test
    void findUserByIdShouldReturnEmptyForNonExistentId() {
        Optional<User> found = userRepository.findUserById(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void updateShouldModifyUserFields() {
        User user = buildUser("update@test.com", "uid-update");
        userRepository.create(user);

        user.setName("Nombre Actualizado");
        user.setStatus(false);
        User updated = userRepository.update(user);

        assertEquals("Nombre Actualizado", updated.getName());
        assertFalse(updated.isStatus());
    }

    // ── findPaginated ─────────────────────────────────────────────────────────
    // Requiere status no nulo — siempre filtra WHERE u.status = :status.
    // Paginación base-1: page=1 es la primera página.

    @Test
    void findPaginatedShouldReturnActiveUsersOnPage1() {
        List<User> result = userRepository.findPaginated(1, 10, null, true);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(User::isStatus));
    }

    @Test
    @Transactional
    void findPaginatedShouldFilterBySearchTerm() {
        userRepository.create(buildUser("busqueda@test.com", "uid-busqueda"));

        List<User> result = userRepository.findPaginated(1, 10, "busqueda", true);

        assertTrue(result.stream().anyMatch(u -> "busqueda@test.com".equals(u.getEmail())));
    }

    @Test
    @Transactional
    void findPaginatedShouldReturnInactiveUsersWhenStatusFalse() {
        User inactive = buildUser("inactivo@test.com", "uid-inactivo");
        inactive.setStatus(false);
        userRepository.create(inactive);

        List<User> result = userRepository.findPaginated(1, 10, null, false);

        assertTrue(result.stream().allMatch(u -> !u.isStatus()));
    }

    // ── countUsers ────────────────────────────────────────────────────────────
    // Requiere status no nulo — siempre filtra WHERE u.status = :status.

    @Test
    void countUsersShouldReturnPositiveCountForActiveUsers() {
        long count = userRepository.countUsers(null, true);
        assertTrue(count >= 1);
    }

    @Test
    @Transactional
    void countUsersShouldFilterBySearchTerm() {
        userRepository.create(buildUser("conteo@test.com", "uid-conteo"));

        long count = userRepository.countUsers("conteo", true);
        assertTrue(count >= 1);
    }
}