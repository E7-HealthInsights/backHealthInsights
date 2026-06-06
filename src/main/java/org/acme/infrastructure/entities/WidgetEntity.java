package org.acme.infrastructure.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "Widget")
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "Widget.full",
                attributeNodes = {
                        @NamedAttributeNode("tipo"),
                        @NamedAttributeNode("usuario")
                }
        ),
        @NamedEntityGraph(
                name = "Widget.withTipo",
                attributeNodes = {
                        @NamedAttributeNode("tipo")
                }
        ),
        @NamedEntityGraph(
                name = "Widget.withUsuario",
                attributeNodes = {
                        @NamedAttributeNode("usuario")
                }
        )
})
public class WidgetEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "usuario_id", nullable = true)
    private UserEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoWidgetEntity tipo;

    @Column(name = "rol_id", nullable = true)
    private Byte rolId;

    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;   // JSON config guardado como String

    @Column(name = "orden")
    private int orden;

    @Column(name = "tipo_semantico", length = 20, nullable = true)
    private String tipoSemantico;

    @Column(name = "nivel_geografico", length = 20, nullable = true)
    private String nivelGeografico;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public UserEntity getUsuario() {
        return usuario;
    }

    public void setUsuario(UserEntity usuario) {
        this.usuario = usuario;
    }

    public TipoWidgetEntity getTipo() {
        return tipo;
    }

    public void setTipo(TipoWidgetEntity tipo) {
        this.tipo = tipo;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public Byte getRolId() {
        return rolId;
    }

    public void setRolId(Byte rolId) {
        this.rolId = rolId;
    }

    public String getTipoSemantico() {
        return tipoSemantico;
    }

    public void setTipoSemantico(String tipoSemantico) {
        this.tipoSemantico = tipoSemantico;
    }

    public String getNivelGeografico() {
        return nivelGeografico;
    }

    public void setNivelGeografico(String nivelGeografico) {
        this.nivelGeografico = nivelGeografico;
    }
}