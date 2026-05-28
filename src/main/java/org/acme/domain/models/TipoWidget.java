package org.acme.domain.models;

public class TipoWidget {
    private Byte id;
    private String nombre;  // "STAT", "LINE", "BAR", "PIE", "TABLE"

    public TipoWidget() {
    }

    public TipoWidget(Byte id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

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
