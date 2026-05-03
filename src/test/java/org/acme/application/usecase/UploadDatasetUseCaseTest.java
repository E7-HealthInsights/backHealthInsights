package org.acme.application.usecase;

import org.acme.application.dto.ColumnDefinitionDto;
import org.acme.application.dto.UploadDatasetDto;
import org.acme.domain.exception.TableAlreadyExistsException;
import org.acme.domain.models.Dataset;
import org.acme.domain.models.Metrica;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.MetricaRepository;
import org.acme.infrastructure.csv.CsvIngestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UploadDatasetUseCaseTest {

    private DatasetRepository  datasetRepository;
    private MetricaRepository  metricaRepository;
    private CsvIngestService   csvIngestService;
    private UploadDatasetUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        datasetRepository = mock(DatasetRepository.class);
        metricaRepository = mock(MetricaRepository.class);
        csvIngestService  = mock(CsvIngestService.class);

        // Por defecto: la tabla no existe y save devuelve lo que recibe
        when(datasetRepository.existsByNombreTabla(anyString())).thenReturn(false);
        when(datasetRepository.save(any(Dataset.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(metricaRepository).saveAll(anyList());
        doNothing().when(csvIngestService)
                .crearTablaEInsertarDatos(anyString(), anyList(), any());

        useCase = new UploadDatasetUseCase(datasetRepository, metricaRepository, csvIngestService);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UploadDatasetDto buildDto(String nombre, String archivoNombre,
                                      List<ColumnDefinitionDto> columnas) {
        String csv = "col1,col2\nval1,val2\n";
        String b64 = Base64.getEncoder().encodeToString(csv.getBytes());

        UploadDatasetDto dto = new UploadDatasetDto();
        dto.setNombre(nombre);
        dto.setDescripcion("Descripción de prueba");
        dto.setFuente("INEGI");
        dto.setArchivoNombre(archivoNombre);
        dto.setArchivoCsvBase64(b64);
        dto.setColumnas(columnas);
        return dto;
    }

    private ColumnDefinitionDto col(String original, String display, String sqlType) {
        ColumnDefinitionDto c = new ColumnDefinitionDto();
        c.setOriginalName(original);
        c.setDisplayName(display);
        c.setSqlType(sqlType);
        return c;
    }

    // ── Tests: flujo feliz ────────────────────────────────────────────────────

    @Test
    void executeShouldPersistDatasetWithCorrectFields() {
        var dto = buildDto("ENSANUT 2024", "ensanut_2024.csv",
                List.of(col("estado", "Estado", "VARCHAR(255)")));

        Dataset result = useCase.execute(dto);

        assertNotNull(result.getId());
        assertEquals("ENSANUT 2024",    result.getNombre());
        assertEquals("ensanut_2024",    result.getNombreTabla());
        assertEquals("INEGI",           result.getFuente());
        assertEquals("ensanut_2024.csv",result.getArchivoCsv());
        assertTrue(result.isEstado());
        assertNotNull(result.getFechaActualizacion());
    }

    @Test
    void executeShouldPersistOneMetricaPerColumn() throws Exception {
        var columnas = List.of(
                col("estado",   "Estado",   "VARCHAR(255)"),
                col("casos",    "Casos",    "INT"),
                col("porcentaje","Porcentaje","DECIMAL(10,2)")
        );
        var dto = buildDto("Test Dataset", "test.csv", columnas);

        useCase.execute(dto);

        // saveAll recibe exactamente 3 métricas
        verify(metricaRepository, times(1)).saveAll(argThat(list -> list.size() == 3));
    }

    @Test
    void executeShouldMapDisplayNameToMetricaNombre() {
        var dto = buildDto("Test", "test.csv",
                List.of(col("edad_promedio", "Edad Promedio", "DECIMAL(10,2)")));

        useCase.execute(dto);

        verify(metricaRepository).saveAll(argThat((List<Metrica> list) -> {
            Metrica m = list.get(0);
            return "Edad Promedio".equals(m.getNombre())
                    && "edad_promedio".equals(m.getColumnaCsv());
        }));
    }

    @Test
    void executeShouldGenerateUniqueDatasetIds() {
        var dto1 = buildDto("Dataset A", "a.csv", List.of(col("x", "X", "INT")));
        var dto2 = buildDto("Dataset B", "b.csv", List.of(col("y", "Y", "INT")));

        when(datasetRepository.existsByNombreTabla("a")).thenReturn(false);
        when(datasetRepository.existsByNombreTabla("b")).thenReturn(false);

        Dataset r1 = useCase.execute(dto1);
        Dataset r2 = useCase.execute(dto2);

        assertNotEquals(r1.getId(), r2.getId());
    }

    @Test
    void executeShouldCallCsvIngestWithCorrectTableName() throws Exception {
        var dto = buildDto("Mi Dataset", "mis_datos_2024.csv",
                List.of(col("col", "Col", "VARCHAR(255)")));

        useCase.execute(dto);

        verify(csvIngestService).crearTablaEInsertarDatos(
                eq("mis_datos_2024"), anyList(), any()
        );
    }

    // ── Tests: slugify ────────────────────────────────────────────────────────

    @Test
    void executeShouldSlugifyFileNameWithSpaces() {
        var dto = buildDto("X", "ENSANUT 2024 Final.csv", List.of(col("c","C","INT")));
        Dataset result = useCase.execute(dto);
        assertEquals("ensanut_2024_final", result.getNombreTabla());
    }

    @Test
    void executeShouldSlugifyFileNameWithSpecialChars() {
        var dto = buildDto("X", "datos(2023).csv", List.of(col("c","C","INT")));
        Dataset result = useCase.execute(dto);
        assertEquals("datos_2023", result.getNombreTabla());
    }

    @Test
    void executeShouldSlugifyFileNameWithoutExtension() {
        var dto = buildDto("X", "reporte_final.csv", List.of(col("c","C","INT")));
        Dataset result = useCase.execute(dto);
        // la extensión .csv se quita
        assertFalse(result.getNombreTabla().endsWith("csv"));
        assertEquals("reporte_final", result.getNombreTabla());
    }

    // ── Tests: validaciones ───────────────────────────────────────────────────

    @Test
    void executeShouldThrowWhenTableAlreadyExists() {
        when(datasetRepository.existsByNombreTabla("duplicado")).thenReturn(true);
        var dto = buildDto("X", "duplicado.csv", List.of(col("c","C","INT")));

        assertThrows(TableAlreadyExistsException.class, () -> useCase.execute(dto));
        verify(datasetRepository, never()).save(any());
        verify(metricaRepository, never()).saveAll(anyList());
    }

    @Test
    void executeShouldThrowWhenBase64IsInvalid() {
        UploadDatasetDto dto = new UploadDatasetDto();
        dto.setNombre("X");
        dto.setArchivoNombre("test.csv");
        dto.setArchivoCsvBase64("esto-no-es-base64!!!");
        dto.setColumnas(List.of(col("c","C","INT")));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(dto));
    }

    @Test
    void executeShouldThrowAndNotSaveWhenCsvIngestFails() throws Exception {
        doThrow(new SQLException("Error de BD"))
                .when(csvIngestService).crearTablaEInsertarDatos(anyString(), anyList(), any());

        var dto = buildDto("X", "test.csv", List.of(col("c","C","INT")));

        assertThrows(RuntimeException.class, () -> useCase.execute(dto));
    }

    @Test
    void executeShouldNotCallCsvIngestWhenTableExists() throws Exception {
        when(datasetRepository.existsByNombreTabla(anyString())).thenReturn(true);
        var dto = buildDto("X", "test.csv", List.of(col("c","C","INT")));

        assertThrows(TableAlreadyExistsException.class, () -> useCase.execute(dto));
        verify(csvIngestService, never()).crearTablaEInsertarDatos(any(), any(), any());
    }
}
