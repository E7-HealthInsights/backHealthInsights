package org.acme.infrastructure.csv;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.application.dto.ColumnDefinitionDto;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio de infraestructura responsable de:
 * 1. Crear dinámicamente la tabla en MySQL con los tipos definidos por el admin.
 * 2. Insertar los datos del CSV en lotes (batch inserts).
 * 3. Hacer DROP TABLE en caso de error para dejar la BD limpia.
 *
 * Trabaja directamente con JDBC para operaciones DDL, ya que JPA/Hibernate
 * no gestiona DDL dinámico en runtime.
 */
@ApplicationScoped
public class CsvIngestService {

    private static final Logger LOG = Logger.getLogger(CsvIngestService.class);
    private static final int BATCH_SIZE = 500;

    /** Identificador de tabla seguro: solo minúsculas, dígitos y guion bajo (1-64). */
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-z0-9_]{1,64}$");

    /**
     * Whitelist de tipos SQL permitidos. Debe coincidir con el @Pattern de
     * ColumnDefinitionDto: este servicio recibe los datos vía Kafka, donde la
     * bean validation del REST NO se ejecuta, así que revalidamos aquí.
     */
    private static final Pattern SQL_TYPE_PATTERN = Pattern.compile(
            "^(VARCHAR\\(255\\)|TEXT|INT|BIGINT|DECIMAL\\(10,2\\)|FLOAT|DOUBLE|BOOLEAN|DATE|DATETIME|TIMESTAMP|JSON)$");

    @Inject
    DataSource dataSource;

    /**
     * Crea la tabla y carga todos los datos del CSV.
     *
     * @param nombreTabla nombre seguro de la tabla (ya validado como slug)
     * @param columnas    definición de columnas (nombre original → tipo SQL)
     * @param csvStream   stream del archivo CSV subido
     * @throws SQLException si ocurre algún error de BD
     * @throws IOException  si el stream del CSV no se puede leer
     */
    public void crearTablaEInsertarDatos(
            String nombreTabla,
            List<ColumnDefinitionDto> columnas,
            InputStream csvStream
    ) throws SQLException, IOException {

        String safeTableName = validateTableName(nombreTabla);
        String ddl = buildCreateTable(safeTableName, columnas);
        LOG.infof("Creando tabla dinámica: %s", ddl);

        try (Connection conn = dataSource.getConnection()) {
            // 1 — Crear tabla
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(ddl);
            }

            // 2 — Insertar datos en batch
            try {
                insertarEnBatch(conn, safeTableName, columnas, csvStream);
            } catch (Exception e) {
                // Si falla la carga de datos, eliminamos la tabla para dejar la BD limpia
                LOG.warnf("Error al insertar datos en '%s'. Haciendo DROP TABLE para limpiar.", safeTableName);
                try (Statement dropStmt = conn.createStatement()) {
                    dropStmt.execute("DROP TABLE IF EXISTS `" + safeTableName + "`");
                } catch (SQLException dropEx) {
                    LOG.errorf("No se pudo hacer DROP TABLE '%s': %s", safeTableName, dropEx.getMessage());
                }
                throw e; // re-lanzar para que el use case haga rollback JPA
            }
        }
    }

    /**
     * Elimina la tabla si existe. Usado para rollback manual cuando falla
     * después del DDL pero antes de que termine la transacción JPA.
     */
    public void dropTablaIfExists(String nombreTabla) {
        String safeTableName = validateTableName(nombreTabla);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS `" + safeTableName + "`");
            LOG.infof("DROP TABLE '%s' ejecutado correctamente.", safeTableName);
        } catch (SQLException e) {
            LOG.errorf("Error al hacer DROP TABLE '%s': %s", safeTableName, e.getMessage());
        }
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    /**
     * Valida que el nombre de tabla sea un identificador SQL seguro.
     * Defensa en profundidad: aunque el nombre se genera con slugify() en el
     * use case, este servicio lo recibe vía Kafka (frontera de confianza donde
     * la validación del REST no aplica), así que lo revalidamos aquí.
     *
     * @throws IllegalArgumentException si el nombre no es un identificador seguro.
     */
    private String validateTableName(String nombreTabla) {
        if (nombreTabla == null || !TABLE_NAME_PATTERN.matcher(nombreTabla).matches()) {
            throw new IllegalArgumentException("Nombre de tabla inválido: " + nombreTabla);
        }
        return nombreTabla;
    }

    /**
     * Valida que el tipo SQL esté en la whitelist permitida (misma que el DTO).
     * Necesario porque el consumer Kafka no ejecuta bean validation.
     *
     * @throws IllegalArgumentException si el tipo no está permitido.
     */
    private String validateSqlType(String sqlType) {
        if (sqlType == null || !SQL_TYPE_PATTERN.matcher(sqlType).matches()) {
            throw new IllegalArgumentException("Tipo SQL no permitido: " + sqlType);
        }
        return sqlType;
    }

    private String buildCreateTable(String nombreTabla, List<ColumnDefinitionDto> columnas) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE `").append(validateTableName(nombreTabla)).append("` (");
        sb.append("`_id` BIGINT AUTO_INCREMENT PRIMARY KEY, ");

        for (int i = 0; i < columnas.size(); i++) {
            ColumnDefinitionDto col = columnas.get(i);
            // Backtick para soportar nombres con espacios o caracteres especiales
            sb.append("`").append(sanitizeColumnName(col.getOriginalName())).append("` ");
            sb.append(validateSqlType(col.getSqlType()));
            sb.append(" NULL");
            if (i < columnas.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    private void insertarEnBatch(
            Connection conn,
            String nombreTabla,
            List<ColumnDefinitionDto> columnas,
            InputStream csvStream
    ) throws SQLException, IOException {

        String insertSql = buildInsertSql(nombreTabla, columnas);
        LOG.debugf("INSERT template: %s", insertSql);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvStream, StandardCharsets.UTF_8));
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            // Saltar la línea de cabeceras
            String headerLine = reader.readLine();
            if (headerLine == null) {
                LOG.warn("El CSV está vacío (sin cabeceras).");
                return;
            }

            String line;
            int batchCount = 0;
            int totalRows = 0;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                List<String> values = parseCsvLine(line);

                for (int i = 0; i < columnas.size(); i++) {
                    String rawValue = i < values.size() ? values.get(i) : null;
                    if (rawValue == null || rawValue.isBlank()) {
                        ps.setNull(i + 1, java.sql.Types.NULL);
                    } else {
                        String cleanValue = cleanValue(rawValue, columnas.get(i).getSqlType());
                        if (cleanValue == null) {
                            ps.setNull(i + 1, java.sql.Types.NULL);
                        } else {
                            ps.setString(i + 1, cleanValue);
                        }
                    }
                }

                ps.addBatch();
                batchCount++;
                totalRows++;

                if (batchCount == BATCH_SIZE) {
                    ps.executeBatch();
                    batchCount = 0;
                    LOG.debugf("Batch ejecutado — %d filas insertadas hasta ahora.", totalRows);
                }
            }

            // Ejecutar el último batch (filas restantes)
            if (batchCount > 0) {
                ps.executeBatch();
            }

            LOG.infof("Carga completada: %d filas insertadas en '%s'.", totalRows, nombreTabla);
        }
    }

    private String buildInsertSql(String nombreTabla, List<ColumnDefinitionDto> columnas) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO `").append(validateTableName(nombreTabla)).append("` (");

        for (int i = 0; i < columnas.size(); i++) {
            sb.append("`").append(sanitizeColumnName(columnas.get(i).getOriginalName())).append("`");
            if (i < columnas.size() - 1) sb.append(", ");
        }

        sb.append(") VALUES (");
        sb.append("?,".repeat(columnas.size()));
        // Reemplazar la última coma por el cierre de paréntesis
        sb.setCharAt(sb.length() - 1, ')');

        return sb.toString();
    }

    /**
     * Parser CSV manual que respeta comillas dobles.
     * Maneja campos entre comillas que pueden contener comas.
     */
    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                // Comilla doble escapada dentro de campo ("" → ")
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

    /**
     * Genera un nombre de columna seguro para SQL.
     */
    private String sanitizeColumnName(String name) {
        String clean = name.toLowerCase()
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
        // MySQL no permite nombres de columna que empiecen con dígito
        if (!clean.isEmpty() && Character.isDigit(clean.charAt(0))) {
            clean = "_" + clean;
        }
        return clean.isEmpty() ? "col_sin_nombre" : clean;
    }

    /**
     * Limpia el valor raw del CSV según el tipo SQL destino.
     * - Tipos numéricos: quita %, $, comas de miles. Si tras limpiar el valor
     *   no es un número válido (N/A, -, null, texto), retorna null → se guarda como NULL.
     * - Otros tipos: retorna el valor tal cual (trim).
     */
    private String cleanValue(String raw, String sqlType) {
        String trimmed = raw.trim();
        boolean isNumeric = sqlType.matches("INT|BIGINT|FLOAT|DOUBLE|DECIMAL\\(.*\\)");
        if (isNumeric) {
            String cleaned = trimmed
                    .replace("%", "")
                    .replace("$", "")
                    .replace(",", "")
                    .trim();
            // Si tras limpiar está vacío o no es un número válido → NULL
            if (cleaned.isEmpty()) return null;
            try {
                Double.parseDouble(cleaned);
                return cleaned;
            } catch (NumberFormatException e) {
                return null; // N/A, -, n/d, texto, etc. → NULL
            }
        }
        return trimmed;
    }
}