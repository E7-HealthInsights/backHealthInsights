CREATE TABLE Role (
                      id TINYINT AUTO_INCREMENT PRIMARY KEY,
                      name VARCHAR(200)
);

CREATE TABLE Users (
                       id VARCHAR(36) PRIMARY KEY,
                       name VARCHAR(50),
                       last_name VARCHAR(100),
                       email VARCHAR(100) UNIQUE,
                       role_id tinyint,
                       status BOOLEAN DEFAULT true,
                       provider_id VARCHAR(255),

                       FOREIGN KEY (role_id) REFERENCES Role(id)
);

CREATE TABLE Dataset (
                         id VARCHAR(36) PRIMARY KEY,
                         nombre VARCHAR(300),
                         nombre_tabla VARCHAR(150),
                         descripcion TEXT,
                         fuente VARCHAR(300),
                         archivo_csv VARCHAR(100),
                         link VARCHAR(500),
                         estado BOOLEAN DEFAULT true,
                         fecha_actualizacion DATETIME
);

CREATE TABLE Metrica (
                         id VARCHAR(36) PRIMARY KEY,
                         nombre VARCHAR(50),
                         columna_csv VARCHAR(50),
                         unidad VARCHAR(10),
                         dataset_id VARCHAR(36),
                         FOREIGN KEY (dataset_id) REFERENCES Dataset(id)
);

-- Tabla Tipo de Grafica
CREATE TABLE Tipo_de_Grafica (
                                 id TINYINT AUTO_INCREMENT PRIMARY KEY,
                                 nombre VARCHAR(100)
);

-- Tabla Widget
CREATE TABLE Widget (
                        id VARCHAR(36) PRIMARY KEY,
                        title VARCHAR(100),
                        usuario_id VARCHAR(36) NULL,
                        rol_id TINYINT NULL,
                        tipo_id TINYINT,
                        query text,
                        orden int,

                        FOREIGN KEY (usuario_id) REFERENCES Users(id),
                        FOREIGN KEY (rol_id) REFERENCES Role(id),
                        FOREIGN KEY (tipo_id) REFERENCES Tipo_de_Grafica(id)
);

-- Tabla Proyeccion
CREATE TABLE Proyeccion (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(100),
    descripcion text,
    usuario_id VARCHAR(36),
    query text,
    fecha_creacion DATETIME,
    fecha_actualizacion DATETIME,

    FOREIGN KEY (usuario_id) REFERENCES Users(id)
);



-- ── Roles ─────────────────────────────────────────────────────────────────────

INSERT INTO Role VALUES (1, 'ADMIN');
INSERT INTO Role VALUES (2, 'DIRECTOR_GENERAL');
INSERT INTO Role VALUES (3, 'DIRECTOR_FINANZAS');
INSERT INTO Role VALUES (4, 'DIRECTOR_MERCADOTECNIA');

-- ── Usuarios de prueba ────────────────────────────────────────────────────────

INSERT INTO Users (id, name, last_name, email, role_id, status, provider_id)
VALUES ('08631269-3f4c-4299-a1e7-23f5684e1091', 'Santiago', 'Niño', 'santiago.nino@example.com', 1, true, 'i8AULkutUNTy9xIUyp2lpHczMHi2');

INSERT INTO Users (id, name, last_name, email, role_id, status, provider_id)
VALUES ('b2d4f8a1-6c3e-4f2a-9d5b-7e8c1a0f3d42', 'Gabriel', 'Gutiérrez', 'gabogg2004@gmail.com', 2, true, '855m35Eei6Zla4ewGGbQkLTRHow1');

INSERT INTO Users (id, name, last_name, email, role_id, status, provider_id)
VALUES ('c3e5f9b2-7d4f-5a3b-ae6c-8f9d2b1e4c53', 'Admin', 'Admin', 'admin@gmail.com', 1, true, 'JWYXnZE8uAckSn67K8QhXa7PvA92');

