package org.acme.application.dto;

import java.util.UUID;

public class MetricaResponseDto {
    private UUID id;
    private String nombre;
    private String columnaCsv;
    private String unidad;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getColumnaCsv() { return columnaCsv; }
    public void setColumnaCsv(String columnaCsv) { this.columnaCsv = columnaCsv; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
}