package org.acme.infrastructure.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FirebaseUserCreator {

    public UserRecord create(String email, String password) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest();   //request es el objeto que se le pasa a Firebase para crear el usuario
        request.setEmail(email);
        request.setPassword(password);
        request.setDisabled(false);  // para que el usuario se mantenga activo
        request.setEmailVerified(true);  // para que el usuario se mantenga verificado
        return FirebaseAuth.getInstance().createUser(request);  //Firebase responde con un uuid
    }

}
