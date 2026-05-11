package org.acme.application.usecase;

import org.acme.application.dto.WidgetResponseDto;
import org.acme.domain.models.*;
import org.acme.domain.repository.DatasetRepository;
import org.acme.domain.repository.MetricaRepository;
import org.acme.domain.repository.WidgetRepository;
import org.acme.infrastructure.query.QueryExecutor;
import org.acme.infrastructure.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetUserWidgetsUseCaseTest {

    private WidgetRepository widgetRepository;
    private AuthContext authContext;
    private GetUserWidgetsUseCase useCase;
    private User authenticatedUser;
    private Role role;
    private Dataset datasetMock;
    private QueryExecutor queryExecutor;
    private DatasetRepository datasetRepository;
    private MetricaRepository metricaRepository;

    @BeforeEach
    void setUp() {
        widgetRepository = mock(WidgetRepository.class);
        authContext = mock(AuthContext.class);
        queryExecutor = mock(QueryExecutor.class);
        datasetRepository = mock(DatasetRepository.class);
        metricaRepository = mock(MetricaRepository.class);


        role = new Role((byte) 3, "DIRECTOR_FINANZAS");
        authenticatedUser = new User(
                UUID.randomUUID(), "Test", "Testt", "test@test.com", role, true, "firebase-uid");

        when(authContext.getUser()).thenReturn(authenticatedUser);

        // Por defecto los defaults y personales regresan vacío
        when(widgetRepository.findDefaultsByRolId(any())).thenReturn(List.of());
        when(widgetRepository.findByUserId(any())).thenReturn(List.of());

        // QueryExecutor retorna data simulada por defecto
        when(queryExecutor.execute(anyString(), anyString()))
                .thenReturn(Map.of("value", 42));

        // Dataset mock por defecto
        datasetMock = new Dataset();
        datasetMock.setId(UUID.randomUUID());
        datasetMock.setFuente("IMSS");
        when(datasetRepository.findByNombreTabla(anyString()))
                .thenReturn(Optional.of(datasetMock));

        // Métrica no encontrada por defecto
        when(metricaRepository.findByColumnaCsvAndDatasetId(anyString(), any()))
                .thenReturn(Optional.empty());

        useCase = new GetUserWidgetsUseCase(widgetRepository, authContext, queryExecutor, datasetRepository, metricaRepository);
    }

    @Test
    void executeShouldReturnWidgetsBelongingToUserAndRole() {
        UUID userId = authenticatedUser.getId();

        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");

        Widget wPersonal = new Widget(UUID.randomUUID(), "Widget personal", authenticatedUser, tipo,
                "{\"tabla\":\"f4_pib_bancomundial\",\"colX\":\"time_period\",\"colY\":\"obs_value\",\"funcion\":\"AVG\",\"groupBy\":\"time_period\"}",
                1, null);

        Widget wDefault = new Widget(UUID.randomUUID(), "Widget de rol", null, tipo,
                "{\"tabla\":\"f11_health_coverage_oecd\",\"colLabel\":\"insurance_type\",\"colValue\":\"obs_value\",\"funcion\":\"AVG\"}",
                1, (byte) 3);

        when(widgetRepository.findByUserId(userId)).thenReturn(List.of(wPersonal));
        when(widgetRepository.findDefaultsByRolId((byte) 3)).thenReturn(List.of(wDefault));


        List<WidgetResponseDto> result = useCase.execute();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Widget de rol", result.get(0).getTitulo());
        assertEquals("Widget personal", result.get(1).getTitulo());
        verify(widgetRepository, times(1)).findByUserId(userId);  // verifica que se llamó al repo con el id correcto
    }

    @Test
    void executeShouldOnlyQueryByTheProvidedUserId() {
        UUID userId = authenticatedUser.getId();
        UUID otroUserId = UUID.randomUUID();  // otro usuario

        when(widgetRepository.findByUserId(userId)).thenReturn(List.of());
        when(widgetRepository.findByUserId(otroUserId)).thenReturn(List.of(
                new Widget(UUID.randomUUID(), "Widget ajeno", null, null, "query", 1, null)
        ));

        List<WidgetResponseDto> result = useCase.execute();

        // el resultado está vacío porque el usuario autenticado no tiene widgets
        assertTrue(result.isEmpty());
        // nunca se consultó por el otro usuario
        verify(widgetRepository, never()).findByUserId(otroUserId);
    }

    @Test
    void executeShouldReturnEmptyListWhenUserHasNoWidgets() {
        List<WidgetResponseDto> result = useCase.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void executeShouldCallQueryExecutorForEachWidget() {
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        String queryJson = "{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"SUM\",\"columna\":\"detecciones\"}";

        Widget w1 = new Widget(UUID.randomUUID(), "Widget 1", authenticatedUser, tipo, queryJson, 1, null);
        Widget w2 = new Widget(UUID.randomUUID(), "Widget 2", authenticatedUser, tipo, queryJson, 2, null);

        when(widgetRepository.findByUserId(authenticatedUser.getId()))
                .thenReturn(List.of(w1, w2));

        useCase.execute();

        // Verifica que el executor se llamó una vez por cada widget
        verify(queryExecutor, times(2)).execute(queryJson, "STAT");
    }

    @Test
    void executeShouldNeverQueryAnotherUsersWidgets() {
        UUID otroUserId = UUID.randomUUID();

        when(widgetRepository.findByUserId(otroUserId)).thenReturn(List.of(
                new Widget(UUID.randomUUID(), "Widget ajeno", null, null, "{}", 1, null)
        ));

        List<WidgetResponseDto> result = useCase.execute();

        assertTrue(result.isEmpty());
        verify(widgetRepository, never()).findByUserId(otroUserId);
    }

    @Test
    void executeShouldIncludeDataFromQueryExecutorInResponse() {
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        String queryJson = "{\"tabla\":\"idf_diabetes_atlas\",\"funcion\":\"MAX\",\"columna\":\"expenditure_per_person_usd\"}";

        Widget widget = new Widget(UUID.randomUUID(), "Gasto per cápita", authenticatedUser, tipo, queryJson, 1, null);
        when(widgetRepository.findByUserId(authenticatedUser.getId())).thenReturn(List.of(widget));
        when(queryExecutor.execute(queryJson, "STAT")).thenReturn(Map.of("value", 1438));

        List<WidgetResponseDto> result = useCase.execute();

        assertEquals(1, result.size());
        assertEquals(1438, result.get(0).getData().get("value"));
    }

    @Test
    void executeShouldSetSubtituloFromDatasetFuente() {
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        String query = "{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"SUM\",\"columna\":\"detecciones\"}";

        datasetMock.setFuente("IMSS");
        when(datasetRepository.findByNombreTabla("imss_deteccion_diabetes"))
                .thenReturn(Optional.of(datasetMock));

        Widget widget = new Widget(UUID.randomUUID(), "Widget", authenticatedUser, tipo, query, 1, null);
        when(widgetRepository.findByUserId(authenticatedUser.getId()))
                .thenReturn(List.of(widget));

        List<WidgetResponseDto> result = useCase.execute();

        assertEquals("Fuente: IMSS", result.get(0).getSubtitulo());
    }

    @Test
    void executeShouldLeaveSubtituloNullWhenDatasetNotFound() {
        when(datasetRepository.findByNombreTabla(anyString())).thenReturn(Optional.empty());

        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        String query = "{\"tabla\":\"tabla_inexistente\",\"funcion\":\"COUNT\",\"columna\":\"id\"}";
        Widget widget = new Widget(UUID.randomUUID(), "Widget", authenticatedUser, tipo, query, 1, null);
        when(widgetRepository.findByUserId(authenticatedUser.getId()))
                .thenReturn(List.of(widget));

        List<WidgetResponseDto> result = useCase.execute();

        assertNull(result.get(0).getSubtitulo()); // no explota, solo deja null
    }

    @Test
    void executeShouldAddLabelToDataWhenMetricaHasUnidad() {
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        String query = "{\"tabla\":\"f10_gasto_diabetes\",\"funcion\":\"MAX\",\"columna\":\"gastomillones_de_dolares\"}";

        Metrica metrica = new Metrica();
        metrica.setNombre("Gasto en diabetes");
        metrica.setUnidad("Dolares");

        when(datasetRepository.findByNombreTabla("f10_gasto_diabetes"))
                .thenReturn(Optional.of(datasetMock));
        when(metricaRepository.findByColumnaCsvAndDatasetId("gastomillones_de_dolares", datasetMock.getId()))
                .thenReturn(Optional.of(metrica));
        when(queryExecutor.execute(query, "STAT"))
                .thenReturn(new HashMap<>(Map.of("value", 407209.0)));

        Widget widget = new Widget(UUID.randomUUID(), "Gasto diabetes",
                authenticatedUser, tipo, query, 1, null);
        when(widgetRepository.findByUserId(authenticatedUser.getId()))
                .thenReturn(List.of(widget));

        List<WidgetResponseDto> result = useCase.execute();

        assertEquals("Dolares", result.get(0).getData().get("label"));
        assertEquals(407209.0,  result.get(0).getData().get("value"));
    }

    @Test
    void executeShouldNotAddLabelWhenUnidadIsNull() {
        TipoWidget tipo = new TipoWidget((byte) 1, "STAT");
        String query = "{\"tabla\":\"imss_deteccion_diabetes\",\"funcion\":\"SUM\",\"columna\":\"detecciones\"}";

        Metrica metrica = new Metrica();
        metrica.setNombre("Detecciones");
        metrica.setUnidad(null); // sin unidad

        when(metricaRepository.findByColumnaCsvAndDatasetId("detecciones", datasetMock.getId()))
                .thenReturn(Optional.of(metrica));
        when(queryExecutor.execute(query, "STAT"))
                .thenReturn(new HashMap<>(Map.of("value", 142300)));

        Widget widget = new Widget(UUID.randomUUID(), "Total", authenticatedUser, tipo, query, 1, null);
        when(widgetRepository.findByUserId(authenticatedUser.getId()))
                .thenReturn(List.of(widget));

        List<WidgetResponseDto> result = useCase.execute();

        assertNull(result.get(0).getData().get("label")); // no agrega label si unidad es null
        assertEquals(142300, result.get(0).getData().get("value"));
    }

    // ─── Dashboard Mercadotecnia (rol_id = 4) ────────────────────────────

    @Test
    void executeShouldReturnMercadotecniaDefaultsWhenUserHasRole4() {
        Role mktRole = new Role((byte) 4, "DIRECTOR_MERCADOTECNIA");
        User mktUser = new User(UUID.randomUUID(), "Maria", "Mkt", "mkt@test.com",
                mktRole, true, "fb-uid-mkt");
        when(authContext.getUser()).thenReturn(mktUser);

        TipoWidget statTipo  = new TipoWidget((byte) 1, "STAT");
        TipoWidget lineTipo  = new TipoWidget((byte) 2, "LINE");
        TipoWidget tableTipo = new TipoWidget((byte) 5, "TABLE");
        TipoWidget msTipo    = new TipoWidget((byte) 6, "MULTISERIES");

        List<Widget> defaults = List.of(
                new Widget(UUID.randomUUID(), "% Personas con diabetes no diagnosticadas", null, statTipo,
                        "{\"tabla\":\"v_f8_no_diagnosticados_actual\",\"funcion\":\"MAX\",\"columna\":\"porcentaje_no_diagnosticados\"}", 1, (byte) 4),
                new Widget(UUID.randomUUID(), "Estado prioritario por detecciones", null, statTipo,
                        "{\"tabla\":\"v_f5_estado_prioritario_actual\",\"funcion\":\"MAX\",\"columna\":\"nombre_estado\"}", 2, (byte) 4),
                new Widget(UUID.randomUUID(), "% Población con diabetes (2024)", null, statTipo,
                        "{\"tabla\":\"f12_idf_mexico_porcentajes\",\"funcion\":\"MAX\",\"columna\":\"value\"}", 3, (byte) 4),
                new Widget(UUID.randomUUID(), "Carga de diabetes (DALYs) por edad y sexo", null, msTipo,
                        "{\"tabla\":\"f7_burden_diabetes\",\"colX\":\"age_group\",\"colY\":\"value\",\"colSerie\":\"sex\",\"funcion\":\"MAX\"}", 4, (byte) 4),
                new Widget(UUID.randomUUID(), "Detecciones de diabetes por año", null, lineTipo,
                        "{\"tabla\":\"f5_diabetes_delegaciones\",\"colX\":\"ano\",\"colY\":\"detecciones\",\"funcion\":\"SUM\",\"groupBy\":\"ano\"}", 5, (byte) 4),
                new Widget(UUID.randomUUID(), "Obesidad y sobrepeso por edad", null, msTipo,
                        "{\"tabla\":\"f9_obesidad_paho\",\"colX\":\"age_group\",\"colY\":\"value\",\"colSerie\":\"indicator_name\",\"funcion\":\"MAX\"}", 6, (byte) 4),
                new Widget(UUID.randomUUID(), "Carencias sociales por municipio", null, tableTipo,
                        "{\"tabla\":\"v_f2_carencias_por_municipio\",\"columnas\":\"municipio, rezago_educativo\",\"limite\":20}", 7, (byte) 4),
                new Widget(UUID.randomUUID(), "Top 5 estados con más detecciones", null, tableTipo,
                        "{\"tabla\":\"v_f5_top_estados_actual\",\"columnas\":\"estado, detecciones_total\",\"limite\":5}", 8, (byte) 4)
        );

        when(widgetRepository.findDefaultsByRolId((byte) 4)).thenReturn(defaults);

        List<WidgetResponseDto> result = useCase.execute();

        assertEquals(8, result.size());
        verify(widgetRepository, times(1)).findDefaultsByRolId((byte) 4);
        verify(widgetRepository, never()).findDefaultsByRolId((byte) 3);
        // los títulos corresponden a las 8 subtareas de HI-485
        List<String> titles = result.stream().map(WidgetResponseDto::getTitulo).toList();
        assertTrue(titles.contains("% Personas con diabetes no diagnosticadas"));
        assertTrue(titles.contains("Estado prioritario por detecciones"));
        assertTrue(titles.contains("Top 5 estados con más detecciones"));
    }

    @Test
    void executeShouldPassMultiseriesQueryWithDoubleFilterToExecutor() {
        Role mktRole = new Role((byte) 4, "DIRECTOR_MERCADOTECNIA");
        User mktUser = new User(UUID.randomUUID(), "Maria", "Mkt", "mkt@test.com",
                mktRole, true, "fb-uid-mkt");
        when(authContext.getUser()).thenReturn(mktUser);

        TipoWidget msTipo = new TipoWidget((byte) 6, "MULTISERIES");
        String hi507Query = "{\"tabla\":\"f7_burden_diabetes\",\"colX\":\"age_group\",\"colY\":\"value\",\"colSerie\":\"sex\",\"funcion\":\"MAX\","
                + "\"filtroCol\":\"measure_name\",\"filtroVal\":\"Disability-Adjusted Life Years (DALYs)\","
                + "\"filtroCol2\":\"year\",\"filtroVal2\":\"2021\"}";

        Widget hi507 = new Widget(UUID.randomUUID(), "Carga de diabetes (DALYs) por edad y sexo",
                null, msTipo, hi507Query, 4, (byte) 4);
        when(widgetRepository.findDefaultsByRolId((byte) 4)).thenReturn(List.of(hi507));

        useCase.execute();

        // El JSON crudo (incluyendo filtroCol2/filtroVal2) debe llegar tal cual al executor
        verify(queryExecutor, times(1)).execute(hi507Query, "MULTISERIES");
    }

    @Test
    void executeShouldPassStatQueryWithDoubleFilterToExecutor() {
        Role mktRole = new Role((byte) 4, "DIRECTOR_MERCADOTECNIA");
        User mktUser = new User(UUID.randomUUID(), "Maria", "Mkt", "mkt@test.com",
                mktRole, true, "fb-uid-mkt");
        when(authContext.getUser()).thenReturn(mktUser);

        TipoWidget statTipo = new TipoWidget((byte) 1, "STAT");
        String hi506Query = "{\"tabla\":\"f12_idf_mexico_porcentajes\",\"funcion\":\"MAX\",\"columna\":\"value\","
                + "\"filtroCol\":\"indicator\",\"filtroVal\":\"Age-standardised prevalence of diabetes (%)\","
                + "\"filtroCol2\":\"year\",\"filtroVal2\":\"2024\"}";

        Widget hi506 = new Widget(UUID.randomUUID(), "% Población con diabetes (2024)",
                null, statTipo, hi506Query, 3, (byte) 4);
        when(widgetRepository.findDefaultsByRolId((byte) 4)).thenReturn(List.of(hi506));
        when(queryExecutor.execute(hi506Query, "STAT")).thenReturn(Map.of("value", 16.4));

        List<WidgetResponseDto> result = useCase.execute();

        verify(queryExecutor, times(1)).execute(hi506Query, "STAT");
        assertEquals(16.4, result.get(0).getData().get("value"));
    }

    @Test
    void executeShouldSetSeriesNameAndAxisLabelsForLineChart() {
        TipoWidget tipo = new TipoWidget((byte) 2, "LINE");
        String query = "{\"tabla\":\"worldbank_health_expenditure\",\"colX\":\"time_period\",\"colY\":\"obs_value\",\"funcion\":\"AVG\",\"groupBy\":\"time_period\"}";

        Metrica metricaY = new Metrica();
        metricaY.setNombre("Observación");
        metricaY.setUnidad("%");

        Metrica metricaX = new Metrica();
        metricaX.setNombre("Período");
        metricaX.setUnidad(null);

        when(datasetRepository.findByNombreTabla("worldbank_health_expenditure"))
                .thenReturn(Optional.of(datasetMock));
        when(metricaRepository.findByColumnaCsvAndDatasetId("obs_value", datasetMock.getId()))
                .thenReturn(Optional.of(metricaY));
        when(metricaRepository.findByColumnaCsvAndDatasetId("time_period", datasetMock.getId()))
                .thenReturn(Optional.of(metricaX));
        when(queryExecutor.execute(query, "LINE"))
                .thenReturn(new HashMap<>(Map.of("labels", List.of(2020, 2021), "values", List.of(5.8, 6.0))));

        Widget widget = new Widget(UUID.randomUUID(), "Gasto % PIB",
                authenticatedUser, tipo, query, 1, null);
        when(widgetRepository.findByUserId(authenticatedUser.getId()))
                .thenReturn(List.of(widget));

        List<WidgetResponseDto> result = useCase.execute();

        WidgetResponseDto dto = result.get(0);
        assertEquals("Observación",       dto.getSeriesName());
        assertEquals("Observación (%)",   dto.getyAxisLabel());
        assertEquals("Período",           dto.getxAxisLabel());
    }
}