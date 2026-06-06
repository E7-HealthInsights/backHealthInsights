-- ============================================================
-- MIGRACIÓN: BD a español
-- Tablas afectadas: Widget, Proyeccion, Users → Usuario, Role → Rol
-- GCP Cloud SQL (MySQL)
-- ============================================================
-- INSTRUCCIONES:
--   1. Ejecutar en una ventana de mantenimiento (idealmente con el
--      servicio de Cloud Run en 0 instancias o en modo solo-lectura).
--   2. Hacer backup previo desde Cloud SQL Console antes de correr.
--   3. Verificar con los SELECT al final antes de confirmar deploy.
-- ============================================================

-- Desactivar revisión de FKs durante el rename para evitar errores de orden
SET FOREIGN_KEY_CHECKS = 0;

-- ─────────────────────────────────────────────────────────────
-- 1. Widget: columna title → titulo
-- ─────────────────────────────────────────────────────────────
ALTER TABLE Widget
    CHANGE title titulo VARCHAR(100) NOT NULL;

-- ─────────────────────────────────────────────────────────────
-- 2. Proyeccion: columna title → titulo
-- ─────────────────────────────────────────────────────────────
ALTER TABLE Proyeccion
    CHANGE title titulo VARCHAR(100) NOT NULL;

-- ─────────────────────────────────────────────────────────────
-- 3. Role → Rol  (renombrar tabla)
-- ─────────────────────────────────────────────────────────────
RENAME TABLE Role TO Rol;

-- 3a. Role: columna name → nombre
ALTER TABLE Rol
    CHANGE name nombre VARCHAR(200);

-- ─────────────────────────────────────────────────────────────
-- 4. Users → Usuario  (renombrar tabla)
-- ─────────────────────────────────────────────────────────────
RENAME TABLE Users TO Usuario;

-- 4a. Usuario: renombrar TODAS las columnas a español
ALTER TABLE Usuario
    CHANGE name           nombre       VARCHAR(50)  NOT NULL,
    CHANGE last_name      apellido     VARCHAR(100) NOT NULL,
    CHANGE email          correo       VARCHAR(100) NOT NULL,
    CHANGE role_id        rol_id       TINYINT,
    CHANGE status         estatus      BOOLEAN      DEFAULT true,
    CHANGE provider_id    proveedor_id VARCHAR(255) NOT NULL,
    CHANGE modified_by    modificado_por VARCHAR(36);

-- ─────────────────────────────────────────────────────────────
-- Reactivar FKs
-- ─────────────────────────────────────────────────────────────
SET FOREIGN_KEY_CHECKS = 1;

-- ─────────────────────────────────────────────────────────────
-- VERIFICACIÓN — correr después y revisar que todo se ve bien
-- ─────────────────────────────────────────────────────────────
SELECT 'Widget columns:' AS check_name;
SHOW COLUMNS FROM Widget;

SELECT 'Proyeccion columns:' AS check_name;
SHOW COLUMNS FROM Proyeccion;

SELECT 'Rol columns:' AS check_name;
SHOW COLUMNS FROM Rol;

SELECT 'Usuario columns:' AS check_name;
SHOW COLUMNS FROM Usuario;

SELECT 'FKs activas (debe verse rol_id apuntando a Rol, etc.):' AS check_name;
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME IN ('Usuario', 'Rol')
ORDER BY TABLE_NAME;
