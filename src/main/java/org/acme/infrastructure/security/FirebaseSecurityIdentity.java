package org.acme.infrastructure.security;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import org.acme.domain.models.User;

import java.security.Permission;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

public class FirebaseSecurityIdentity implements SecurityIdentity {

    private final User user;

    public FirebaseSecurityIdentity(User user) {
        this.user = user;
    }

    @Override
    public Principal getPrincipal() {
        return () -> user.getEmail();
    }

    @Override
    public boolean isAnonymous() { return false; }

    @Override
    public Set<String> getRoles() {
        return Set.of(user.getRole().getName()); // ej: "ADMIN", "DIRECTOR_GENERAL"
    }

    @Override
    public boolean hasRole(String role) {
        // Quarkus llama a este método cuando evalúa @RolesAllowed
        return user.getRole() != null && user.getRole().getName().equals(role);
    }

    @Override
    public <T extends Principal> T getPrincipal(Class<T> clazz) { return null; }

    @Override
    public Set<Credential> getCredentials() { return Set.of(); }

    @Override
    public Map<String, Object> getAttributes() { return Map.of(); }

    @Override
    public Uni<Boolean> checkPermission(Permission permission) {
        return Uni.createFrom().item(false);
    }

    @Override
    public Set<Permission> getPermissions() {
        return Set.of();
    }

    @Override
    public <T extends Credential> T getCredential(Class<T> aClass) {
        return null;
    }

    @Override
    public <T> T getAttribute(String name) {
        return null;
    }

}