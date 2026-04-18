package org.acme.infrastructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@Startup
@ApplicationScoped
public class FirebaseConfig {
    @ConfigProperty(name="firebase.credentials")   //es un json que es mi llave privada del servidor
    private String path;

    void onStart(@Observes StartupEvent ev) throws FileNotFoundException {
        try {
            if(FirebaseApp.getApps().isEmpty()){
                try(InputStream serviceAccount = new FileInputStream(path)){
                    FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
                    FirebaseApp.initializeApp(options);   //conexion backend-firebase
                }
            }
        } catch (Exception e){}
    }
}