INSERT INTO Users (id, name, last_name, email, role_id, status, provider_id)
VALUES ('84f3167c-7088-4d63-8f8f-bedc1f95e080', 'Alejandra', 'Finanzas', 'alejandra@example.com', 3, true, 'SfqxwVKxPmdGyNE2ekhX9SAKWg82');

INSERT INTO Users (id, name, last_name, email, role_id, status, provider_id)
VALUES ('84f3167c-7088-4d63-8f8f-bedc1f95e982', 'Sara', 'Merca', 'merca@gmail.com', 4, true, 'XvqjxqVrRfaeHzmVc9iyFvsUCKG2');

-- ── Datasets de prueba ────────────────────────────────────────────────────────

INSERT INTO Dataset (id, nombre, nombre_tabla, descripcion, fuente, estado, fecha_actualizacion) VALUES
                                                                                                     ('e1000000-0000-0000-0000-000000000001', 'Diabetes México 2023',   'diabetes_2023',    'Casos y defunciones por diabetes en México, desglosados por estado y sexo.', 'SINAVE / Secretaría de Salud', true, '2024-01-15 00:00:00'),
                                                                                                     ('e2000000-0000-0000-0000-000000000002', 'Hipertensión 2022',      'hipertension_2022','Prevalencia de hipertensión arterial por entidad federativa.',              'ENSANUT 2022',                true, '2023-06-01 00:00:00'),
                                                                                                     ('e3000000-0000-0000-0000-000000000003', 'Obesidad Nacional 2021', 'obesidad_2021',    'Índice de masa corporal promedio y porcentaje de obesidad por estado.',     'INEGI / ENSANUT',             true, '2022-03-20 00:00:00');

-- ── Métricas: Diabetes México 2023 ───────────────────────────────────────────

INSERT INTO Metrica (id, nombre, columna_csv, unidad, dataset_id) VALUES
                                                                      ('11000000-0000-0000-0000-000000000001', 'Estado',        'estado',        NULL,  'e1000000-0000-0000-0000-000000000001'),
                                                                      ('11000000-0000-0000-0000-000000000002', 'Año',           'anio',          NULL,  'e1000000-0000-0000-0000-000000000001'),
                                                                      ('11000000-0000-0000-0000-000000000003', 'Casos',         'casos',         NULL,  'e1000000-0000-0000-0000-000000000001'),
                                                                      ('11000000-0000-0000-0000-000000000004', 'Defunciones',   'defunciones',   NULL,  'e1000000-0000-0000-0000-000000000001'),
                                                                      ('11000000-0000-0000-0000-000000000005', 'Edad promedio', 'edad_promedio', 'años','e1000000-0000-0000-0000-000000000001'),
                                                                      ('11000000-0000-0000-0000-000000000006', 'Sexo',          'sexo',          NULL,  'e1000000-0000-0000-0000-000000000001'),
                                                                      ('11000000-0000-0000-0000-000000000007', 'Presupuesto',   'presupuesto',   'MXN', 'e1000000-0000-0000-0000-000000000001');

-- ── Métricas: Hipertensión 2022 ──────────────────────────────────────────────

INSERT INTO Metrica (id, nombre, columna_csv, unidad, dataset_id) VALUES
                                                                      ('22000000-0000-0000-0000-000000000001', 'Estado',        'estado',        NULL,  'e2000000-0000-0000-0000-000000000002'),
                                                                      ('22000000-0000-0000-0000-000000000002', 'Año',           'anio',          NULL,  'e2000000-0000-0000-0000-000000000002'),
                                                                      ('22000000-0000-0000-0000-000000000003', 'Casos',         'casos',         NULL,  'e2000000-0000-0000-0000-000000000002'),
                                                                      ('22000000-0000-0000-0000-000000000004', 'Prevalencia',   'prevalencia',   '%',   'e2000000-0000-0000-0000-000000000002'),
                                                                      ('22000000-0000-0000-0000-000000000005', 'Edad promedio', 'edad_promedio', 'años','e2000000-0000-0000-0000-000000000002'),
                                                                      ('22000000-0000-0000-0000-000000000006', 'Sexo',          'sexo',          NULL,  'e2000000-0000-0000-0000-000000000002');

