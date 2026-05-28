package org.acme.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class WidgetOrdenDto {

    @NotNull
    private UUID id;

    @Min(1)
    private int orden;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
