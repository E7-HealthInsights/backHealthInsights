package org.acme.application.usecase;

import org.acme.domain.models.Dataset;
import org.acme.domain.models.DatasetEstado;
import org.acme.domain.repository.DatasetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetDatasetsUseCaseTest {

    private DatasetRepository datasetRepository;
    private GetDatasetsUseCase useCase;

    @BeforeEach
    void setUp() {
        datasetRepository = mock(DatasetRepository.class);
        useCase = new GetDatasetsUseCase(datasetRepository);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Dataset buildDataset(String nombre, DatasetEstado estado) {
        Dataset d = new Dataset();
        d.setId(UUID.randomUUID());
        d.setNombre(nombre);
        d.setNombreTabla(nombre.toLowerCase().replace(" ", "_"));
        d.setDescripcion("Descripción de " + nombre);
        d.setFuente("SINAVE");
        d.setEstado(estado);
        d.setFechaActualizacion(LocalDateTime.now());
        return d;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void executeShouldReturnAllDatasets() {
        Dataset d1 = buildDataset("Diabetes México 2023", DatasetEstado.READY);
        Dataset d2 = buildDataset("Hipertensión 2022", DatasetEstado.INACTIVE);

        when(datasetRepository.findAllDatasets()).thenReturn(List.of(d1, d2));

        List<Dataset> result = useCase.execute();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Diabetes México 2023", result.get(0).getNombre());
        assertEquals("Hipertensión 2022", result.get(1).getNombre());
        verify(datasetRepository, times(1)).findAllDatasets();
    }

    @Test
    void executeShouldReturnEmptyListWhenNoDatasetsExist() {
        when(datasetRepository.findAllDatasets()).thenReturn(List.of());

        List<Dataset> result = useCase.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(datasetRepository, times(1)).findAllDatasets();
    }

    @Test
    void executeShouldDelegateFetchingToRepository() {
        Dataset d1 = buildDataset("Obesidad 2021", DatasetEstado.READY);
        when(datasetRepository.findAllDatasets()).thenReturn(List.of(d1));

        List<Dataset> result = useCase.execute();

        assertEquals(1, result.size());
        verify(datasetRepository, times(1)).findAllDatasets();
        verifyNoMoreInteractions(datasetRepository);
    }

    @Test
    void executeShouldPreserveDatasetFields() {
        Dataset d = buildDataset("Diabetes México 2023", DatasetEstado.READY);
        d.setFuente("SINAVE / Secretaría de Salud");
        d.setLink("https://sinave.gob.mx");

        when(datasetRepository.findAllDatasets()).thenReturn(List.of(d));

        List<Dataset> result = useCase.execute();

        Dataset returned = result.get(0);
        assertEquals(d.getId(),          returned.getId());
        assertEquals(d.getNombre(),      returned.getNombre());
        assertEquals(d.getNombreTabla(), returned.getNombreTabla());
        assertEquals(d.getFuente(),      returned.getFuente());
        assertEquals(d.getLink(),        returned.getLink());
    }
}
