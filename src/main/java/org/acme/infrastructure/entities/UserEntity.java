package org.acme.infrastructure.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Users")
@NamedEntityGraphs({   // definimos grafos de entidades para controlar qué relaciones se cargan automáticamente al recuperar un Entity
        @NamedEntityGraph(name = "User.full",    //Full es el grafo que carga todas las relaciones
                attributeNodes = {
                        @NamedAttributeNode("role")
                })
}
)

public class UserEntity {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    //Columna de roles - fk a role
    @ManyToOne(fetch = FetchType.LAZY, optional = false)  //relación de muchos a uno con UserEntity, cada usuario tiene un rol, pero un rol puede tener muchos usuarios
    @JoinColumn(name = "role_id", nullable = false)   //le ponemos nombre a la columna de la clave foranea por convencion mysql
    private RoleEntity role;

    @Column(nullable = false)
    private boolean status;

    @Column(name = "provider_id", nullable = false, unique = true)
    private String providerId;

    @Column(name = "modified_by", length = 36)
    private String modifiedBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}
