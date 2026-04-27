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