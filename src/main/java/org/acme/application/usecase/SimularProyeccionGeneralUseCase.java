package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.application.dto.SimularProyeccionGeneralResponseDto;
import org.acme.application.dto.SimularProyeccionGeneralResponseDto.KpisGeneral;
import org.acme.application.dto.SimularProyeccionGeneralResponseDto.PuntoProyeccion;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SimularProyeccionGeneralUseCase {

    // Personas con diabetes México 2024 — IDF Diabetes Atlas 2024
    // Fuente: dato real en BD (f12_idf_personas_diabetes)
    private static final double CASOS_BASE_2024_MILLONES = 13.587;

    public SimularProyeccionGeneralResponseDto execute(
            double tasaCrecimiento,   // ej. 2.1 → se divide entre 100 internamente
            double intensidadPolitica, // 0 = sin intervención, 100 = máxima
            int    periodoInicio,
            int    periodoFin
    ) {
        // Exactamente como en el frontend:
        // const tasa   = tasaCrecimiento / 100
        // const factor = 1 - intensidadPolitica / 100
        double tasa   = tasaCrecimiento / 100.0;
        double factor = 1.0 - intensidadPolitica / 100.0;

        // Curvas año por año — misma fórmula que el front
        List<PuntoProyeccion> puntos = new ArrayList<>();

        for (int i = 0; i <= periodoFin - periodoInicio; i++) {
            int año = periodoInicio + i;
            double sinIntervencion = round2(
                CASOS_BASE_2024_MILLONES * Math.pow(1 + tasa, i)
            );
            double conIntervencion = round2(
                CASOS_BASE_2024_MILLONES * Math.pow(1 + tasa * factor, i)
            );
            puntos.add(new PuntoProyeccion(año, sinIntervencion, conIntervencion));
        }

        // KPIs al año final — misma fórmula que el front
        PuntoProyeccion ultimo = puntos.get(puntos.size() - 1);

        double casosEvitados = round2(
            ultimo.getSinIntervencion() - ultimo.getConIntervencion()
        );
        double reduccionPorcentual = round1(
            ((ultimo.getConIntervencion() - ultimo.getSinIntervencion())
             / ultimo.getSinIntervencion()) * 100
        );

        SimularProyeccionGeneralResponseDto response = new SimularProyeccionGeneralResponseDto();
        response.setPuntos(puntos);
        response.setKpis(new KpisGeneral(
            ultimo.getSinIntervencion(), // casosProyectados al año final
            casosEvitados,
            reduccionPorcentual
        ));

        return response;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}