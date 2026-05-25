package org.acme.infrastructure.messaging;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

/**
 * Deserializador Kafka para {@link DatasetCsvUploadedEvent}.
 *
 * Extiende {@link ObjectMapperDeserializer} de Quarkus, que usa el
 * ObjectMapper CDI ya configurado en la aplicación — no hace falta
 * configuración extra.
 *
 * Se referencia en application.properties:
 *   mp.messaging.incoming.dataset-csv-uploaded-in.value.deserializer=
 *       org.acme.infrastructure.messaging.DatasetCsvUploadedEventDeserializer
 */
public class DatasetCsvUploadedEventDeserializer
        extends ObjectMapperDeserializer<DatasetCsvUploadedEvent> {

    public DatasetCsvUploadedEventDeserializer() {
        super(DatasetCsvUploadedEvent.class);
    }
}