-- ── Métricas: Obesidad Nacional 2021 ─────────────────────────────────────────

INSERT INTO Metrica (id, nombre, columna_csv, unidad, dataset_id) VALUES
                                                                      ('33000000-0000-0000-0000-000000000001', 'Estado',       'estado',       NULL, 'e3000000-0000-0000-0000-000000000003'),
                                                                      ('33000000-0000-0000-0000-000000000002', 'Año',          'anio',         NULL, 'e3000000-0000-0000-0000-000000000003'),
                                                                      ('33000000-0000-0000-0000-000000000003', 'IMC Promedio', 'imc_promedio', NULL, 'e3000000-0000-0000-0000-000000000003'),
                                                                      ('33000000-0000-0000-0000-000000000004', 'Población',    'poblacion',    NULL, 'e3000000-0000-0000-0000-000000000003'),
                                                                      ('33000000-0000-0000-0000-000000000005', 'Porcentaje',   'porcentaje',   '%',  'e3000000-0000-0000-0000-000000000003'),
                                                                      ('33000000-0000-0000-0000-000000000006', 'Sexo',         'sexo',         NULL, 'e3000000-0000-0000-0000-000000000003');


-- Tipos de gráfica
INSERT INTO Tipo_de_Grafica (id, nombre) VALUES (1, 'STAT');
INSERT INTO Tipo_de_Grafica (id, nombre) VALUES (2, 'LINE');
INSERT INTO Tipo_de_Grafica (id, nombre) VALUES (3, 'BAR');
INSERT INTO Tipo_de_Grafica (id, nombre) VALUES (4, 'PIE');
INSERT INTO Tipo_de_Grafica (id, nombre) VALUES (5, 'TABLE');
INSERT INTO Tipo_de_Grafica (id, nombre) VALUES (6, 'MULTISERIES');
INSERT INTO Tipo_de_Grafica (id, nombre) VALUES (7, 'MULTIBAR');

-- ================================================
-- WIDGETS DEFAULT DIRECTOR FINANZAS (usuario_id = '3')
-- ================================================

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
    (UUID(), 'Gasto Público de México, 2024', NULL, 1,
     '{\"tabla\": \"f12_idf_mexico_dolares\", \"funcion\": \"MAX\", \"columna\": \"value\", \"filtroCol\": \"_id\", \"filtroVal\": \"1\" }',
     1, 3);

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
    (UUID(), 'Gasto Per Cápita, 2024', NULL, 1,
     '{\"tabla\": \"f12_idf_mexico_dolares\", \"funcion\": \"MAX\", \"columna\": \"value\", \"filtroCol\": \"_id\", \"filtroVal\": \"4\" }',
     2, 3);

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
    (UUID(), 'Promedio histórico del PIB dedicado al sector salud', NULL, 1,
     '{\"tabla\": \"f4_pib_bancomundial\", \"funcion\": \"AVG\", \"columna\": \"obs_value\" }',
     3, 3);

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
    (UUID(), 'Evolución gasto % PIB 2000–2024', NULL, 3,
     '{"tabla":"f4_pib_bancomundial","colX":"time_period","colY":"obs_value","funcion":"AVG","groupBy":"time_period"}',
     4, 3);

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Cobertura pública vs privada (evolución)', NULL, 6,
 '{"tabla":"f11_health_coverage_oecd_pt","colX":"time_period","colY":"obs_value","colSerie":"insurance_type","funcion":"MAX"}',
 5, 3);

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Gasto en Diabetes vs otros países', NULL, 6,
 '{"tabla":"f10_gasto_diabetes","colX":"ao","colY":"gastomillones_de_dolares","colSerie":"pais","funcion":"MAX"}',
 6, 3);

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Distribucion de cobertura por tipo de seguro', NULL, 4,
 '{"tabla":"f11_health_coverage_oecd_pt","colLabel":"insurance_type","colValue":"obs_value","funcion":"AVG"}',
 7, 3);

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
    (UUID(), 'Gasto per cápita en diabetes evolución', NULL, 2,
     '{"tabla":"f12_idf_mexico_dolares","colX":"year","colY":"value","funcion":"MAX","groupBy":"year", \"filtroCol\": \"indicator\", \"filtroVal\": \"Diabetes-related health expenditure per person (USD)\"}',
     8, 3);

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Gasto vs Casos de diabetes (índice 2000=100)', NULL, 6,
 '{"tabla":"f12_idf_gastovscasos","colX":"ano","colY":"valor","colSerie":"serie","funcion":"MAX"}',
 9, 3);

