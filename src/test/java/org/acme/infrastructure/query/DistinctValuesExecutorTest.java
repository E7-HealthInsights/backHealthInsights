package org.acme.infrastructure.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DistinctValuesExecutorTest {

    // Prueba unitaria pura — verifica la lógica de validación de identificadores
    // (SAFE_IDENTIFIER regex) sin necesitar base de datos real.
    // El método fetchDistinct lanza IllegalArgumentException antes de tocar JDBC
    // cuando tabla o columna contienen caracteres peligrosos.

    private DistinctValuesExecutor executor;

    @BeforeEach
    void setUp() {
        // DataSource no se usa hasta después de la validación → se puede pasar null
        // porque fetchDistinct lanza IllegalArgumentException antes de llamar a getConnection()
        executor = new DistinctValuesExecutor();
        // inyectamos dataSource como null vía reflection — la validación ocurre antes de usarlo
        try {
            var field = DistinctValuesExecutor.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            field.set(executor, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── Tests: tabla válida ───────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "diabetes_2023",
            "imss_deteccion",
            "tabla123",
            "mi tabla",          // espacios permitidos
            "tabla-con-guion",   // guion medio permitido
            "MAYUSCULAS",
            "Mix_tura 2024"
    })
    void shouldAcceptValidTableNames(String tabla) {
        // La excepción, si ocurre, sería IllegalArgumentException en la validación.
        // Con DataSource null, si pasa la validación lanzará NullPointerException al acceder
        // a la conexión — eso significa que la validación pasó correctamente.
        assertThrows(NullPointerException.class,
                () -> executor.fetchDistinct(tabla, "columna", 10),
                "Se esperaba NullPointerException (validación pasó, DataSource es null) para tabla: " + tabla
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "columna_estado",
            "nombre columna",
            "col123",
            "Estado-Region",
            "UPPER_CASE"
    })
    void shouldAcceptValidColumnNames(String columna) {
        assertThrows(NullPointerException.class,
                () -> executor.fetchDistinct("tabla_valida", columna, 10),
                "Se esperaba NullPointerException (validación pasó, DataSource es null) para columna: " + columna
        );
    }

    // ── Tests: tabla inválida → IllegalArgumentException ─────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "tabla'; DROP TABLE users;--",   // SQL injection clásico
            "tabla`backtick`",               // backtick
            "tabla/con/slash",               // slash
            "tabla\\backslash",              // backslash
            "tabla\ncon\nnewline",           // newlines
            "tabla\tcon\ttab",              // tabs
            "tabla*wildcard",               // asterisco
            "tabla=equal",                  // igual
            "tabla(parenthesis)"            // paréntesis
    })
    void shouldRejectInvalidTableNames(String tabla) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> executor.fetchDistinct(tabla, "columna", 10)
        );
        assertTrue(ex.getMessage().contains("tabla inválido") || ex.getMessage().contains(tabla));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "col'; DROP TABLE--",
            "col`quoted`",
            "col/slash",
            "col*star",
            "col(paren)",
            "col=sign"
    })
    void shouldRejectInvalidColumnNames(String columna) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> executor.fetchDistinct("tabla_valida", columna, 10)
        );
        assertTrue(ex.getMessage().contains("columna inválido") || ex.getMessage().contains(columna));
    }

    // ── Tests: cadena vacía ───────────────────────────────────────────────────

    @Test
    void shouldRejectEmptyTableName() {
        assertThrows(IllegalArgumentException.class,
                () -> executor.fetchDistinct("", "columna", 10)
        );
    }

    @Test
    void shouldRejectEmptyColumnName() {
        assertThrows(IllegalArgumentException.class,
                () -> executor.fetchDistinct("tabla_valida", "", 10)
        );
    }

    // ── Tests: tabla válida, columna inválida (y viceversa) ───────────────────

    @Test
    void shouldRejectWhenOnlyColumnIsInvalid() {
        // Tabla válida pero columna con caracteres de inyección
        assertThrows(IllegalArgumentException.class,
                () -> executor.fetchDistinct("tabla_valida", "col'; DELETE FROM--", 10)
        );
    }

    @Test
    void shouldRejectWhenOnlyTableIsInvalid() {
        // Columna válida pero tabla con caracteres de inyección
        assertThrows(IllegalArgumentException.class,
                () -> executor.fetchDistinct("'; DROP TABLE usuarios;--", "estado", 10)
        );
    }
}
