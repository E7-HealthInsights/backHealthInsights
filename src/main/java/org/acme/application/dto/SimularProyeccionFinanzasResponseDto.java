package org.acme.application.dto;

import java.util.List;

public class SimularProyeccionFinanzasResponseDto {

    private List<PuntoProyeccion> puntos;
    private KpisSimulacion kpis;

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

    public static class KpisSimulacion {
        private double reduccionPct;
        private long casosEvitados;
        private double ahorroEstimadoUSD_M;
        private double ROI;

        public KpisSimulacion(double reduccionPct, long casosEvitados,
                               double ahorroEstimadoUSD_M, double ROI) {
            this.reduccionPct        = reduccionPct;
            this.casosEvitados       = casosEvitados;
            this.ahorroEstimadoUSD_M = ahorroEstimadoUSD_M;
            this.ROI                 = ROI;
        }

        public double getReduccionPct() { return reduccionPct; }
        public long getCasosEvitados() { return casosEvitados; }
        public double getAhorroEstimadoUSD_M() { return ahorroEstimadoUSD_M; }
        public double getROI() { return ROI; }
    }

    public List<PuntoProyeccion> getPuntos() { return puntos; }
    public void setPuntos(List<PuntoProyeccion> puntos) { this.puntos = puntos; }

    public KpisSimulacion getKpis() { return kpis; }
    public void setKpis(KpisSimulacion kpis) { this.kpis = kpis; }
}