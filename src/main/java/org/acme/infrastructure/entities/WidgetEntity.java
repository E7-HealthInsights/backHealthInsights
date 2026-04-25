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

    @Column(name = "title", nullable = false, length = 100)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoWidgetEntity tipo;

    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;   // JSON config guardado como String

    @Column(name = "orden")
    private int orden;
}
