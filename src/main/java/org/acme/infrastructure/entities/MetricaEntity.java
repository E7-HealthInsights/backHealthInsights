package org.acme.infrastructure.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "Metrica")
public class MetricaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "columna_csv", nullable = false, length = 50)
    private String columnaCsv;

    @Column(name = "unidad", length = 10)
    private String unidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private DatasetEntity dataset;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getColumnaCsv() { return columnaCsv; }
    public void setColumnaCsv(String columnaCsv) { this.columnaCsv = columnaCsv; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public DatasetEntity getDataset() { return dataset; }
    public void setDataset(DatasetEntity dataset) { this.dataset = dataset; }
}