package org.acme.infrastructure.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Dataset")
public class DatasetEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 300)
    private String nombre;

    @Column(name = "nombre_tabla", length = 150)
    private String nombreTabla;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fuente", length = 300)
    private String fuente;

    @Column(name = "archivo_csv", length = 100)
    private String archivoCsv;

    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "estado", nullable = false)
    private boolean estado = true;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "modified_by", length = 36)
    private String modifiedBy;

    // Relación con métricas — se carga bajo demanda
    @OneToMany(mappedBy = "dataset", fetch = FetchType.LAZY)
    private List<MetricaEntity> metricas;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNombreTabla() { return nombreTabla; }
    public void setNombreTabla(String nombreTabla) { this.nombreTabla = nombreTabla; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public String getArchivoCsv() { return archivoCsv; }
    public void setArchivoCsv(String archivoCsv) { this.archivoCsv = archivoCsv; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public List<MetricaEntity> getMetricas() { return metricas; }
    public void setMetricas(List<MetricaEntity> metricas) { this.metricas = metricas; }
}