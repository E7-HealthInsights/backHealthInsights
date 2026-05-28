USE `health_insights`;

drop trigger if exists trg_usuario_after_insert;
drop trigger if exists trg_usuario_after_update;
drop trigger if exists trg_dataset_after_insert;
drop trigger if exists trg_dataset_after_update;

DELIMITER $$

-- Trigger 1: AFTER INSERT en Usuario
-- Registra cuando el admin crea un nuevo usuario
CREATE TRIGGER trg_usuario_after_insert
AFTER INSERT ON Users
FOR EACH ROW
BEGIN
    INSERT INTO LogActividad (
        id, usuario_id, accion, detalle, entidad_tipo, entidad_id, fecha
    ) VALUES (
        UUID(),
        NEW.modified_by,
        CONCAT('Usuario creado: ', NEW.name, ' ', NEW.last_name),
        CONCAT('Email: ', NEW.email, ' | Rol: ', NEW.role_id),
        'USUARIO',
        NEW.id,
        NOW()
    );
END$$

-- Trigger 2: AFTER UPDATE en Usuario
-- Distingue entre baja lógica y edición de datos
CREATE TRIGGER trg_usuario_after_update
AFTER UPDATE ON Users
FOR EACH ROW
BEGIN
    -- Baja lógica
    IF OLD.status = 1 AND NEW.status = 0 THEN
        INSERT INTO LogActividad (
            id, usuario_id, accion, detalle, entidad_tipo, entidad_id, fecha
        ) VALUES (
            UUID(),
            NEW.modified_by,
            CONCAT('Usuario desactivado: ', NEW.name, ' ', NEW.last_name),
            CONCAT('Email: ', NEW.email),
            'USUARIO',
            NEW.id,
            NOW()
        );
    -- Reactivación
    ELSEIF OLD.status = 0 AND NEW.status = 1 THEN
        INSERT INTO LogActividad (
            id, usuario_id, accion, detalle, entidad_tipo, entidad_id, fecha
        ) VALUES (
            UUID(),
            NEW.modified_by,
            CONCAT('Usuario reactivado: ', NEW.name, ' ', NEW.last_name),
            CONCAT('Email: ', NEW.email),
            'USUARIO',
            NEW.id,
            NOW()
        );
    -- Edición general
    ELSE
        INSERT INTO LogActividad (
            id, usuario_id, accion, detalle, entidad_tipo, entidad_id, fecha
        ) VALUES (
            UUID(),
            NEW.modified_by,
            CONCAT('Usuario editado: ', NEW.name, ' ', NEW.last_name),
            NULL,
            'USUARIO',
            NEW.id,
            NOW()
        );
    END IF;
END$$

-- Trigger 3: AFTER INSERT en Dataset
-- Registra cuando el admin sube un nuevo dataset
CREATE TRIGGER trg_dataset_after_insert
AFTER INSERT ON Dataset
FOR EACH ROW
BEGIN
    INSERT INTO LogActividad (
        id, usuario_id, accion, detalle, entidad_tipo, entidad_id, fecha
    ) VALUES (
        UUID(),
        NEW.modified_by,
        CONCAT('Dataset creado: ', NEW.nombre),
        CONCAT('Tabla: ', NEW.nombre_tabla, ' | Fuente: ', IFNULL(NEW.fuente, 'N/A')),
        'DATASET',
        NEW.id,
        NOW()
    );
END$$

-- Trigger 4: AFTER UPDATE en Dataset
-- Distingue entre desactivación y reactivación
CREATE TRIGGER trg_dataset_after_update
AFTER UPDATE ON Dataset
FOR EACH ROW
BEGIN
    -- Desactivación
    IF OLD.estado != 'INACTIVE' AND NEW.estado = 'INACTIVE' THEN
        INSERT INTO LogActividad (
            id, usuario_id, accion, detalle, entidad_tipo, entidad_id, fecha
        ) VALUES (
            UUID(),
            NEW.modified_by,
            CONCAT('Dataset desactivado: ', NEW.nombre),
            CONCAT('Tabla: ', NEW.nombre_tabla, ' | Fuente: ', IFNULL(NEW.fuente, 'N/A')),
            'DATASET',
            NEW.id,
            NOW()
        );
    -- Reactivación
    ELSEIF OLD.estado = 'INACTIVE' AND NEW.estado = 'READY' THEN
        INSERT INTO LogActividad (
            id, usuario_id, accion, detalle, entidad_tipo, entidad_id, fecha
        ) VALUES (
            UUID(),
            NEW.modified_by,
            CONCAT('Dataset reactivado: ', NEW.nombre),
            CONCAT('Tabla: ', NEW.nombre_tabla, ' | Fuente: ', IFNULL(NEW.fuente, 'N/A')),
            'DATASET',
            NEW.id,
            NOW()
        );
    END IF;
END$$

DELIMITER ;