-- ================================================
-- WIDGETS DEFAULT DIRECTOR MERCADOTECNIA (rol_id = 4)
-- HU HI-485 - subtareas HI-504..HI-511
-- ================================================

-- HI-504: StatCard con no diagnosticados (vista calcula 100 - cobertura)
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), '% Personas con diabetes no diagnosticadas', NULL, 1,
 '{"tabla":"v_f8_no_diagnosticados_actual","funcion":"MAX","columna":"porcentaje_no_diagnosticados"}',
 1, 4);

-- HI-505: StatCard con estado prioritario (nombre del estado top en detecciones)
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Estado prioritario por detecciones', NULL, 1,
 '{"tabla":"v_f5_estado_prioritario_actual","funcion":"MAX","columna":"nombre_estado"}',
 2, 4);

-- HI-506: StatCard con % población con diabetes (prevalencia age-standardized 2024)
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), '% Población con diabetes (2024)', NULL, 1,
 '{"tabla":"f12_idf_mexico_porcentajes","funcion":"MAX","columna":"value","filtroCol":"indicator","filtroVal":"Age-standardised prevalence of diabetes (%)","filtroCol2":"year","filtroVal2":"2024"}',
 3, 4);

-- HI-507: Barras edad y sexo vs diabetes (DALYs Rate por edad, 3 series de sexo, año 2021)
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Carga de diabetes (DALYs) por edad y sexo', NULL, 6,
 '{"tabla":"f7_burden_diabetes","colX":"age_group","colY":"value","colSerie":"sex","funcion":"MAX","filtroCol":"measure_name","filtroVal":"Disability-Adjusted Life Years (DALYs)","filtroCol2":"year","filtroVal2":"2021"}',
 4, 4);

-- HI-508: Línea detecciones por año (SUM de todos los estados, sin filtros)
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Detecciones de diabetes por año', NULL, 2,
 '{"tabla":"f5_diabetes_delegaciones","colX":"ano","colY":"detecciones","funcion":"SUM","groupBy":"ano"}',
 5, 4);

-- HI-509: Barras obesidad y sobrepeso por edad (multiseries, ambos sexos, año 2022)
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Obesidad y sobrepeso por edad', NULL, 6,
 '{"tabla":"f9_obesidad_paho","colX":"age_group","colY":"value","colSerie":"indicator_name","funcion":"MAX","filtroCol":"sex","filtroVal":"Both sexes","filtroCol2":"year","filtroVal2":"2022"}',
 6, 4);

-- HI-510: Mapa de calor con carencias sociales por municipio (TABLE con 1 fila por municipio)
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Carencias sociales por municipio', NULL, 5,
 '{"tabla":"v_f2_carencias_por_municipio","columnas":"municipio, rezago_educativo, acceso_salud, acceso_seguridad_social, calidad_vivienda, servicios_basicos, acceso_alimentacion","limite":20}',
 7, 4);

-- HI-511: Tabla top 5 estados por detecciones (vista pre-ordenada)
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Top 5 estados con más detecciones', NULL, 5,
 '{"tabla":"v_f5_top_estados_actual","columnas":"estado, detecciones_total","limite":5}',
 8, 4);

