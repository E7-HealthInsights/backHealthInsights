-- ============================================
-- STORED PROCEDURES PARA EJECUCIÓN DE WIDGETS
-- ============================================

USE `health_insights`;

DROP PROCEDURE IF EXISTS sp_widget_stat;
DROP PROCEDURE IF EXISTS sp_widget_series;
DROP PROCEDURE IF EXISTS sp_widget_pie;
DROP PROCEDURE IF EXISTS sp_widget_table;

DELIMITER $$

-- ─────────────────────────────────────────────
-- STAT: devuelve 1 valor agregado
-- Ejemplo: SUM(detecciones) → { value: 142300 }
-- ─────────────────────────────────────────────
CREATE PROCEDURE sp_widget_stat(
    IN p_tabla       VARCHAR(150),
    IN p_funcion     VARCHAR(10),
    IN p_columna     VARCHAR(100),
    IN p_filtro_col  VARCHAR(100),   -- filtro 1 (opcional)
    IN p_filtro_val  VARCHAR(255),
    IN p_filtro_col2 VARCHAR(100),   -- filtro 2 (opcional)
    IN p_filtro_val2 VARCHAR(255)
)
BEGIN
    SET @where = '';
    IF p_filtro_col IS NOT NULL AND p_filtro_val IS NOT NULL THEN
        SET @where = CONCAT(' WHERE `', p_filtro_col, '` = ', QUOTE(p_filtro_val));
    END IF;
    IF p_filtro_col2 IS NOT NULL AND p_filtro_val2 IS NOT NULL THEN
        IF @where = '' THEN
            SET @where = CONCAT(' WHERE `', p_filtro_col2, '` = ', QUOTE(p_filtro_val2));
        ELSE
            SET @where = CONCAT(@where, ' AND `', p_filtro_col2, '` = ', QUOTE(p_filtro_val2));
        END IF;
    END IF;

    SET @sql = CONCAT(
        'SELECT ', p_funcion, '(`', p_columna, '`) AS value ',
        'FROM `', p_tabla, '`',
        @where
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END$$

-- ─────────────────────────────────────────────
-- SERIES: devuelve pares x,y para LINE y BAR
-- Ejemplo: ano, SUM(detecciones) GROUP BY ano
-- ─────────────────────────────────────────────
CREATE PROCEDURE sp_widget_series(
    IN p_tabla       VARCHAR(150),
    IN p_col_x       VARCHAR(100),
    IN p_col_y       VARCHAR(100),
    IN p_funcion     VARCHAR(10),
    IN p_group_by    VARCHAR(100),
    IN p_filtro_col  VARCHAR(100),
    IN p_filtro_val  TEXT,
    IN p_filtro_col2 VARCHAR(100),
    IN p_filtro_val2 TEXT
)
BEGIN
    SET @where = '';
    IF p_filtro_col IS NOT NULL AND p_filtro_val IS NOT NULL THEN
        SET @where = CONCAT(' WHERE `', p_filtro_col, '` = ', QUOTE(p_filtro_val));
    END IF;
    IF p_filtro_col2 IS NOT NULL AND p_filtro_val2 IS NOT NULL THEN
        IF @where = '' THEN
            SET @where = CONCAT(' WHERE `', p_filtro_col2, '` = ', QUOTE(p_filtro_val2));
        ELSE
            SET @where = CONCAT(@where, ' AND `', p_filtro_col2, '` = ', QUOTE(p_filtro_val2));
        END IF;
    END IF;

    SET @sql = CONCAT(
        'SELECT `', p_col_x, '` AS label, ',
        p_funcion, '(`', p_col_y, '`) AS value ',
        'FROM `', p_tabla, '` ',
        @where, ' ',
        'GROUP BY `', p_col_x, '` ',
        'ORDER BY `', p_col_x, '`'
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END$$

-- ─────────────────────────────────────────────
-- PIE: devuelve pares label,value agrupados
-- Ejemplo: delegacion, SUM(detecciones)
-- ─────────────────────────────────────────────
CREATE PROCEDURE sp_widget_pie(
    IN p_tabla      VARCHAR(150),
    IN p_col_label  VARCHAR(100),
    IN p_col_value  VARCHAR(100),
    IN p_funcion    VARCHAR(10)
)
BEGIN
    SET @sql = CONCAT(
        'SELECT `', p_col_label, '` AS label, ',
        p_funcion, '(`', p_col_value, '`) AS value ',
        'FROM `', p_tabla, '` ',
        'GROUP BY `', p_col_label, '`',
        'ORDER BY value DESC'
    );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END$$

-- ─────────────────────────────────────────────
-- TABLE: devuelve todas las columnas indicadas
-- Ejemplo: SELECT col1, col2 FROM tabla LIMIT 100
-- ─────────────────────────────────────────────
CREATE PROCEDURE sp_widget_table(
    IN p_tabla    VARCHAR(150),
    IN p_columnas VARCHAR(500),
    IN p_limite   INT
)
BEGIN
    SET @sql = CONCAT(
        'SELECT ', p_columnas,
        ' FROM `', p_tabla, '`',
        ' LIMIT ', p_limite
    );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END$$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_widget_multiseries;
DELIMITER $$

CREATE PROCEDURE sp_widget_multiseries(
    IN p_tabla       VARCHAR(150),
    IN p_col_x       VARCHAR(100),
    IN p_col_y       VARCHAR(100),
    IN p_col_serie   VARCHAR(100),   -- columna que define cada serie
    IN p_funcion     VARCHAR(10),
    IN p_filtro_col  VARCHAR(100),   -- filtro 1 (opcional, NULL = sin filtro)
    IN p_filtro_val  VARCHAR(255),
    IN p_filtro_col2 VARCHAR(100),   -- filtro 2 (opcional, NULL = sin filtro)
    IN p_filtro_val2 VARCHAR(255)
)
BEGIN
    SET @where = '';
    IF p_filtro_col IS NOT NULL AND p_filtro_val IS NOT NULL THEN
        SET @where = CONCAT(' WHERE `', p_filtro_col, '` = ', QUOTE(p_filtro_val));
    END IF;
    IF p_filtro_col2 IS NOT NULL AND p_filtro_val2 IS NOT NULL THEN
        IF @where = '' THEN
            SET @where = CONCAT(' WHERE `', p_filtro_col2, '` = ', QUOTE(p_filtro_val2));
        ELSE
            SET @where = CONCAT(@where, ' AND `', p_filtro_col2, '` = ', QUOTE(p_filtro_val2));
        END IF;
    END IF;

    SET @sql = CONCAT(
        'SELECT `', p_col_x, '` AS label, ',
        '`', p_col_serie, '` AS serie, ',
        p_funcion, '(`', p_col_y, '`) AS value ',
        'FROM `', p_tabla, '`',
        @where, ' ',
        'GROUP BY `', p_col_x, '`, `', p_col_serie, '` ',
        'ORDER BY `', p_col_x, '`'
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END$$

DELIMITER ;