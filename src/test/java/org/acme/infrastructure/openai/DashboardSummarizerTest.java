package org.acme.infrastructure.openai;

import org.acme.application.dto.WidgetResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DashboardSummarizerTest {

    // Prueba unitaria pura — sin mocks, sin Quarkus.
    // Verifica la lógica de compresión de widgets para el prompt de OpenAI.

    private DashboardSummarizer summarizer;

    @BeforeEach
    void setUp() {
        summarizer = new DashboardSummarizer();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WidgetResponseDto buildWidget(String tipo, Map<String, Object> data) {
        WidgetResponseDto w = new WidgetResponseDto();
        w.setId(UUID.randomUUID());
        w.setTitulo("Widget de prueba");
        w.setTipo(tipo);
        w.setData(data);
        return w;
    }

    private Map<String, Object> statData(Object value) {
        return Map.of("value", value);
    }

    private Map<String, Object> statDataWithLabel(Object value, String label) {
        return Map.of("value", value, "label", label);
    }

    private Map<String, Object> seriesData(List<Object> labels, List<Object> values) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("labels", labels);
        data.put("values", values);
        return data;
    }

    // ── Tests: lista nula / vacía ─────────────────────────────────────────────

    @Test
    void summarizeShouldReturnEmptyListWhenInputIsNull() {
        List<Map<String, Object>> result = summarizer.summarize(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void summarizeShouldReturnEmptyListWhenInputIsEmpty() {
        List<Map<String, Object>> result = summarizer.summarize(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── Tests: widgets filtrados ──────────────────────────────────────────────

    @Test
    void summarizeShouldSkipWidgetsWithNullData() {
        WidgetResponseDto w = buildWidget("STAT", null);
        List<Map<String, Object>> result = summarizer.summarize(List.of(w));
        assertTrue(result.isEmpty());
    }

    @Test
    void summarizeShouldSkipWidgetsWithErrorInData() {
        WidgetResponseDto w = buildWidget("STAT", Map.of("error", "BD no disponible"));
        List<Map<String, Object>> result = summarizer.summarize(List.of(w));
        assertTrue(result.isEmpty());
    }

    // ── Tests: campos comunes ─────────────────────────────────────────────────

    @Test
    void summarizeShouldIncludeWidgetId() {
        UUID id = UUID.randomUUID();
        WidgetResponseDto w = buildWidget("STAT", statData(42));
        w.setId(id);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals(id.toString(), result.get("widget_id"));
    }

    @Test
    void summarizeShouldIncludeTitulo() {
        WidgetResponseDto w = buildWidget("STAT", statData(42));
        w.setTitulo("Casos por estado");

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("Casos por estado", result.get("titulo"));
    }

    @Test
    void summarizeShouldIncludeTipo() {
        WidgetResponseDto w = buildWidget("STAT", statData(42));
        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("STAT", result.get("tipo"));
    }

    @Test
    void summarizeShouldIncludeFuenteWhenSubtituloPresent() {
        WidgetResponseDto w = buildWidget("STAT", statData(42));
        w.setSubtitulo("Fuente: IMSS 2023");

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("Fuente: IMSS 2023", result.get("fuente"));
    }

    @Test
    void summarizeShouldNotIncludeFuenteWhenSubtituloIsNull() {
        WidgetResponseDto w = buildWidget("STAT", statData(42));
        w.setSubtitulo(null);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertFalse(result.containsKey("fuente"));
    }

    @Test
    void summarizeShouldIncludeTipoSemanticoWhenPresent() {
        WidgetResponseDto w = buildWidget("STAT", statData(42));
        w.setTipoSemantico("porcentaje");

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("porcentaje", result.get("tipo_semantico"));
    }

    @Test
    void summarizeShouldIncludeNivelGeograficoWhenPresent() {
        WidgetResponseDto w = buildWidget("STAT", statData(42));
        w.setNivelGeografico("estado");

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("estado", result.get("nivel_geografico"));
    }

    // ── Tests: STAT ───────────────────────────────────────────────────────────

    @Test
    void summarizeStatShouldIncludeValor() {
        WidgetResponseDto w = buildWidget("STAT", statData(16.4));
        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals(16.4, result.get("valor"));
    }

    @Test
    void summarizeStatShouldIncludeUnidadWhenLabelPresent() {
        WidgetResponseDto w = buildWidget("STAT", statDataWithLabel(500, "casos"));
        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("casos", result.get("unidad"));
    }

    @Test
    void summarizeStatShouldNotIncludeUnidadWhenLabelAbsent() {
        WidgetResponseDto w = buildWidget("STAT", statData(500));
        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertFalse(result.containsKey("unidad"));
    }

    // ── Tests: LINE / BAR / PIE (summarizeSeries) ─────────────────────────────

    @Test
    void summarizeLineShouldIncludeUltimosPuntos() {
        Map<String, Object> data = seriesData(
                List.of("2020", "2021", "2022"),
                List.of(10.0, 15.0, 20.0)
        );
        WidgetResponseDto w = buildWidget("LINE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertTrue(result.containsKey("ultimos_puntos"));
    }

    @Test
    void summarizeLineShouldLimitToMax6Points() {
        // 10 puntos — solo deben incluirse los últimos 6
        List<Object> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            labels.add("2" + String.format("%03d", i));
            values.add((double) i * 10);
        }
        WidgetResponseDto w = buildWidget("LINE", seriesData(labels, values));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> puntos = (List<Map<String, Object>>)
                summarizer.summarize(List.of(w)).get(0).get("ultimos_puntos");

        assertEquals(6, puntos.size());
    }

    @Test
    void summarizeLineShouldReturnAllPointsWhenLessThan6() {
        Map<String, Object> data = seriesData(
                List.of("2021", "2022", "2023"),
                List.of(10.0, 12.0, 15.0)
        );
        WidgetResponseDto w = buildWidget("LINE", data);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> puntos = (List<Map<String, Object>>)
                summarizer.summarize(List.of(w)).get(0).get("ultimos_puntos");

        assertEquals(3, puntos.size());
    }

    @Test
    void summarizeLineShouldCalculateDeltaPctWhenFirstIsNonZero() {
        // De 100 a 120 → +20%
        Map<String, Object> data = seriesData(List.of("2021", "2022"), List.of(100.0, 120.0));
        WidgetResponseDto w = buildWidget("LINE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals(20.0, result.get("delta_pct"));
    }

    @Test
    void summarizeLineShouldMarkTendenciaSubidaWhenDeltaAbove2() {
        Map<String, Object> data = seriesData(List.of("2021", "2022"), List.of(100.0, 110.0));
        WidgetResponseDto w = buildWidget("LINE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("subida", result.get("tendencia"));
    }

    @Test
    void summarizeLineShouldMarkTendenciaBajadaWhenDeltaBelow2() {
        // De 100 a 90 → -10%
        Map<String, Object> data = seriesData(List.of("2021", "2022"), List.of(100.0, 90.0));
        WidgetResponseDto w = buildWidget("LINE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("bajada", result.get("tendencia"));
    }

    @Test
    void summarizeLineShouldMarkTendenciaEstableWhenDeltaWithin2() {
        // De 100 a 101 → +1%
        Map<String, Object> data = seriesData(List.of("2021", "2022"), List.of(100.0, 101.0));
        WidgetResponseDto w = buildWidget("LINE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("estable", result.get("tendencia"));
    }

    @Test
    void summarizeLineShouldNotIncludeDeltaWhenFirstIsZero() {
        // División por cero: first == 0 → no se añade delta_pct ni tendencia
        Map<String, Object> data = seriesData(List.of("2021", "2022"), List.of(0.0, 10.0));
        WidgetResponseDto w = buildWidget("LINE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertFalse(result.containsKey("delta_pct"));
        assertFalse(result.containsKey("tendencia"));
    }

    @Test
    void summarizeLineShouldReturnNoDisponibleWhenDataMalformed() {
        WidgetResponseDto w = buildWidget("LINE", Map.of("labels", "no-es-lista"));
        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("(no disponible)", result.get("data"));
    }

    @Test
    void summarizeLineShouldReturnVacioWhenEmptyLists() {
        WidgetResponseDto w = buildWidget("LINE", seriesData(List.of(), List.of()));
        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("(vacío)", result.get("data"));
    }

    @Test
    void summarizeBarShouldBehaveLikeLineSeries() {
        Map<String, Object> data = seriesData(List.of("A", "B"), List.of(5.0, 10.0));
        WidgetResponseDto w = buildWidget("BAR", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertTrue(result.containsKey("ultimos_puntos"));
    }

    @Test
    void summarizePieShouldBehaveLikeSeries() {
        Map<String, Object> data = seriesData(List.of("Norte", "Sur"), List.of(60.0, 40.0));
        WidgetResponseDto w = buildWidget("PIE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertTrue(result.containsKey("ultimos_puntos"));
    }

    // ── Tests: MULTISERIES / MULTIBAR ─────────────────────────────────────────

    @Test
    void summarizeMultiseriesShouldIncludeSeriesKeys() {
        List<Map<String, Object>> points = List.of(
                Map.of("label", "CDMX", "diabetes", 18.0, "obesidad", 35.0),
                Map.of("label", "Jalisco", "diabetes", 14.0, "obesidad", 28.0)
        );
        Map<String, Object> data = Map.of("seriesKeys", List.of("diabetes", "obesidad"), "data", points);
        WidgetResponseDto w = buildWidget("MULTISERIES", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertTrue(result.containsKey("series"));
        assertTrue(result.containsKey("ultimos_puntos"));
    }

    @Test
    void summarizeMultiseriesShouldLimitToMax6Points() {
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            points.add(Map.of("label", "Estado" + i, "serie1", (double) i));
        }
        Map<String, Object> data = Map.of("seriesKeys", List.of("serie1"), "data", points);
        WidgetResponseDto w = buildWidget("MULTISERIES", data);

        @SuppressWarnings("unchecked")
        List<?> puntos = (List<?>) summarizer.summarize(List.of(w)).get(0).get("ultimos_puntos");
        assertEquals(6, puntos.size());
    }

    @Test
    void summarizeMultiseriesShouldReturnVacioWhenPointsEmpty() {
        Map<String, Object> data = Map.of("seriesKeys", List.of("serie1"), "data", List.of());
        WidgetResponseDto w = buildWidget("MULTISERIES", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("(vacío)", result.get("data"));
    }

    @Test
    void summarizeMultiseriesShouldReturnNoDisponibleWhenMalformed() {
        WidgetResponseDto w = buildWidget("MULTISERIES", Map.of("seriesKeys", "no-lista"));
        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("(no disponible)", result.get("data"));
    }

    @Test
    void summarizeMultibarShouldBehaveLikeMultiseries() {
        List<Map<String, Object>> points = List.of(
                Map.of("label", "CDMX", "casos", 500.0)
        );
        Map<String, Object> data = Map.of("seriesKeys", List.of("casos"), "data", points);
        WidgetResponseDto w = buildWidget("MULTIBAR", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertTrue(result.containsKey("ultimos_puntos"));
    }

    // ── Tests: TABLE ──────────────────────────────────────────────────────────

    @Test
    void summarizeTableShouldIncludeTopFilas() {
        List<Map<String, Object>> rows = List.of(
                new LinkedHashMap<>(Map.of("estado", "CDMX",    "casos", 500)),
                new LinkedHashMap<>(Map.of("estado", "Jalisco", "casos", 300))
        );
        Map<String, Object> data = Map.of("columns", List.of("estado", "casos"), "rows", rows);
        WidgetResponseDto w = buildWidget("TABLE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertTrue(result.containsKey("top_filas"));
        assertTrue(result.containsKey("total_filas"));
    }

    @Test
    void summarizeTableShouldLimitToMax5Rows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            rows.add(new LinkedHashMap<>(Map.of("estado", "E" + i, "casos", i * 100)));
        }
        Map<String, Object> data = Map.of("rows", rows);
        WidgetResponseDto w = buildWidget("TABLE", data);

        @SuppressWarnings("unchecked")
        List<?> top = (List<?>) summarizer.summarize(List.of(w)).get(0).get("top_filas");
        assertEquals(5, top.size());
    }

    @Test
    void summarizeTableShouldReportTotalFilasCorrectly() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            rows.add(new LinkedHashMap<>(Map.of("estado", "E" + i, "casos", i)));
        }
        Map<String, Object> data = Map.of("rows", rows);
        WidgetResponseDto w = buildWidget("TABLE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals(8, result.get("total_filas"));
    }

    @Test
    void summarizeTableShouldSortByFirstNumericColumnDescending() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(new LinkedHashMap<>(Map.of("estado", "Jalisco", "casos", 100)));
        rows.add(new LinkedHashMap<>(Map.of("estado", "CDMX",    "casos", 900)));
        rows.add(new LinkedHashMap<>(Map.of("estado", "NL",      "casos", 500)));

        Map<String, Object> data = Map.of("rows", rows);
        WidgetResponseDto w = buildWidget("TABLE", data);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> top = (List<Map<String, Object>>)
                summarizer.summarize(List.of(w)).get(0).get("top_filas");

        // El primero debe ser el de mayor valor (CDMX con 900)
        assertEquals("CDMX", top.get(0).get("estado"));
    }

    @Test
    void summarizeTableShouldIncludeOrdenadoPorWhenNumericColumnFound() {
        List<Map<String, Object>> rows = List.of(
                new LinkedHashMap<>(Map.of("estado", "CDMX", "casos", 500))
        );
        Map<String, Object> data = Map.of("rows", rows);
        WidgetResponseDto w = buildWidget("TABLE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertTrue(result.containsKey("ordenado_por"));
    }

    @Test
    void summarizeTableShouldReturnVacioWhenRowsEmpty() {
        Map<String, Object> data = Map.of("rows", List.of());
        WidgetResponseDto w = buildWidget("TABLE", data);

        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("(vacío)", result.get("data"));
    }

    @Test
    void summarizeTableShouldReturnNoDisponibleWhenRowsMalformed() {
        WidgetResponseDto w = buildWidget("TABLE", Map.of("rows", "no-es-lista"));
        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("(no disponible)", result.get("data"));
    }

    // ── Tests: tipo desconocido ───────────────────────────────────────────────

    @Test
    void summarizeShouldHandleUnknownWidgetType() {
        WidgetResponseDto w = buildWidget("GAUGE", Map.of("value", 42));
        Map<String, Object> result = summarizer.summarize(List.of(w)).get(0);
        assertEquals("(omitido)", result.get("data"));
    }

    // ── Tests: múltiples widgets ──────────────────────────────────────────────

    @Test
    void summarizeShouldProcessMultipleWidgets() {
        WidgetResponseDto w1 = buildWidget("STAT", statData(100));
        WidgetResponseDto w2 = buildWidget("STAT", statData(200));
        WidgetResponseDto w3 = buildWidget("STAT", null); // debe filtrarse

        List<Map<String, Object>> result = summarizer.summarize(List.of(w1, w2, w3));
        assertEquals(2, result.size());
    }
}
