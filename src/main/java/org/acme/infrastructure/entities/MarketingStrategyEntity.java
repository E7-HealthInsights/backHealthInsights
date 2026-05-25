package org.acme.infrastructure.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Marketing_Strategy")
public class MarketingStrategyEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 36, nullable = false)
    private UUID usuarioId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "contexto_extra", columnDefinition = "TEXT")
    private String contextoExtra;

    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "nota_resultado", columnDefinition = "TEXT")
    private String notaResultado;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "comentarios_json", columnDefinition = "LONGTEXT")
    private String comentariosJson;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getContextoExtra() { return contextoExtra; }
    public void setContextoExtra(String contextoExtra) { this.contextoExtra = contextoExtra; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNotaResultado() { return notaResultado; }
    public void setNotaResultado(String notaResultado) { this.notaResultado = notaResultado; }

    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }

    public String getComentariosJson() { return comentariosJson; }
    public void setComentariosJson(String comentariosJson) { this.comentariosJson = comentariosJson; }
}
