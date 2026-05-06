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
                default -> Map.of("error", "Tipo no soportado: " + tipoNombre);
            };
        } catch (Exception e) {
            return Map.of("error", "Error ejecutando widget: " + e.getMessage());
        }
    }

    private Map<String, Object> callStat(JsonNode c) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_widget_stat(?,?,?)}")) {

            cs.setString(1, c.get("tabla").asText());
            cs.setString(2, c.get("funcion").asText());
            cs.setString(3, c.get("columna").asText());

            ResultSet rs = cs.executeQuery();
            Map<String, Object> result = new HashMap<>();
            if (rs.next()) result.put("value", rs.getObject("value"));
            return result;
        }
    }

    private Map<String, Object> callSeries(JsonNode c) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_widget_series(?,?,?,?,?)}")) {

            cs.setString(1, c.get("tabla").asText());
            cs.setString(2, c.get("colX").asText());
            cs.setString(3, c.get("colY").asText());
            cs.setString(4, c.get("funcion").asText());
            cs.setString(5, c.get("groupBy").asText());

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
}