-- ================================================
-- WIDGETS DEFAULT DIRECTOR GENERAL (usuario_id = '2')
-- ================================================

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
    (UUID(), 'Casos de diabetes en México', NULL, 1,
     '{\"tabla\": \"fuente1_datos_casos_ssa\", \"funcion\": \"MAX\", \"columna\": \"Valor\", \"filtroCol\": \"_id\", \"filtroVal\": \"36\" }',
     1, 2);

INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
    (UUID(), 'Tasa de mortalidad por diabetes (100 mil habitantes)', NULL, 1,
     '{\"tabla\": \"fuente1_datos_casos_ssa\", \"funcion\": \"MAX\", \"columna\": \"Valor\", \"filtroCol\": \"_id\", \"filtroVal\": \"108\" }',
     2, 2);

-- StatCard personas no diagnosticadas 2024
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Personas con diabetes no diagnosticadas (2024)', NULL, 1,
 '{"tabla":"f12_idf_mexico_limpio","funcion":"MAX","columna":"value","filtroCol":"indicator","filtroVal":"People with undiagnosed diabetes (1000s)","filtroCol2":"year","filtroVal2":"2024"}',
 3, 2);

-- Línea Prevalencia 1990-2022
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Prevalencia de diabetes en México (1990-2022)', NULL, 2,
 '{"tabla":"f8_prevalence_and_treatment_diabetes","colX":"year","colY":"value","funcion":"MAX","groupBy":"year","filtroCol":"indicator_name_en","filtroVal":"Prevalence of diabetes in adults aged 18+ years (FBG ≥7.0 mmol/L or HbA1c ≥6.5% or currently taking medication for diabetes) (crude estimates)","filtroCol2":"sex_en","filtroVal2":"Both sexes"}',
 4, 2);

-- Heatmap defunciones por estado y año
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Defunciones por diabetes según estado y año', NULL, 5,
 '{"tabla":"v_f3_defunciones_heatmap","columnas":"ent_regis, anio_ocur, defunciones","limite":500}',
 10, 2);

-- Multibar tasa de muertes por edad y sexo
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Tasa de mortalidad por diabetes según edad y sexo', NULL, 7,
 '{"tabla":"f7_burden_diabetes","colX":"age_group","colY":"value","colSerie":"sex","funcion":"MAX","filtroCol":"measure_name_en","filtroVal":"Deaths","filtroCol2":"metric_name_en","filtroVal2":"Rate"}',
 5, 2);

-- Dona %PIB destinado a salud vs resto (2024)
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), '% PIB destinado a salud (2024)', NULL, 4,
 '{"tabla":"v_pib_dona_2024","colLabel":"categoria","colValue":"valor","funcion":"MAX"}',
 6, 2);

-- Línea DALYs / carga de la enfermedad
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Carga de enfermedad por diabetes (DALYs)', NULL, 2,
 '{"tabla":"f7_burden_diabetes","colX":"year","colY":"value","funcion":"MAX","groupBy":"year","filtroCol":"measure_name_en","filtroVal":"Disability-Adjusted Life Years (DALYs)","filtroCol2":"age_group","filtroVal2":"All ages"}',
 7, 2);

-- Línea personas con diabetes por año
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Personas con diabetes en México por año', NULL, 2,
 '{"tabla":"fuente1_datos_casos_ssa","colX":"ao","colY":"valor","funcion":"MAX","groupBy":"ao","filtroCol":"indicador","filtroVal":"Casos (millones)"}',
 8, 2);

-- Línea muertes por año
INSERT INTO Widget (id, title, usuario_id, tipo_id, query, orden, rol_id) VALUES
(UUID(), 'Muertes por diabetes en México por año', NULL, 2,
 '{"tabla":"fuente1_datos_casos_ssa","colX":"ao","colY":"valor","funcion":"MAX","groupBy":"ao","filtroCol":"indicador","filtroVal":"Muertes(miles)"}',
 9, 2);
