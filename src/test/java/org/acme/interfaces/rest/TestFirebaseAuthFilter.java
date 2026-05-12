package org.acme.interfaces.rest;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.ext.Provider;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.infrastructure.security.AuthContext;
import org.acme.infrastructure.security.FirebaseAuthFilter;
import org.acme.infrastructure.security.FirebaseSecurityIdentity;
import io.quarkus.test.Mock;

import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import java.util.UUID;

/**
 * Reemplaza al FirebaseAuthFilter real durante los tests de integración (@QuarkusTest).
 * Inyecta un usuario ADMIN fijo para que todos los endpoints protegidos funcionen
 * sin necesitar un token de Firebase real.
 */
@Mock
@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class TestFirebaseAuthFilter extends FirebaseAuthFilter {

    public static final UUID TEST_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Inject
    AuthContext authContext;

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Role role = new Role((byte) 1, "ADMIN");
        User user = new User(
                TEST_USER_ID, "Test", "User",
                "test@test.com", role, true, "test-firebase-uid");

        authContext.setUser(user);
        identityAssociation.setIdentity(
                Uni.createFrom().item(new FirebaseSecurityIdentity(user))
        );
    }
}