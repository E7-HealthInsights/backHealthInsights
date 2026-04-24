package org.acme.infrastructure.security;

import jakarta.ws.rs.core.SecurityContext;
import org.acme.domain.models.User;

import java.security.Principal;

public class FirebaseSecurityContext implements SecurityContext {

    private final User user;

    public FirebaseSecurityContext(User user) {
        this.user = user;
    }

    @Override
    public Principal getUserPrincipal() {
        return () -> user.getEmail(); // identifica al usuario por email
    }

    @Override
    public boolean isUserInRole(String role) {
        return user.getRole() != null && user.getRole().getName().equals(role);
        // compara el rol del usuario con el rol requerido por @RolesAllowed
    }

    @Override
    public boolean isSecure() { return true; }

    @Override
    public String getAuthenticationScheme() { return "Bearer"; }
}