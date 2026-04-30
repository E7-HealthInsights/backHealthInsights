package org.acme.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.acme.application.dto.UserResponseDto;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;
import org.acme.interfaces.rest.AuthResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.smallrye.mutiny.Uni;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de integración del flujo de inicio de sesión.
 * Conecta el FirebaseAuthFilter, el AuthContext y el AuthResource con instancias reales,
 * mockeando solo las dependencias externas (FirebaseAuth, UserRepository, CurrentIdentityAssociation).
 */
class LoginIntegrationTest {

    private UserRepository userRepository;
    private CurrentIdentityAssociation identityAssociation;
    private AuthContext authContext;

    private FirebaseAuthFilter filter;
    private AuthResource resource;

    private ContainerRequestContext requestContext;
    private UriInfo uriInfo;
    private MultivaluedMap<String, String> headers;

    private MockedStatic<FirebaseAuth> firebaseAuthStatic;
    private FirebaseAuth firebaseAuth;

    private User existingUser;
    private Role role;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        identityAssociation = mock(CurrentIdentityAssociation.class);
        authContext = new AuthContext();

        filter = new FirebaseAuthFilter();
        filter.userRepository = userRepository;
        filter.authContext = authContext;
        filter.identityAssociation = identityAssociation;

        resource = new AuthResource();
        injectAuthContext(resource, authContext);

        requestContext = mock(ContainerRequestContext.class);
        uriInfo = mock(UriInfo.class);
        headers = new MultivaluedHashMap<>();

        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getHeaders()).thenReturn(headers);
        when(uriInfo.getPath()).thenReturn("/auth/me");
        when(requestContext.getMethod()).thenReturn("GET");

        firebaseAuth = mock(FirebaseAuth.class);
        firebaseAuthStatic = mockStatic(FirebaseAuth.class);
        firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

        role = new Role((byte) 1, "ADMIN");
        existingUser = new User(
                UUID.randomUUID(),
                "Gabriel",
                "Gutiérrez",
                "gabriel@test.com",
                role,
                true,
                "firebase-uid-1");
    }

    @AfterEach
    void tearDown() {
        firebaseAuthStatic.close();
    }

    @Test
    void loginFlowShouldReturnAuthenticatedUserWhenTokenAndUserAreValid() throws Exception {
        headers.add("Authorization", "Bearer valid-token");

        FirebaseToken decoded = mock(FirebaseToken.class);
        when(decoded.getUid()).thenReturn("firebase-uid-1");
        when(firebaseAuth.verifyIdToken("valid-token", true)).thenReturn(decoded);
        when(userRepository.findByFirebaseUuid("firebase-uid-1")).thenReturn(Optional.of(existingUser));

        // 1. Pasa por el filter
        filter.filter(requestContext);

        // El filter no aborta y deja al user en el contexto
        verify(requestContext, never()).abortWith(any());
        assertEquals(existingUser, authContext.getUser());

        // 2. Llega al resource
        Response response = resource.me();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        UserResponseDto dto = (UserResponseDto) response.getEntity();
        assertEquals(existingUser.getId(), dto.getId());
        assertEquals("Gabriel", dto.getName());
        assertEquals("Gutiérrez", dto.getLastName());
        assertEquals("gabriel@test.com", dto.getEmail());
        assertEquals("ADMIN", dto.getRole());
        assertTrue(dto.isStatus());
    }

    @Test
    void loginFlowShouldReturnUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
        // headers vacíos

        filter.filter(requestContext);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext, times(1)).abortWith(captor.capture());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), captor.getValue().getStatus());
        assertNull(authContext.getUser());
        verify(userRepository, never()).findByFirebaseUuid(anyString());
    }

    @Test
    void loginFlowShouldReturnUnauthorizedWhenFirebaseTokenIsInvalid() throws Exception {
        headers.add("Authorization", "Bearer token-invalido");

        when(firebaseAuth.verifyIdToken("token-invalido", true))
                .thenThrow(mock(FirebaseAuthException.class));

        filter.filter(requestContext);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext, times(1)).abortWith(captor.capture());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), captor.getValue().getStatus());
        assertNull(authContext.getUser());
    }

    @Test
    void loginFlowShouldReturnUnauthorizedWhenUserIsNotRegisteredInDatabase() throws Exception {
        headers.add("Authorization", "Bearer valid-token");

        FirebaseToken decoded = mock(FirebaseToken.class);
        when(decoded.getUid()).thenReturn("uid-no-registrado");
        when(firebaseAuth.verifyIdToken("valid-token", true)).thenReturn(decoded);
        when(userRepository.findByFirebaseUuid("uid-no-registrado")).thenReturn(Optional.empty());

        filter.filter(requestContext);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext, times(1)).abortWith(captor.capture());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), captor.getValue().getStatus());
        assertNull(authContext.getUser());
        verify(identityAssociation, never()).setIdentity(any(Uni.class));
    }

    @Test
    void loginFlowShouldNotLeakRoleObjectInResponse() throws Exception {
        headers.add("Authorization", "Bearer valid-token");

        FirebaseToken decoded = mock(FirebaseToken.class);
        when(decoded.getUid()).thenReturn("firebase-uid-1");
        when(firebaseAuth.verifyIdToken("valid-token", true)).thenReturn(decoded);
        when(userRepository.findByFirebaseUuid("firebase-uid-1")).thenReturn(Optional.of(existingUser));

        filter.filter(requestContext);
        Response response = resource.me();
        UserResponseDto dto = (UserResponseDto) response.getEntity();

        // El DTO solo expone el nombre del rol, nunca el providerId ni el objeto Role completo
        assertEquals(role.getName(), dto.getRole());
    }

    // El campo authContext de AuthResource es package-private (otro package que el de este test),
    // así que se inyecta por reflexión solo para esta prueba de integración.
    private static void injectAuthContext(AuthResource resource, AuthContext authContext) {
        try {
            Field field = AuthResource.class.getDeclaredField("authContext");
            field.setAccessible(true);
            field.set(resource, authContext);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
