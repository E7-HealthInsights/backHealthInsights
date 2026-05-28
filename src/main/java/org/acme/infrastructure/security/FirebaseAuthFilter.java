package org.acme.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.acme.domain.models.User;
import org.acme.domain.repository.UserRepository;

import java.io.IOException;
import java.util.Optional;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class FirebaseAuthFilter implements ContainerRequestFilter {

    @Inject
    UserRepository userRepository;
    @Inject
    AuthContext authContext;
    @Inject
    CurrentIdentityAssociation identityAssociation;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path=requestContext.getUriInfo().getPath();
        System.out.println("Path: "+path);
//        if (path.equals("/users")){
//            return;   //si la ruta es /users, no se requiere autenticación (registro de usuario)
//        }

        if(path.startsWith("/q/")){
            return;
        }

        if(path.startsWith("/status")){
            return;
        }

        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            return;
        }

        String authHeader = requestContext.getHeaders().getFirst("Authorization");   //se obtiene el header de autorización de la solicitud

        if(authHeader == null){
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("No autorizado").build());
            return;  //si no hay header de autorización, se aborta la solicitud con un error
        }

        if(!authHeader.startsWith("Bearer ")){
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("No autorizado").build());
            return;  //si el header no tiene el formato correcto, se aborta la solicitud con un error
        }

        String token = authHeader.substring("Bearer ".length());  //se extrae el token del header de autorización

        try{
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token, true);
            // firebase dice: token valido, usuario real
            Optional<User> userOptional = userRepository.findByFirebaseUuid(decodedToken.getUid());  //se busca el usuario en la base de datos usando el UUID de Firebase
            if(userOptional.isEmpty()) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("No autorizado").build());
                return;  //si no se encuentra el usuario en la base de datos, se aborta la solicitud con un error
            }

            User user = userOptional.get();
            authContext.setUser(user);

            //security context
            //requestContext.setSecurityContext(new FirebaseSecurityContext(user));

            identityAssociation.setIdentity(Uni.createFrom().item(new FirebaseSecurityIdentity(user)));


            //si se encuentra el usuario, se establece en el contexto de autenticación para que esté disponible en los recursos protegidos
        } catch (FirebaseAuthException e){
            System.out.println("FirebaseAuthException: " + e.getAuthErrorCode() + " - " + e.getMessage());
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("No autorizado").build());
        };
    }
}
