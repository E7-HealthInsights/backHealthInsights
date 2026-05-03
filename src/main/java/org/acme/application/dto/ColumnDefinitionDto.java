package org.acme.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ColumnDefinitionDto {

    /** Nombre real de la columna en el CSV (se usará como nombre de columna en la tabla dinámica). */
    @NotBlank(message = "El nombre original de la columna es obligatorio")
    @Size(max = 100, message = "El nombre original no debe exceder 100 caracteres")
    private String originalName;

    /** Nombre amigable que verá el usuario (campo `nombre` en Metrica). */
    @NotBlank(message = "El nombre para el sistema es obligatorio")
    @Size(max = 50, message = "El nombre para el sistema no debe exceder 50 caracteres")
    private String displayName;

    /**
     * Tipo SQL a usar en el CREATE TABLE.
     * Restringido a los valores que ofrece el frontend.
     */
    @NotBlank(message = "El tipo SQL es obligatorio")
    @Pattern(
            regexp = "VARCHAR\\(255\\)|TEXT|INT|BIGINT|DECIMAL\\(10,2\\)|FLOAT|BOOLEAN|DATE|DATETIME|TIMESTAMP|JSON",
            message = "Tipo SQL no permitido"
    )
    private String sqlType;

    /** Unidad de medida opcional (ej. \"%\", \"años\", \"MXN\"). */
    @Size(max = 10, message = "La unidad no debe exceder 10 caracteres")
    private String unidad;

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getSqlType() { return sqlType; }
    public void setSqlType(String sqlType) { this.sqlType = sqlType; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
}
