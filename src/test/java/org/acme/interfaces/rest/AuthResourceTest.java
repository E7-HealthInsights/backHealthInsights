package org.acme.interfaces.rest;

import jakarta.ws.rs.core.Response;
import org.acme.application.dto.UserResponseDto;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthResourceTest {

    private AuthContext authContext;
    private AuthResource resource;

    private User authenticatedUser;
    private Role role;

    @BeforeEach
    void setUp() {
        authContext = mock(AuthContext.class);

        role = new Role((byte) 1, "ADMIN");
        authenticatedUser = new User(
                UUID.randomUUID(),
                "Gabriel",
                "Gutiérrez",
                "gabriel@test.com",
                role,
                true,
                "firebase-uid-1");

        when(authContext.getUser()).thenReturn(authenticatedUser);

        resource = new AuthResource();
        resource.authContext = authContext;
    }

    @Test
    void meShouldReturnOkResponseWithCurrentUserData() {
        Response response = resource.me();

        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        assertInstanceOf(UserResponseDto.class, response.getEntity());

        UserResponseDto dto = (UserResponseDto) response.getEntity();
        assertEquals(authenticatedUser.getId(), dto.getId());
        assertEquals("Gabriel", dto.getName());
        assertEquals("Gutiérrez", dto.getLastName());
        assertEquals("gabriel@test.com", dto.getEmail());
        assertEquals("ADMIN", dto.getRole());
        assertTrue(dto.isStatus());
    }

    @Test
    void meShouldMapRoleNameAndNotTheWholeRoleObject() {
        Response response = resource.me();
        UserResponseDto dto = (UserResponseDto) response.getEntity();

        // El DTO solo debe exponer el nombre del rol, no el objeto entero
        assertEquals(role.getName(), dto.getRole());
    }

    @Test
    void meShouldReadUserFromAuthContextExactlyOnce() {
        resource.me();

        verify(authContext, times(1)).getUser();
        verifyNoMoreInteractions(authContext);
    }

    @Test
    void meShouldReflectInactiveStatusFromAuthenticatedUser() {
        User inactivo = new User(
                UUID.randomUUID(),
                "Ana",
                "García",
                "ana@test.com",
                role,
                false,
                "firebase-uid-2");
        when(authContext.getUser()).thenReturn(inactivo);

        Response response = resource.me();
        UserResponseDto dto = (UserResponseDto) response.getEntity();

        assertFalse(dto.isStatus());
        assertEquals("ana@test.com", dto.getEmail());
    }
}
