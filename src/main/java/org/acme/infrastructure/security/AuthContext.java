package org.acme.infrastructure.security;

import jakarta.enterprise.context.RequestScoped;
import org.acme.domain.models.User;

@RequestScoped
public class AuthContext {

    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
