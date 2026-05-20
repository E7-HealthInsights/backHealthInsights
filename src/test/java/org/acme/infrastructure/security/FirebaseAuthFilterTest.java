package org.acme.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.acme.domain.models.Role;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link FirebaseAuthFilter}.
 *
 * Valida los casos de rechazo (401) que no se pueden probar en
 * AuthResourceTest porque el entorno @QuarkusTest reemplaza el filtro
 * real con TestFirebaseAuthFilter.
 *
 * Se usa Mockito para simular FirebaseAuth (estática) y el contexto de
 * petición, evitando dependencias externas.
 */
class FirebaseAuthFilterTest {

    private FirebaseAuthFilter filter;
    private ContainerRequestContext requestContext;
    private UserRepository userRepository;
    private AuthContext authContext;
    private CurrentIdentityAssociation identityAssociation;
    private MultivaluedMap<String, String> headers;

    // Usuario válido para los casos de éxito
    private final User validUser = new User(
            UUID.randomUUID(), "Test", "User",
            "test@test.com", new Role((byte) 1, "ADMIN"),
            true, "firebase-uid-123"
    );

    @BeforeEach
    void setUp() {
        userRepository        = mock(UserRepository.class);
        authContext           = mock(AuthContext.class);
        identityAssociation   = mock(CurrentIdentityAssociation.class);
        requestContext        = mock(ContainerRequestContext.class);

        filter = new FirebaseAuthFilter();
        // Inyección directa (campos package-private, mismo paquete)
        filter.userRepository      = userRepository;
        filter.authContext         = authContext;
        filter.identityAssociation = identityAssociation;

        // Configuración base del contexto de petición
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/auth/me");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");

        headers = new MultivaluedHashMap<>();
        when(requestContext.getHeaders()).thenReturn(headers);

    }

    // ── Rutas excluidas del filtro ────────────────────────────────────────────

    @Test
    void filterShouldSkipQuarkusInternalRoutes() throws Exception {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/q/health");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filterShouldSkipStatusRoute() throws Exception {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/status");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filterShouldSkipOptionsRequests() throws Exception {
        when(requestContext.getMethod()).thenReturn("OPTIONS");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    // ── Casos de 401: header ausente o malformado ─────────────────────────────
    // Estos casos se resuelven ANTES de llamar a Firebase, por lo que
    // no se necesita mockear FirebaseAuth.

    @Test
    void filterShouldReturn401WhenNoAuthorizationHeader() throws Exception {
        // headers vacíos → getFirst devuelve null
        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(r ->
                r.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
        ));
    }

    @Test
    void filterShouldReturn401WhenAuthorizationHeaderHasNoBearer() throws Exception {
        headers.putSingle("Authorization", "token-sin-bearer");

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(r ->
                r.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
        ));
    }

    @Test
    void filterShouldReturn401WhenAuthorizationHeaderIsEmpty() throws Exception {
        headers.putSingle("Authorization", "");

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(r ->
                r.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
        ));
    }

    // ── Casos con token: requieren mockear FirebaseAuth ───────────────────────

    @Test
    void filterShouldReturn401WhenFirebaseTokenIsInvalid() throws Exception {
        headers.putSingle("Authorization", "Bearer token-invalido");

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            when(mockAuth.verifyIdToken(anyString(), anyBoolean()))
                    .thenThrow(mock(FirebaseAuthException.class));

            filter.filter(requestContext);

            verify(requestContext).abortWith(argThat(r ->
                    r.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
            ));
        }
    }

    @Test
    void filterShouldReturn401WhenUserNotFoundInDatabase() throws Exception {
        headers.putSingle("Authorization", "Bearer token-valido");

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            FirebaseToken mockToken = mock(FirebaseToken.class);
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            when(mockAuth.verifyIdToken(anyString(), anyBoolean())).thenReturn(mockToken);
            when(mockToken.getUid()).thenReturn("uid-sin-usuario");
            when(userRepository.findByFirebaseUuid("uid-sin-usuario"))
                    .thenReturn(Optional.empty());

            filter.filter(requestContext);

            verify(requestContext).abortWith(argThat(r ->
                    r.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
            ));
        }
    }

    @Test
    void filterShouldSetAuthContextWhenTokenAndUserAreValid() throws Exception {
        headers.putSingle("Authorization", "Bearer token-valido");

        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            FirebaseToken mockToken = mock(FirebaseToken.class);
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            when(mockAuth.verifyIdToken(anyString(), anyBoolean())).thenReturn(mockToken);
            when(mockToken.getUid()).thenReturn("firebase-uid-123");
            when(userRepository.findByFirebaseUuid("firebase-uid-123"))
                    .thenReturn(Optional.of(validUser));

            filter.filter(requestContext);

            verify(authContext).setUser(validUser);
            verify(requestContext, never()).abortWith(any());
        }
    }
}