package org.acme.application.dto;

import java.util.List;

public class PaginadoResponseDto<T> {

    private List<T> data;
    private long totalElementos;
    private int totalPaginas;
    private int paginaActual;
    private int tamañoPagina;

    public PaginadoResponseDto(List<T> data, long totalElementos,
                                int paginaActual, int tamañoPagina) {
        this.data           = data;
        this.totalElementos = totalElementos;
        this.paginaActual   = paginaActual;
        this.tamañoPagina   = tamañoPagina;
        this.totalPaginas   = (int) Math.ceil((double) totalElementos / tamañoPagina);
    }

    public List<T> getData() { return data; }
    public long getTotalElementos() { return totalElementos; }
    public int getTotalPaginas() { return totalPaginas; }
    public int getPaginaActual() { return paginaActual; }
    public int getTamañoPagina() { return tamañoPagina; }
}