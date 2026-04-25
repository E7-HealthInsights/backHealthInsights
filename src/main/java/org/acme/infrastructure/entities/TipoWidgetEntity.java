package org.acme.infrastructure.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "Tipo_de_Grafica")
public class TipoWidgetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Byte id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    public Byte getId() {
        return id;
    }

    public void setId(Byte id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
