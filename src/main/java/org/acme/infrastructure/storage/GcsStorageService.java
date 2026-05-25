package org.acme.infrastructure.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Servicio de infraestructura para Google Cloud Storage.
 *
 * Responsabilidades:
 *  - Subir el CSV temporal al bucket antes de publicar el mensaje Kafka.
 *  - Descargar el CSV en el consumer para procesarlo.
 *  - Borrar el archivo del bucket una vez que el ingest termina (éxito o error).
 *
 * Configuración requerida (application.properties o variables de entorno):
 *  gcs.bucket.name    → nombre del bucket (ej: health-insights-csv-uploads)
 *
 * Autenticación:
 *  - En Cloud Run: usa la Service Account del servicio automáticamente.
 *  - En local: exporta GOOGLE_APPLICATION_CREDENTIALS=/ruta/a/sa-key.json
 */
@ApplicationScoped
public class GcsStorageService {

    private static final Logger LOG = Logger.getLogger(GcsStorageService.class);

    @ConfigProperty(name = "gcs.bucket.name")
    String bucketName;

    private Storage storage;

    @PostConstruct
    void init() {
        // Application Default Credentials: en Cloud Run usa la SA del servicio.
        // En local requiere GOOGLE_APPLICATION_CREDENTIALS o gcloud auth application-default login.
        this.storage = StorageOptions.getDefaultInstance().getService();
        LOG.infof("GcsStorageService inicializado — bucket: %s", bucketName);
    }

    /**
     * Sube los bytes del CSV al bucket.
     *
     * @param objectName nombre del objeto dentro del bucket (ej: "datasets/uuid-archivo.csv")
     * @param csvBytes   contenido del CSV en bytes
     * @return la URI gs:// del archivo subido
     */
    public String upload(String objectName, byte[] csvBytes) {
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("text/csv")
                .build();

        storage.create(blobInfo, csvBytes);

        String gsUri = "gs://" + bucketName + "/" + objectName;
        LOG.infof("CSV subido a GCS: %s (%d bytes)", gsUri, csvBytes.length);
        return gsUri;
    }

    /**
     * Descarga el contenido del archivo como InputStream.
     *
     * @param objectName nombre del objeto dentro del bucket
     * @return InputStream con el contenido del CSV
     */
    public InputStream download(String objectName) {
        BlobId blobId = BlobId.of(bucketName, objectName);
        byte[] bytes = storage.readAllBytes(blobId);
        LOG.infof("CSV descargado de GCS: gs://%s/%s (%d bytes)", bucketName, objectName, bytes.length);
        return new ByteArrayInputStream(bytes);
    }

    /**
     * Elimina el archivo del bucket.
     * Se llama al finalizar el ingest (éxito o error) para no dejar residuos.
     *
     * @param objectName nombre del objeto a eliminar
     */
    public void delete(String objectName) {
        BlobId blobId = BlobId.of(bucketName, objectName);
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            LOG.infof("CSV eliminado de GCS: gs://%s/%s", bucketName, objectName);
        } else {
            LOG.warnf("CSV no encontrado al intentar eliminar: gs://%s/%s", bucketName, objectName);
        }
    }

    /**
     * Extrae el nombre del objeto (objectName) a partir de una URI gs://.
     * Ejemplo: "gs://mi-bucket/datasets/archivo.csv" → "datasets/archivo.csv"
     */
    public static String objectNameFromUri(String gsUri) {
        // gsUri tiene forma gs://bucket-name/object/name
        int slashAfterBucket = gsUri.indexOf('/', 5); // salta "gs://"
        return gsUri.substring(slashAfterBucket + 1);
    }
}
