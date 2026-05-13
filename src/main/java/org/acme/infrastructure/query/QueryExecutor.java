package org.acme.infrastructure.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@ApplicationScoped
public class QueryExecutor {

    @Inject
    DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> execute(String queryJson, String tipoNombre) {
        try {
            JsonNode config = objectMapper.readTree(queryJson);
            return switch (tipoNombre) {
                case "STAT"        -> callStat(config);
                case "LINE", "BAR" -> callSeries(config);
                case "PIE"         -> callPie(config);
                case "TABLE"       -> callTable(config);
                case "MULTISERIES", "MULTIBAR" -> callMultiseries(config);
                default -> Map.of("error", "Tipo no soportado: " + tipoNombre);
            };
        } catch (Exception e) {
            return Map.of("error", "Error ejecutando widget: " + e.getMessage());
        }
    }

    private Map<String, Object> callStat(JsonNode c) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_widget_stat(?,?,?,?,?,?,?)}")) {

            cs.setString(1, c.get("tabla").asText());
            cs.setString(2, c.get("funcion").asText());
            cs.setString(3, c.get("columna").asText());

            if (c.has("filtroCol") && c.has("filtroVal")) {
                cs.setString(4, c.get("filtroCol").asText());
                cs.setString(5, c.get("filtroVal").asText());
            } else {
                cs.setNull(4, java.sql.Types.VARCHAR);
                cs.setNull(5, java.sql.Types.VARCHAR);
            }

            if (c.has("filtroCol2") && c.has("filtroVal2")) {
                cs.setString(6, c.get("filtroCol2").asText());
                cs.setString(7, c.get("filtroVal2").asText());
            } else {
                cs.setNull(6, java.sql.Types.VARCHAR);
                cs.setNull(7, java.sql.Types.VARCHAR);
            }

            ResultSet rs = cs.executeQuery();
            Map<String, Object> result = new HashMap<>();
            if (rs.next()) result.put("value", rs.getObject("value"));
            return result;
        }
    }

    private Map<String, Object> callSeries(JsonNode c) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_widget_series(?,?,?,?,?,?,?,?,?)}")) {

            cs.setString(1, c.get("tabla").asText());
            cs.setString(2, c.get("colX").asText());
            cs.setString(3, c.get("colY").asText());
            cs.setString(4, c.get("funcion").asText());
            cs.setString(5, c.get("groupBy").asText());

            if (c.has("filtroCol") && c.has("filtroVal")) {
                cs.setString(6, c.get("filtroCol").asText());
                cs.setString(7, c.get("filtroVal").asText());
            } else {
                cs.setNull(6, java.sql.Types.VARCHAR);
                cs.setNull(7, java.sql.Types.VARCHAR);
            }

            if (c.has("filtroCol2") && c.has("filtroVal2")) {
                cs.setString(8, c.get("filtroCol2").asText());
                cs.setString(9, c.get("filtroVal2").asText());
            } else {
                cs.setNull(8, java.sql.Types.VARCHAR);
                cs.setNull(9, java.sql.Types.VARCHAR);
            }

            ResultSet rs = cs.executeQuery();
            List<Object> labels = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            while (rs.next()) {
                labels.add(rs.getObject("label"));
                values.add(rs.getObject("value"));
            }
            return Map.of("labels", labels, "values", values);
        }
    }

    private Map<String, Object> callPie(JsonNode c) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_widget_pie(?,?,?,?)}")) {

            cs.setString(1, c.get("tabla").asText());
            cs.setString(2, c.get("colLabel").asText());
            cs.setString(3, c.get("colValue").asText());
            cs.setString(4, c.get("funcion").asText());

            ResultSet rs = cs.executeQuery();
            List<Object> labels = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            while (rs.next()) {
                labels.add(rs.getObject("label"));
                values.add(rs.getObject("value"));
            }
            return Map.of("labels", labels, "values", values);
        }
    }

    private Map<String, Object> callTable(JsonNode c) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_widget_table(?,?,?)}")) {

            cs.setString(1, c.get("tabla").asText());
            cs.setString(2, c.get("columnas").asText());
            cs.setInt(3, c.has("limite") ? c.get("limite").asInt() : 100);

            ResultSet rs = cs.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= cols; i++) columns.add(meta.getColumnName(i));

            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) row.put(columns.get(i-1), rs.getObject(i));
                rows.add(row);
            }
            return Map.of("columns", columns, "rows", rows);
        }
    }

    private Map<String, Object> callMultiseries(JsonNode c) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_widget_multiseries(?,?,?,?,?,?,?,?,?)}")) {

            cs.setString(1, c.get("tabla").asText());
            cs.setString(2, c.get("colX").asText());
            cs.setString(3, c.get("colY").asText());
            cs.setString(4, c.get("colSerie").asText());
            cs.setString(5, c.get("funcion").asText());

            if (c.has("filtroCol") && c.has("filtroVal")) {
                cs.setString(6, c.get("filtroCol").asText());
                cs.setString(7, c.get("filtroVal").asText());
            } else {
                cs.setNull(6, java.sql.Types.VARCHAR);
                cs.setNull(7, java.sql.Types.VARCHAR);
            }

            if (c.has("filtroCol2") && c.has("filtroVal2")) {
                cs.setString(8, c.get("filtroCol2").asText());
                cs.setString(9, c.get("filtroVal2").asText());
            } else {
                cs.setNull(8, java.sql.Types.VARCHAR);
                cs.setNull(9, java.sql.Types.VARCHAR);
            }
    
            ResultSet rs = cs.executeQuery();
    
            // Agrupa por label → { "2002": {"COVGCMED": 48.3, "PHINDUPI": null}, ... }
            Map<Object, Map<String, Object>> grouped = new LinkedHashMap<>();
            Set<String> seriesKeys = new LinkedHashSet<>();
    
            while (rs.next()) {
                Object label = rs.getObject("label");
                String serie = rs.getString("serie");
                Object value = rs.getObject("value");
    
                grouped.computeIfAbsent(label, k -> new LinkedHashMap<>()).put(serie, value);
                seriesKeys.add(serie);
            }
    
            // Convierte a array de objetos para Recharts
            // [{"label": 2002, "COVGCMED": 48.3}, {"label": 2002, "PHINDUPI": 2.8}, ...]
            List<Map<String, Object>> data = new ArrayList<>();
            for (Map.Entry<Object, Map<String, Object>> entry : grouped.entrySet()) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("label", entry.getKey());
                point.putAll(entry.getValue());
                data.add(point);
            }
    
            return Map.of(
                "data",       data,
                "seriesKeys", new ArrayList<>(seriesKeys)  // ["COVGCMED", "PHINDUPI"]
            );
        }
    }
}