package org.acme.infrastructure.openai;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.application.dto.WidgetResponseDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprime los widgets del dashboard en un JSON compacto (< 2KB en la mayoría de casos)
 * que se pasa como contexto a OpenAI. NO incluye datasets completos: solo lo necesario
 * para que el modelo razone (top N, deltas, tendencias).
 */
@ApplicationScoped
public class DashboardSummarizer {

    private static final int MAX_POINTS_PER_SERIES = 6;
    private static final int MAX_ROWS_PER_TABLE = 5;

    public List<Map<String, Object>> summarize(List<WidgetResponseDto> widgets) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (widgets == null) return out;

        for (WidgetResponseDto w : widgets) {
            Map<String, Object> data = w.getData();
            if (data == null || data.containsKey("error")) continue;

            Map<String, Object> compact = new LinkedHashMap<>();
            compact.put("widget_id", String.valueOf(w.getId()));
            compact.put("titulo", w.getTitulo());
            if (w.getSubtitulo() != null) compact.put("fuente", w.getSubtitulo());
            compact.put("tipo", w.getTipo());
            if (w.getTipoSemantico() != null) compact.put("tipo_semantico", w.getTipoSemantico());
            if (w.getNivelGeografico() != null) compact.put("nivel_geografico", w.getNivelGeografico());

            switch (String.valueOf(w.getTipo())) {
                case "STAT" -> summarizeStat(compact, data);
                case "LINE", "BAR", "PIE" -> summarizeSeries(compact, data);
                case "MULTISERIES", "MULTIBAR" -> summarizeMultiseries(compact, data);
                case "TABLE" -> summarizeTable(compact, data);
                default -> compact.put("data", "(omitido)");
            }

            out.add(compact);
        }
        return out;
    }

    private void summarizeStat(Map<String, Object> out, Map<String, Object> data) {
        out.put("valor", data.get("value"));
        if (data.containsKey("label")) out.put("unidad", data.get("label"));
    }

    @SuppressWarnings("unchecked")
    private void summarizeSeries(Map<String, Object> out, Map<String, Object> data) {
        Object labelsObj = data.get("labels");
        Object valuesObj = data.get("values");
        if (!(labelsObj instanceof List<?>) || !(valuesObj instanceof List<?>)) {
            out.put("data", "(no disponible)");
            return;
        }
        List<Object> labels = (List<Object>) labelsObj;
        List<Object> values = (List<Object>) valuesObj;
        int n = Math.min(labels.size(), values.size());
        if (n == 0) { out.put("data", "(vacío)"); return; }

        List<Map<String, Object>> sample = new ArrayList<>();
        int start = Math.max(0, n - MAX_POINTS_PER_SERIES);
        for (int i = start; i < n; i++) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("x", String.valueOf(labels.get(i)));
            p.put("y", toNumber(values.get(i)));
            sample.add(p);
        }
        out.put("ultimos_puntos", sample);

        Double first = toNumber(values.get(0));
        Double last  = toNumber(values.get(n - 1));
        if (first != null && last != null && first != 0) {
            double deltaPct = ((last - first) / Math.abs(first)) * 100.0;
            out.put("delta_pct", round1(deltaPct));
            out.put("tendencia", deltaPct > 2 ? "subida" : deltaPct < -2 ? "bajada" : "estable");
        }
    }

    @SuppressWarnings("unchecked")
    private void summarizeMultiseries(Map<String, Object> out, Map<String, Object> data) {
        Object seriesKeysObj = data.get("seriesKeys");
        Object dataPointsObj = data.get("data");
        if (!(seriesKeysObj instanceof List<?>) || !(dataPointsObj instanceof List<?>)) {
            out.put("data", "(no disponible)");
            return;
        }
        List<String> seriesKeys = ((List<?>) seriesKeysObj).stream().map(String::valueOf).toList();
        List<Map<String, Object>> points = (List<Map<String, Object>>) dataPointsObj;
        if (points.isEmpty()) { out.put("data", "(vacío)"); return; }

        out.put("series", seriesKeys);
        List<Map<String, Object>> sample = new ArrayList<>();
        int start = Math.max(0, points.size() - MAX_POINTS_PER_SERIES);
        for (int i = start; i < points.size(); i++) {
            Map<String, Object> p = new LinkedHashMap<>();
            Map<String, Object> orig = points.get(i);
            p.put("x", orig.get("label"));
            for (String k : seriesKeys) {
                Object v = orig.get(k);
                if (v != null) p.put(k, toNumber(v));
            }
            sample.add(p);
        }
        out.put("ultimos_puntos", sample);
    }

    @SuppressWarnings("unchecked")
    private void summarizeTable(Map<String, Object> out, Map<String, Object> data) {
        Object columnsObj = data.get("columns");
        Object rowsObj = data.get("rows");
        if (!(rowsObj instanceof List<?>)) { out.put("data", "(no disponible)"); return; }
        List<Map<String, Object>> rows = (List<Map<String, Object>>) rowsObj;
        if (rows.isEmpty()) { out.put("data", "(vacío)"); return; }

        if (columnsObj instanceof List<?>) {
            out.put("columnas", columnsObj);
        }

        Map<String, Object> first = rows.get(0);
        String topNumericCol = null;
        for (Map.Entry<String, Object> e : first.entrySet()) {
            if (e.getValue() instanceof Number) {
                topNumericCol = e.getKey();
                break;
            }
        }

        List<Map<String, Object>> rowsSorted = new ArrayList<>(rows);
        if (topNumericCol != null) {
            final String col = topNumericCol;
            rowsSorted.sort((a, b) -> Double.compare(
                    toNumber(b.get(col)) == null ? 0 : toNumber(b.get(col)),
                    toNumber(a.get(col)) == null ? 0 : toNumber(a.get(col))));
            out.put("ordenado_por", topNumericCol);
        }

        List<Map<String, Object>> top = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_ROWS_PER_TABLE, rowsSorted.size()); i++) {
            top.add(rowsSorted.get(i));
        }
        out.put("top_filas", top);
        out.put("total_filas", rows.size());
    }

    private Double toNumber(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
