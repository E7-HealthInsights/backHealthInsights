USE `health_insights`;

DROP FUNCTION IF EXISTS fn_total_usuarios;
DROP FUNCTION IF EXISTS fn_total_datasets;

DELIMITER $$

CREATE FUNCTION fn_total_usuarios(
    p_rol_id TINYINT,   -- NULL = todos los roles
    p_status BOOLEAN    -- TRUE = activos, FALSE = inactivos
)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE total INT;

    IF p_rol_id IS NULL THEN
        SELECT COUNT(*) INTO total
        FROM Usuario
        WHERE estatus = p_status;
    ELSE
        SELECT COUNT(*) INTO total
        FROM Usuario
        WHERE estatus  = p_status
          AND rol_id = p_rol_id;
    END IF;

    RETURN total;
END$$


CREATE FUNCTION fn_total_datasets(
    p_estado VARCHAR(20) CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci
)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE total INT;

    SELECT COUNT(*) INTO total
    FROM Dataset
    WHERE estado = p_estado;

    RETURN total;
END$$

DELIMITER ;