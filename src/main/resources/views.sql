-- ============================================
-- VISTAS AUXILIARES PARA WIDGETS DEFAULT
--
-- Estas vistas pre-calculan agregaciones que los SPs
-- genéricos (sp_widget_stat / sp_widget_table) no
-- pueden hacer en una sola llamada (cálculos derivados,
-- top-N con SUM/GROUP BY, agregación por dimensión).
-- ============================================

USE `health_insights`;

-- ─── HI-505 StatCard "Estado prioritario" ────────────────────────────
-- Devuelve el nombre del estado con más detecciones en el año más reciente.
DROP VIEW IF EXISTS v_f5_estado_prioritario_actual;
CREATE VIEW v_f5_estado_prioritario_actual AS
SELECT delegacion AS nombre_estado,
       SUM(detecciones) AS total_detecciones
FROM f5_diabetes_delegaciones
WHERE ano = (SELECT MAX(ano) FROM f5_diabetes_delegaciones)
GROUP BY delegacion
ORDER BY total_detecciones DESC
LIMIT 1;

-- ─── HI-511 Tabla "Top 5 estados" ────────────────────────────────────
-- Ranking de estados por detecciones en el año más reciente.
DROP VIEW IF EXISTS v_f5_top_estados_actual;
CREATE VIEW v_f5_top_estados_actual AS
SELECT delegacion AS estado,
       SUM(detecciones) AS detecciones_total
FROM f5_diabetes_delegaciones
WHERE ano = (SELECT MAX(ano) FROM f5_diabetes_delegaciones)
GROUP BY delegacion
ORDER BY detecciones_total DESC;

-- ─── HI-510 Heatmap "Carencias sociales por municipio" ───────────────
-- Promedio de cada carencia por municipio (colapsa catg_edad y sexo_int).
DROP VIEW IF EXISTS v_f2_carencias_por_municipio;
CREATE VIEW v_f2_carencias_por_municipio AS
SELECT municipio,
       ROUND(AVG(rez_edu), 2)      AS rezago_educativo,
       ROUND(AVG(acc_sal), 2)      AS acceso_salud,
       ROUND(AVG(acc_seg_soc), 2)  AS acceso_seguridad_social,
       ROUND(AVG(car_cal_viv), 2)  AS calidad_vivienda,
       ROUND(AVG(car_serv_bas), 2) AS servicios_basicos,
       ROUND(AVG(car_acc_ali), 2)  AS acceso_alimentacion
FROM f2_pobreza_diabetes_2015
GROUP BY municipio
ORDER BY municipio;

-- ─── HI-504 StatCard "% No diagnosticados" ───────────────────────────
-- Complemento de la cobertura de tratamiento (age-standardized, 30+ años, Both sexes)
-- del último año disponible. Aproximación: quien no está en tratamiento
-- generalmente no está diagnosticado.
DROP VIEW IF EXISTS v_f8_no_diagnosticados_actual;
CREATE VIEW v_f8_no_diagnosticados_actual AS
SELECT ROUND(100 - value, 2) AS porcentaje_no_diagnosticados
FROM f8_prevalence_treatment_diabetes
WHERE indicator_name LIKE 'Diabetes treatment coverage%age-standardized%'
  AND sex = 'Both sexes'
  AND age_group = '30+ years'
  AND year = (
      SELECT MAX(year)
      FROM f8_prevalence_treatment_diabetes
      WHERE indicator_name LIKE 'Diabetes treatment coverage%age-standardized%'
  );
