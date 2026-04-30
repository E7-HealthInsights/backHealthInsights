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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FirebaseAuthFilterTest {

    private UserRepository userRepository;
    private AuthContext authContext;
    private CurrentIdentityAssociation identityAssociation;

    private FirebaseAuthFilter filter;

    private ContainerRequestContext requestContext;
    private UriInfo uriInfo;
    private MultivaluedMap<String, String> headers;

    private MockedStatic<FirebaseAuth> firebaseAuthStatic;
    private FirebaseAuth firebaseAuth;

    private User existingUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authContext = new AuthContext();
        identityAssociation = mock(CurrentIdentityAssociation.class);

        filter = new FirebaseAuthFilter();
        filter.userRepository = userRepository;
        filter.authContext = authContext;
        filter.identityAssociation = identityAssociation;

        requestContext = mock(ContainerRequestContext.class);
        uriInfo = mock(UriInfo.class);
        headers = new MultivaluedHashMap<>();

        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getHeaders()).thenReturn(headers);

        firebaseAuth = mock(FirebaseAuth.class);
        firebaseAuthStatic = mockStatic(FirebaseAuth.class);
        firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

        Role role = new Role((byte) 1, "ADMIN");
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
    void filterShouldSkipAuthForQuarkusInternalPaths() throws IOException {
        when(uriInfo.getPath()).thenReturn("/q/health");
        when(requestContext.getMethod()).thenReturn("GET");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
        verify(userRepository, never()).findByFirebaseUuid(anyString());
        assertNull(authContext.getUser());
    }

    @Test
    void filterShouldSkipAuthForOptionsRequests() throws IOException {
        when(uriInfo.getPath()).thenReturn("/auth/me");
        when(requestContext.getMethod()).thenReturn("OPTIONS");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
        verify(userRepository, never()).findByFirebaseUuid(anyString());
    }

    @Test
    void filterShouldAbortWithUnauthorizedWhenNoAuthorizationHeader() throws IOException {
        when(uriInfo.getPath()).thenReturn("/auth/me");
        when(requestContext.getMethod()).thenReturn("GET");
        // headers vacíos → no hay Authorization

        filter.filter(requestContext);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext, times(1)).abortWith(captor.capture());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), captor.getValue().getStatus());
        verify(userRepository, never()).findByFirebaseUuid(anyString());
    }

    @Test
    void filterShouldAbortWithUnauthorizedWhenAuthorizationHeaderIsNotBearer() throws IOException {
        when(uriInfo.getPath()).thenReturn("/auth/me");
        when(requestContext.getMethod()).thenReturn("GET");
        headers.add("Authorization", "Basic abc123");

        filter.filter(requestContext);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext, times(1)).abortWith(captor.capture());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), captor.getValue().getStatus());
        verify(userRepository, never()).findByFirebaseUuid(anyString());
    }

    @Test
    void filterShouldSetAuthContextAndIdentityWhenTokenIsValidAndUserExists() throws Exception {
        when(uriInfo.getPath()).thenReturn("/auth/me");
        when(requestContext.getMethod()).thenReturn("GET");
        headers.add("Authorization", "Bearer valid-token");

        FirebaseToken decodedToken = mock(FirebaseToken.class);
        when(decodedToken.getUid()).thenReturn("firebase-uid-1");
        when(firebaseAuth.verifyIdToken("valid-token", true)).thenReturn(decodedToken);
        when(userRepository.findByFirebaseUuid("firebase-uid-1")).thenReturn(Optional.of(existingUser));

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
        assertEquals(existingUser, authContext.getUser());
        verify(identityAssociation, times(1)).setIdentity(any(Uni.class));
    }

    @Test
    void filterShouldAbortWithUnauthorizedWhenFirebaseTokenIsValidButUserNotInDatabase() throws Exception {
        when(uriInfo.getPath()).thenReturn("/auth/me");
        when(requestContext.getMethod()).thenReturn("GET");
        headers.add("Authorization", "Bearer valid-token");

        FirebaseToken decodedToken = mock(FirebaseToken.class);
        when(decodedToken.getUid()).thenReturn("uid-desconocido");
        when(firebaseAuth.verifyIdToken("valid-token", true)).thenReturn(decodedToken);
        when(userRepository.findByFirebaseUuid("uid-desconocido")).thenReturn(Optional.empty());

        filter.filter(requestContext);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext, times(1)).abortWith(captor.capture());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), captor.getValue().getStatus());
        assertNull(authContext.getUser());
        verify(identityAssociation, never()).setIdentity(any(Uni.class));
    }

    @Test
    void filterShouldAbortWithUnauthorizedWhenFirebaseRejectsToken() throws Exception {
        when(uriInfo.getPath()).thenReturn("/auth/me");
        when(requestContext.getMethod()).thenReturn("GET");
        headers.add("Authorization", "Bearer token-invalido");

        when(firebaseAuth.verifyIdToken("token-invalido", true))
                .thenThrow(mock(FirebaseAuthException.class));

        filter.filter(requestContext);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext, times(1)).abortWith(captor.capture());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), captor.getValue().getStatus());
        assertNull(authContext.getUser());
        verify(userRepository, never()).findByFirebaseUuid(anyString());
    }

    @Test
    void filterShouldExtractTokenWithoutBearerPrefix() throws Exception {
        when(uriInfo.getPath()).thenReturn("/auth/me");
        when(requestContext.getMethod()).thenReturn("GET");
        headers.add("Authorization", "Bearer abc.def.ghi");

        FirebaseToken decodedToken = mock(FirebaseToken.class);
        when(decodedToken.getUid()).thenReturn("firebase-uid-1");
        when(firebaseAuth.verifyIdToken(anyString(), eq(true))).thenReturn(decodedToken);
        when(userRepository.findByFirebaseUuid("firebase-uid-1")).thenReturn(Optional.of(existingUser));

        filter.filter(requestContext);

        // Verifica que NO se le pase el prefijo "Bearer " a Firebase
        verify(firebaseAuth, times(1)).verifyIdToken("abc.def.ghi", true);
        verify(firebaseAuth, never()).verifyIdToken(eq("Bearer abc.def.ghi"), anyBoolean());
    }
}
