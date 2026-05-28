package org.acme.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddCommentDto {

    @NotBlank(message = "El comentario no puede estar vacío")
    @Size(max = 2000, message = "El comentario es demasiado largo (máx. 2000 caracteres)")
    private String contenido;

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
}
