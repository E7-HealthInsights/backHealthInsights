package org.acme.infrastructure.csv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class CsvIngestServiceTest {

    // Prueba unitaria pura — verifica la lógica de sanitización de nombres de columna.
    // sanitizeColumnName es privado, se accede via reflection.
    // No necesita DataSource ni Quarkus — es lógica de string pura.
    //
    // NOTA IMPORTANTE: el método usa [^a-z0-9_] para limpiar caracteres no válidos.
    // Esto significa que las letras acentuadas (ú, ó, ñ, é…) se ELIMINAN, no se
    // transliteran. "número" → "nmero", "detección" → "deteccin", "año" → "ao".

    private Method sanitizeMethod;
    private CsvIngestService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new CsvIngestService();
        sanitizeMethod = CsvIngestService.class.getDeclaredMethod("sanitizeColumnName", String.class);
        sanitizeMethod.setAccessible(true);
    }

    private String sanitize(String input) throws Exception {
        return (String) sanitizeMethod.invoke(service, input);
    }

    // ── Tests: lowercase ──────────────────────────────────────────────────────

    @Test
    void shouldConvertToLowercase() throws Exception {
        assertEquals("estado", sanitize("Estado"));
        assertEquals("detecciones_diabetes", sanitize("DETECCIONES_DIABETES"));
        assertEquals("mixedcase", sanitize("MixedCase"));
    }

    // ── Tests: trim ───────────────────────────────────────────────────────────

    @Test
    void shouldTrimLeadingAndTrailingSpaces() throws Exception {
        assertEquals("estado", sanitize("  estado  "));
        assertEquals("col", sanitize("\tcol\t"));
    }

    // ── Tests: espacios internos → guion bajo ─────────────────────────────────

    @Test
    void shouldReplaceInternalSpacesWithUnderscore() throws Exception {
        assertEquals("numero_de_casos", sanitize("numero de casos"));
        assertEquals("estado_municipio", sanitize("estado  municipio")); // múltiples espacios
    }

    // ── Tests: eliminación de caracteres especiales ───────────────────────────

    @Test
    void shouldRemoveSpecialCharacters() throws Exception {
        assertEquals("porcentaje", sanitize("porcentaje%"));
        assertEquals("precio",     sanitize("precio$"));
        // La / se elimina; la ñ también cae fuera de [a-z0-9_] y se elimina
        // "tasa/año" → lowercase "tasa/año" → sin / → "tasaño" → sin ñ → "tasaao"
        assertEquals("tasaao",     sanitize("tasa/año"));
        // Los paréntesis se eliminan pero la 's' queda: "nombre(s)" → "nombres"
        assertEquals("nombres",    sanitize("nombre(s)"));
        assertEquals("col",        sanitize("col!@#"));
    }

    @Test
    void shouldEliminateAccentedLettersNotTransliterate() throws Exception {
        // Las tildes y la ñ caen fuera del charset [a-z0-9_]: se eliminan, no se convierten.
        assertEquals("nmero",    sanitize("número"));    // ú eliminada
        assertEquals("deteccin", sanitize("detección")); // ó eliminada
        assertEquals("ao",       sanitize("año"));       // ñ eliminada
        assertEquals("as",       sanitize("así"));       // í es un solo carácter, se elimina entero
    }

    @Test
    void shouldKeepLettersDigitsAndUnderscores() throws Exception {
        assertEquals("col_123_abc", sanitize("col_123_abc"));
        assertEquals("dato2024",    sanitize("dato2024"));
    }

    // ── Tests: prefijo cuando empieza con dígito ──────────────────────────────

    @Test
    void shouldPrefixWithUnderscoreWhenStartsWithDigit() throws Exception {
        assertEquals("_2024datos", sanitize("2024datos"));
        assertEquals("_1columna",  sanitize("1columna"));
        assertEquals("_9valor",    sanitize("9valor"));
    }

    @Test
    void shouldNotPrefixWhenStartsWithLetter() throws Exception {
        String result = sanitize("datos2024");
        assertFalse(result.startsWith("_"));
        assertEquals("datos2024", result);
    }

    @Test
    void shouldNotPrefixWhenStartsWithUnderscore() throws Exception {
        assertEquals("_col", sanitize("_col"));
    }

    // ── Tests: fallback cuando resulta vacío ──────────────────────────────────

    @Test
    void shouldReturnColSinNombreWhenResultIsEmpty() throws Exception {
        // Solo caracteres especiales → queda vacío → fallback
        assertEquals("col_sin_nombre", sanitize("!!!"));
        assertEquals("col_sin_nombre", sanitize("@#$%"));
        assertEquals("col_sin_nombre", sanitize("---"));
    }

    @Test
    void shouldReturnColSinNombreForEmptyString() throws Exception {
        assertEquals("col_sin_nombre", sanitize(""));
    }

    // ── Tests: casos reales de CSV ────────────────────────────────────────────

    // Los expected usan las cadenas que el método REALMENTE produce.
    // Las entradas con tildes producen resultados con letras faltantes (ú→eliminada, etc.)
    @ParameterizedTest
    @CsvSource({
            "Estado,            estado",
            "Tasa (%),          tasa_",
            "2023_dato,         _2023_dato",
            "Nombre(s),         nombres",
            "ok_column,         ok_column"
    })
    void shouldSanitizeColumnsWithoutAccents(String input, String expected) throws Exception {
        assertEquals(expected.trim(), sanitize(input.trim()));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", value = {
            "número de casos  -> nmero_de_casos",    // ú eliminada
            "Detección Temprana -> deteccin_temprana", // ó eliminada
            "Col. Número       -> col_nmero"           // ú eliminada
    })
    void shouldEliminateAccentsInRealWorldColumnNames(String input, String expected) throws Exception {
        assertEquals(expected.trim(), sanitize(input.trim()));
    }
}