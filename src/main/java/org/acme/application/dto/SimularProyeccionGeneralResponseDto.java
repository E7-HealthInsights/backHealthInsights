package org.acme.application.dto;

import java.util.List;

public class SimularProyeccionGeneralResponseDto {

    private List<PuntoProyeccion> puntos;
    private KpisGeneral kpis;

    public static class PuntoProyeccion {
        private int año;
        private double sinIntervencion;
        private double conIntervencion;

        public PuntoProyeccion(int año, double sinIntervencion, double conIntervencion) {
            this.año = año;
            this.sinIntervencion = sinIntervencion;
            this.conIntervencion = conIntervencion;
        }

        public int getAño() { return año; }
        public double getSinIntervencion() { return sinIntervencion; }
        public double getConIntervencion() { return conIntervencion; }
    }

    public static class KpisGeneral {
        private double casosProyectados2050;  // millones de personas
        private double casosEvitados;         // millones de personas
        private double reduccionPorcentual;   // %

        public KpisGeneral(double casosProyectados2050,
                           double casosEvitados,
                           double reduccionPorcentual) {
            this.casosProyectados2050 = casosProyectados2050;
            this.casosEvitados        = casosEvitados;
            this.reduccionPorcentual  = reduccionPorcentual;
        }

        public double getCasosProyectados2050() { return casosProyectados2050; }
        public double getCasosEvitados() { return casosEvitados; }
        public double getReduccionPorcentual() { return reduccionPorcentual; }
    }

    public List<PuntoProyeccion> getPuntos() { return puntos; }
    public void setPuntos(List<PuntoProyeccion> puntos) { this.puntos = puntos; }

    public KpisGeneral getKpis() { return kpis; }
    public void setKpis(KpisGeneral kpis) { this.kpis = kpis; }
}