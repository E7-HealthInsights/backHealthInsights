package org.acme.domain.models;

import java.util.UUID;

public class Widget {
    private UUID id;
    private String titulo;
    private User usuario;
    private TipoWidget tipo;
    private String query;   // JSON config guardado como String
    private int orden;
    private Byte rolId;

    public Widget() {
    }

    public Widget(UUID id, String titulo, User usuario, TipoWidget tipo, String query, int orden, Byte rolId) {
        this.id = id;
        this.titulo = titulo;
        this.usuario = usuario;
        this.tipo = tipo;
        this.query = query;
        this.orden = orden;
        this.rolId = rolId;
    }

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

    public User getUsuario() {
        return usuario;
    }

    public void setUsuario(User usuario) {
        this.usuario = usuario;
    }

    public TipoWidget getTipo() {
        return tipo;
    }

    public void setTipo(TipoWidget tipo) {
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
}
