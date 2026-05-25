package org.acme.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.application.dto.SimularProyeccionFinanzasResponseDto;
import org.acme.application.dto.SimularProyeccionFinanzasResponseDto.KpisSimulacion;
import org.acme.application.dto.SimularProyeccionFinanzasResponseDto.PuntoProyeccion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SimularProyeccionFinanzasUseCase {

    // ─── Constantes del modelo ────────────────────────────────────────────────
    // Fuentes: DPP Study NEJM 2002, WHO Global Action Plan 2013-2020, IDF 2024

    private static final double PREVALENCIA_BASE_2024  = 16.4;
    private static final double TASA_CRECIMIENTO_BASE  = 0.021;
    private static final long   POBLACION_ADULTA       = 85_000_000L;
    private static final double GASTO_PER_CAPITA_USD   = 1_438.0;
    private static final double TIPO_CAMBIO_MXN_USD    = 17.5;

    // Efectividad clínica por rubro — % reducción de incidencia con cobertura 100%
    private static final Map<String, Double> EFECTIVIDAD = Map.of(
        "NUTRICION",    0.58,  // DPP Study NEJM 2002: -58% en pre-diabéticos
        "MEDICAMENTOS", 0.31,  // DPP Study NEJM 2002: -31% con metformina
        "DETECCION",    0.10,  // CDC Diabetes Prevention Program
        "ATENCION",     0.35   // WHO Global Action Plan 2013-2020 (promedio 25-45%)
    );

    // Costo en MXN millones para cubrir al 100% de los ~12.75M pre-diabéticos
    private static final Map<String, Double> COSTO_COBERTURA_TOTAL = Map.of(
        "NUTRICION",    10_000.0,  // programa intensivo
        "MEDICAMENTOS",  2_000.0,  // metformina genérica
        "DETECCION",     1_000.0,  // tamizaje masivo
        "ATENCION",      5_000.0   // consultas primer nivel
    );

    public SimularProyeccionFinanzasResponseDto execute(
            double presupuestoM,
            double pctNutricion,
            double pctMedicamentos,
            double pctDeteccion,
            double pctAtencion,
            int    hasta
    ) {
        int periodoInicio = 2025;

        Map<String, Double> distribucion = Map.of(
            "NUTRICION",    pctNutricion,
            "MEDICAMENTOS", pctMedicamentos,
            "DETECCION",    pctDeteccion,
            "ATENCION",     pctAtencion
        );

        // 1 — Calcula impacto total
        double impactoTotal = 0.0;
        for (Map.Entry<String, Double> entry : distribucion.entrySet()) {
            String rubro   = entry.getKey();
            double pct     = entry.getValue();
            double inversionM  = presupuestoM * (pct / 100.0);
            double cobertura   = Math.min(inversionM / COSTO_COBERTURA_TOTAL.get(rubro), 1.0);
            impactoTotal      += EFECTIVIDAD.get(rubro) * cobertura;
        }

        // 2 — Tasa reducida por la intervención
        double tasaReducida = Math.max(0, TASA_CRECIMIENTO_BASE * (1 - impactoTotal));

        // 3 — Curvas año por año
        List<PuntoProyeccion> puntos = new ArrayList<>();
        int años = hasta - periodoInicio;

        for (int i = 0; i <= años; i++) {
            int año = periodoInicio + i;
            double sinIntervencion = round2(PREVALENCIA_BASE_2024 * Math.pow(1 + TASA_CRECIMIENTO_BASE, i));
            double conIntervencion = round2(PREVALENCIA_BASE_2024 * Math.pow(1 + tasaReducida, i));
            puntos.add(new PuntoProyeccion(año, sinIntervencion, conIntervencion));
        }

        // 4 — KPIs al año final
        PuntoProyeccion ultimo = puntos.get(puntos.size() - 1);

        double reduccionPct = round1(
            ((ultimo.getConIntervencion() - ultimo.getSinIntervencion())
             / ultimo.getSinIntervencion()) * 100
        );

        long casosEvitados = Math.round(
            ((ultimo.getSinIntervencion() - ultimo.getConIntervencion()) / 100.0)
            * POBLACION_ADULTA
        );

        double ahorroUSD_M = Math.round(casosEvitados * GASTO_PER_CAPITA_USD / 1_000_000.0);

        double inversionTotalM = presupuestoM * años;
        double ahorroMXN_M    = ahorroUSD_M * TIPO_CAMBIO_MXN_USD;
        double ROI = inversionTotalM > 0
                ? round2(ahorroMXN_M / inversionTotalM)
                : 0.0;

        // 5 — Arma response
        SimularProyeccionFinanzasResponseDto response = new SimularProyeccionFinanzasResponseDto();
        response.setPuntos(puntos);
        response.setKpis(new KpisSimulacion(reduccionPct, casosEvitados, ahorroUSD_M, ROI));

        return response;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}