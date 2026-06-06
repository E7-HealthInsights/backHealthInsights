package org.acme.infrastructure.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.acme.domain.models.Role;
import org.acme.domain.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RoleRepositoryImplTest {

    @Inject RoleRepository roleRepository;
    @Inject EntityManager em;

    @BeforeEach
    @Transactional
    void setUp() {
        em.createNativeQuery("MERGE INTO Role (id, name) KEY(id) VALUES (1, 'ADMIN')").executeUpdate();
        em.createNativeQuery("MERGE INTO Role (id, name) KEY(id) VALUES (2, 'DIRECTOR_GENERAL')").executeUpdate();
        em.createNativeQuery("MERGE INTO Role (id, name) KEY(id) VALUES (3, 'DIRECTOR_FINANZAS')").executeUpdate();
        em.createNativeQuery("MERGE INTO Role (id, name) KEY(id) VALUES (4, 'DIRECTOR_MERCADOTECNIA')").executeUpdate();
    }

    @Test
    void findRoleByIdShouldReturnRoleWhenExists() {
        Optional<Role> found = roleRepository.findRoleById((byte) 1);

        assertTrue(found.isPresent());
        assertEquals("ADMIN", found.get().getName());
    }

    @Test
    void findRoleByIdShouldReturnCorrectRoleForEachId() {
        assertEquals("DIRECTOR_GENERAL",       roleRepository.findRoleById((byte) 2).orElseThrow().getName());
        assertEquals("DIRECTOR_FINANZAS",      roleRepository.findRoleById((byte) 3).orElseThrow().getName());
        assertEquals("DIRECTOR_MERCADOTECNIA", roleRepository.findRoleById((byte) 4).orElseThrow().getName());
    }

    @Test
    void findRoleByIdShouldReturnEmptyForNonExistentId() {
        Optional<Role> found = roleRepository.findRoleById((byte) 99);
        assertTrue(found.isEmpty());
    }
}
