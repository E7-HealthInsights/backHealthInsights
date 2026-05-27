package org.acme.infrastructure.query;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class DistinctValuesExecutor {

    // Solo letras, dígitos y guion bajo — previene inyección en nombres de tabla/columna
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_]+$");

    @Inject
    DataSource dataSource;

    /**
     * Ejecuta SELECT DISTINCT {columna} FROM {tabla} LIMIT {limite}.
     * Tanto el nombre de tabla como el de columna se validan contra un patrón
     * seguro antes de interpolarse en el SQL, ya que JDBC no permite parámetros
     * en posición de identificador.
     *
     * @param tabla   nombre real de la tabla en BD (ej. "diabetes_2023")
     * @param columna nombre de la columna CSV (ej. "estado")
     * @param limite  máximo de valores a devolver
     * @return lista de valores distintos como String
     */
    public List<String> fetchDistinct(String tabla, String columna, int limite) {
        if (!SAFE_IDENTIFIER.matcher(tabla).matches()) {
            throw new IllegalArgumentException("Nombre de tabla inválido: " + tabla);
        }
        if (!SAFE_IDENTIFIER.matcher(columna).matches()) {
            throw new IllegalArgumentException("Nombre de columna inválido: " + columna);
        }

        String sql = String.format(
                "SELECT DISTINCT `%s` FROM `%s` WHERE `%s` IS NOT NULL ORDER BY `%s` LIMIT %d",
                columna, tabla, columna, columna, limite
        );

        List<String> valores = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Object val = rs.getObject(1);
                if (val != null) valores.add(val.toString());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo valores distintos: " + e.getMessage(), e);
        }
        return valores;
    }
}
