DROP DATABASE IF EXISTS techzone;
CREATE DATABASE techzone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE techzone;

-- =============================================================================
-- TABLAS DE CATALOGOS
-- =============================================================================

CREATE TABLE categoria (
    id_categoria   INT          NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(80)  NOT NULL,
    descripcion    VARCHAR(255),
    activo         TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id_categoria),
    INDEX idx_categoria_activo (activo)
);

CREATE TABLE metodo_pago (
    id_metodo_pago INT         NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(60) NOT NULL,
    activo         TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (id_metodo_pago)
);

CREATE TABLE tipo_documento (
    id_tipo_documento    INT          NOT NULL AUTO_INCREMENT,
    descripcion          VARCHAR(100) NOT NULL,
    efecto_en_inventario TINYINT      NOT NULL COMMENT '+1 = entrada, -1 = salida',
    PRIMARY KEY (id_tipo_documento),
    CONSTRAINT chk_efecto CHECK (efecto_en_inventario IN (1, -1))
);

CREATE TABLE cargo (
    id_cargo    INT         NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(80) NOT NULL,
    descripcion VARCHAR(255),
    PRIMARY KEY (id_cargo)
);

-- =============================================================================
-- TABLAS DE PERSONAS
-- =============================================================================

CREATE TABLE persona (
    id_persona   INT          NOT NULL AUTO_INCREMENT,
    tipo         ENUM('CLIENTE','EMPLEADO','PROVEEDOR') NOT NULL,
    nombres      VARCHAR(100) NOT NULL,
    apellidos    VARCHAR(100) NOT NULL,
    documento    VARCHAR(20)  NOT NULL,
    telefono     VARCHAR(30),
    email        VARCHAR(120),
    direccion    VARCHAR(255),
    activo       TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id_persona),
    UNIQUE INDEX idx_persona_documento (documento),
    UNIQUE INDEX idx_persona_email (email),
    INDEX idx_persona_tipo (tipo),
    INDEX idx_persona_activo (activo)
);

CREATE TABLE cliente (
    id_persona       INT          NOT NULL,
    fecha_registro   DATE         NOT NULL DEFAULT (CURRENT_DATE),
    contrasena_hash  VARCHAR(64),
    PRIMARY KEY (id_persona),
    CONSTRAINT fk_cliente_persona FOREIGN KEY (id_persona) REFERENCES persona(id_persona)
);

CREATE TABLE empleado (
    id_persona       INT          NOT NULL,
    id_cargo         INT          NOT NULL,
    fecha_ingreso    DATE         NOT NULL,
    contrasena_hash  VARCHAR(64)  NOT NULL,
    salario          DECIMAL(12,2) DEFAULT 0,
    PRIMARY KEY (id_persona),
    CONSTRAINT fk_empleado_persona FOREIGN KEY (id_persona) REFERENCES persona(id_persona),
    CONSTRAINT fk_empleado_cargo   FOREIGN KEY (id_cargo)   REFERENCES cargo(id_cargo)
);

CREATE TABLE proveedor (
    id_persona         INT          NOT NULL,
    nombre_empresa     VARCHAR(150) NOT NULL,
    nit                VARCHAR(30),
    PRIMARY KEY (id_persona),
    CONSTRAINT fk_proveedor_persona FOREIGN KEY (id_persona) REFERENCES persona(id_persona),
    UNIQUE INDEX idx_proveedor_nit (nit)
);

-- =============================================================================
-- TABLAS DE PRODUCTOS E INVENTARIO
-- =============================================================================

CREATE TABLE producto (
    id_producto      INT            NOT NULL AUTO_INCREMENT,
    id_categoria     INT            NOT NULL,
    nombre           VARCHAR(150)   NOT NULL,
    descripcion      TEXT,
    precio_compra    DECIMAL(12,2)  NOT NULL,
    precio_venta     DECIMAL(12,2)  NOT NULL,
    stock_actual     INT            NOT NULL DEFAULT 0,
    stock_minimo     INT            NOT NULL DEFAULT 0,
    activo           TINYINT(1)     NOT NULL DEFAULT 1,
    PRIMARY KEY (id_producto),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria),
    INDEX idx_producto_nombre (nombre),
    INDEX idx_producto_categoria (id_categoria),
    INDEX idx_producto_activo (activo)
);

-- =============================================================================
-- TABLAS DE DOCUMENTOS (VENTAS, COMPRAS)
-- =============================================================================

CREATE TABLE documento (
    id_documento       INT            NOT NULL AUTO_INCREMENT,
    id_tipo_documento  INT            NOT NULL,
    id_persona         INT            NOT NULL,
    -- CORRECCION: id_empleado acepta NULL (compras online sin cajero asignado)
    id_empleado        INT            NULL,
    id_metodo_pago     INT            NULL,
    fecha_documento    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    numero_doc_externo VARCHAR(60),
    subtotal           DECIMAL(14,2)  NOT NULL DEFAULT 0,
    descuento          DECIMAL(14,2)  NOT NULL DEFAULT 0,
    total              DECIMAL(14,2)  NOT NULL DEFAULT 0,
    -- Campo estado para tracking: PENDIENTE, COMPLETADA, ANULADA
    estado             ENUM('PENDIENTE','COMPLETADA','ANULADA') NOT NULL DEFAULT 'COMPLETADA',
    observaciones      TEXT,
    PRIMARY KEY (id_documento),
    CONSTRAINT fk_doc_tipo      FOREIGN KEY (id_tipo_documento) REFERENCES tipo_documento(id_tipo_documento),
    CONSTRAINT fk_doc_persona   FOREIGN KEY (id_persona)        REFERENCES persona(id_persona),
    -- CORRECCION: FK con ON DELETE SET NULL para que no bloquee si se desactiva empleado
    CONSTRAINT fk_doc_empleado  FOREIGN KEY (id_empleado)       REFERENCES empleado(id_persona) ON DELETE SET NULL,
    CONSTRAINT fk_doc_metpago   FOREIGN KEY (id_metodo_pago)    REFERENCES metodo_pago(id_metodo_pago),
    INDEX idx_doc_persona (id_persona),
    INDEX idx_doc_fecha (fecha_documento),
    INDEX idx_doc_tipo (id_tipo_documento),
    INDEX idx_doc_estado (estado)
);

CREATE TABLE movimiento_inventario (
    id_movimiento      INT            NOT NULL AUTO_INCREMENT,
    id_documento       INT            NOT NULL,
    id_producto        INT            NOT NULL,
    -- CORRECCION: id_empleado acepta NULL igual que en documento
    id_empleado        INT            NULL,
    cantidad           INT            NOT NULL,
    precio_unitario    DECIMAL(12,2)  NOT NULL DEFAULT 0,
    -- CORRECCION: subtotal_linea es GENERATED — el DAO NO debe insertarlo
    subtotal_linea     DECIMAL(14,2)  GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
    fecha_movimiento   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_mov_documento FOREIGN KEY (id_documento) REFERENCES documento(id_documento),
    CONSTRAINT fk_mov_producto  FOREIGN KEY (id_producto)  REFERENCES producto(id_producto),
    CONSTRAINT fk_mov_empleado  FOREIGN KEY (id_empleado)  REFERENCES empleado(id_persona) ON DELETE SET NULL,
    INDEX idx_mov_producto (id_producto),
    INDEX idx_mov_documento (id_documento),
    INDEX idx_mov_fecha (fecha_movimiento)
);

-- =============================================================================
-- TABLAS DE CARRITO
-- =============================================================================

CREATE TABLE carrito (
    id_carrito   INT       NOT NULL AUTO_INCREMENT,
    id_cliente   INT       NOT NULL,
    creado_en    DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_carrito),
    CONSTRAINT fk_carrito_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_persona),
    INDEX idx_carrito_cliente (id_cliente)
);

CREATE TABLE item_carrito (
    id_item      INT  NOT NULL AUTO_INCREMENT,
    id_carrito   INT  NOT NULL,
    id_producto  INT  NOT NULL,
    cantidad     INT  NOT NULL DEFAULT 1,
    PRIMARY KEY (id_item),
    CONSTRAINT fk_item_carrito  FOREIGN KEY (id_carrito)  REFERENCES carrito(id_carrito) ON DELETE CASCADE,
    CONSTRAINT fk_item_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    -- CORRECCION: un producto solo puede aparecer una vez por carrito
    UNIQUE KEY uq_carrito_producto (id_carrito, id_producto)
);


CREATE OR REPLACE VIEW vista_persona_cliente AS
SELECT
    p.id_persona,
    p.nombres     AS nombre,
    p.apellidos   AS apellido,
    p.documento,
    p.telefono,
    p.email,
    p.direccion,
    p.activo,
    c.contrasena_hash
FROM persona p
JOIN cliente c ON p.id_persona = c.id_persona
WHERE p.tipo = 'CLIENTE'
  AND p.activo = 1;

-- =============================================================================
-- DATOS INICIALES: catalogos
-- =============================================================================

INSERT INTO tipo_documento (descripcion, efecto_en_inventario) VALUES
    ('Factura de Venta al Cliente',        -1),
    ('Factura de Compra a Proveedor',      +1),
    ('Devolucion del Cliente',             +1),
    ('Devolucion a Proveedor',             -1),
    ('Ajuste de Inventario (Entrada)',     +1),
    ('Ajuste de Inventario (Salida)',      -1),
    ('Baja por mercancia en mal estado',   -1);

INSERT INTO cargo (id_cargo, nombre, descripcion) VALUES
    (1, 'Administrador', 'Acceso completo al sistema'),
    (2, 'Comprador',     'Gestion de catalogo y proveedores'),
    (3, 'Vendedor',      'Atencion al cliente y cotizaciones'),
    (4, 'Cajero',        'Punto de venta y facturacion'),
    (5, 'Bodeguero',     'Control de inventario fisico');

INSERT INTO metodo_pago (nombre) VALUES
    ('Efectivo'),
    ('Tarjeta Debito'),
    ('Tarjeta Credito'),
    ('Transferencia Bancaria'),
    ('Nequi / Daviplata');

INSERT INTO categoria (nombre, descripcion) VALUES
    ('Computadores',  'Laptops, desktops y workstations'),
    ('Celulares',     'Smartphones y tablets'),
    ('Accesorios',    'Perifericos, cables y accesorios varios'),
    ('Componentes',   'Hardware interno: RAM, discos, tarjetas'),
    ('Audio',         'Audifonos, parlantes y micros'),
    ('Redes',         'Routers, switches y cables de red');

-- =============================================================================
-- DATOS DE PRUEBA: personas y usuarios
-- =============================================================================

-- Administrador
INSERT INTO persona (tipo, nombres, apellidos, documento, email, activo)
VALUES ('EMPLEADO', 'Admin', 'TechZone', '10001', 'admin@techzone.co', 1);

INSERT INTO empleado (id_persona, id_cargo, fecha_ingreso, contrasena_hash, salario)
SELECT id_persona, 1, CURDATE(), SHA2('admin123', 256), 5000000
FROM persona WHERE documento = '10001';

-- Comprador
INSERT INTO persona (tipo, nombres, apellidos, documento, email, activo)
VALUES ('EMPLEADO', 'Oscar', 'Soto', '10002', 'osoto@techzone.co', 1);

INSERT INTO empleado (id_persona, id_cargo, fecha_ingreso, contrasena_hash, salario)
SELECT id_persona, 2, CURDATE(), SHA2('comprador123', 256), 2500000
FROM persona WHERE documento = '10002';

-- Vendedor
INSERT INTO persona (tipo, nombres, apellidos, documento, email, activo)
VALUES ('EMPLEADO', 'Leidy', 'Bustamante', '10003', 'lbustamante@techzone.co', 1);

INSERT INTO empleado (id_persona, id_cargo, fecha_ingreso, contrasena_hash, salario)
SELECT id_persona, 3, CURDATE(), SHA2('vendedor123', 256), 2000000
FROM persona WHERE documento = '10003';

-- Cajero
INSERT INTO persona (tipo, nombres, apellidos, documento, email, activo)
VALUES ('EMPLEADO', 'Carlos', 'Martinez', '10004', 'cmartinez@techzone.co', 1);

INSERT INTO empleado (id_persona, id_cargo, fecha_ingreso, contrasena_hash, salario)
SELECT id_persona, 4, CURDATE(), SHA2('cajero123', 256), 1800000
FROM persona WHERE documento = '10004';

-- Bodeguero
INSERT INTO persona (tipo, nombres, apellidos, documento, email, activo)
VALUES ('EMPLEADO', 'Juan', 'Gaviria', '10005', 'jgaviria@techzone.co', 1);

INSERT INTO empleado (id_persona, id_cargo, fecha_ingreso, contrasena_hash, salario)
SELECT id_persona, 5, CURDATE(), SHA2('bodeguero123', 256), 1800000
FROM persona WHERE documento = '10005';

-- Segundo vendedor para pruebas
INSERT INTO persona (tipo, nombres, apellidos, documento, email, activo)
VALUES ('EMPLEADO', 'Alan', 'Caicedo', '10006', 'acaicedo@techzone.co', 1);

INSERT INTO empleado (id_persona, id_cargo, fecha_ingreso, contrasena_hash, salario)
SELECT id_persona, 3, CURDATE(), SHA2('alan123', 256), 2000000
FROM persona WHERE documento = '10006';

-- Clientes de prueba
INSERT INTO persona (tipo, nombres, apellidos, documento, email, telefono, direccion, activo)
VALUES ('CLIENTE', 'Laura', 'Gomez', '20001', 'laura@gmail.com', '3001234567', 'Calle 10 # 5-20, Cali', 1);

INSERT INTO cliente (id_persona, contrasena_hash)
SELECT id_persona, SHA2('laura123', 256) FROM persona WHERE documento = '20001';

INSERT INTO persona (tipo, nombres, apellidos, documento, email, telefono, direccion, activo)
VALUES ('CLIENTE', 'Pedro', 'Ramirez', '20002', 'pedro@gmail.com', '3109876543', 'Carrera 25 # 40-15, Cali', 1);

INSERT INTO cliente (id_persona, contrasena_hash)
SELECT id_persona, SHA2('pedro123', 256) FROM persona WHERE documento = '20002';

-- Proveedor
INSERT INTO persona (tipo, nombres, apellidos, documento, email, telefono, activo)
VALUES ('PROVEEDOR', 'Samsung', 'Colombia', '30001', 'ventas@samsung.co', '6012345678', 1);

INSERT INTO proveedor (id_persona, nombre_empresa, nit)
SELECT id_persona, 'Samsung Electronics Colombia', '900123456-1'
FROM persona WHERE documento = '30001';

INSERT INTO persona (tipo, nombres, apellidos, documento, email, telefono, activo)
VALUES ('PROVEEDOR', 'Apple', 'Distribuidora', '30002', 'ventas@apple-col.co', '6019876543', 1);

INSERT INTO proveedor (id_persona, nombre_empresa, nit)
SELECT id_persona, 'Apple Colombia Distribuidora', '800987654-2'
FROM persona WHERE documento = '30002';

-- =============================================================================
-- DATOS DE PRUEBA: productos
-- =============================================================================

INSERT INTO producto (id_categoria, nombre, descripcion, precio_compra, precio_venta, stock_actual, stock_minimo) VALUES
    (1, 'Laptop ASUS VivoBook 15',         'Intel Core i5, 8GB RAM, 512GB SSD, Windows 11',           2200000, 2800000, 10, 3),
    (1, 'Laptop HP 15-dy',                 'Intel Core i3, 8GB RAM, 256GB SSD, Windows 11',           1500000, 1900000,  8, 2),
    (1, 'MacBook Air M2',                  'Chip Apple M2, 8GB RAM, 256GB SSD, macOS',                5800000, 7200000,  4, 1),
    (2, 'Samsung Galaxy A55',              'Pantalla 6.6, 128GB, camara triple 50MP',                 1100000, 1400000, 15, 5),
    (2, 'iPhone 15',                       'Chip A16, 128GB, camara 48MP',                            4200000, 5100000,  6, 2),
    (2, 'Xiaomi Redmi Note 13',            'Pantalla AMOLED 6.67, 256GB, 108MP',                       600000,  800000, 20, 5),
    (3, 'Mouse Logitech MX Master 3',      'Inalambrico, ergonomico, multidispositivo',                 180000,  250000, 30, 8),
    (3, 'Teclado Logitech MX Keys',        'Inalambrico, retroiluminado, multidispositivo',             250000,  340000, 20, 5),
    (3, 'Monitor Samsung 24" FHD',         'IPS, 75Hz, HDMI, DisplayPort',                            450000,  580000, 12, 3),
    (4, 'RAM Kingston 16GB DDR4',          '3200MHz, sodimm, compatible laptops',                       85000,  120000, 25, 8),
    (4, 'SSD Samsung 970 Evo 500GB',       'NVMe M.2, 3500MB/s lectura',                              180000,  240000, 18, 5),
    (5, 'Audifonos Sony WH-1000XM5',       'Cancelacion de ruido, Bluetooth 5.2, 30h bateria',        700000,  920000,  8, 2),
    (5, 'Audifonos JBL Tune 510BT',        'Bluetooth, 40h bateria, bajo potente',                    120000,  160000, 15, 5),
    (6, 'Router TP-Link AX3000',           'WiFi 6, doble banda, 4 antenas',                          180000,  240000, 10, 3),
    (3, 'Webcam Logitech C920',            'Full HD 1080p, microfono integrado',                       130000,  175000, 14, 4);

-- =============================================================================
-- DATOS DE PRUEBA: ventas de ejemplo para que el dashboard muestre datos
-- =============================================================================

-- Venta 1: Laura compra laptop y mouse (cajero: Carlos Martinez id_persona=8)
INSERT INTO documento (id_tipo_documento, id_persona, id_empleado, id_metodo_pago, subtotal, total, estado)
SELECT 1, p_cli.id_persona, p_emp.id_persona, 1, 3050000, 3050000, 'COMPLETADA'
FROM persona p_cli, persona p_emp
WHERE p_cli.documento = '20001' AND p_emp.documento = '10004';

SET @id_doc1 = LAST_INSERT_ID();

INSERT INTO movimiento_inventario (id_documento, id_producto, id_empleado, cantidad, precio_unitario)
SELECT @id_doc1, p.id_producto,
       (SELECT id_persona FROM persona WHERE documento = '10004'),
       1, 2800000
FROM producto p WHERE p.nombre = 'Laptop ASUS VivoBook 15';

UPDATE producto SET stock_actual = stock_actual - 1 WHERE nombre = 'Laptop ASUS VivoBook 15';

INSERT INTO movimiento_inventario (id_documento, id_producto, id_empleado, cantidad, precio_unitario)
SELECT @id_doc1, p.id_producto,
       (SELECT id_persona FROM persona WHERE documento = '10004'),
       1, 250000
FROM producto p WHERE p.nombre = 'Mouse Logitech MX Master 3';

UPDATE producto SET stock_actual = stock_actual - 1 WHERE nombre = 'Mouse Logitech MX Master 3';

-- Venta 2: Pedro compra celular
INSERT INTO documento (id_tipo_documento, id_persona, id_empleado, id_metodo_pago, subtotal, total, estado)
SELECT 1, p_cli.id_persona, p_emp.id_persona, 2, 1400000, 1400000, 'COMPLETADA'
FROM persona p_cli, persona p_emp
WHERE p_cli.documento = '20002' AND p_emp.documento = '10004';

SET @id_doc2 = LAST_INSERT_ID();

INSERT INTO movimiento_inventario (id_documento, id_producto, id_empleado, cantidad, precio_unitario)
SELECT @id_doc2, p.id_producto,
       (SELECT id_persona FROM persona WHERE documento = '10004'),
       1, 1400000
FROM producto p WHERE p.nombre = 'Samsung Galaxy A55';

UPDATE producto SET stock_actual = stock_actual - 1 WHERE nombre = 'Samsung Galaxy A55';

-- Compra a proveedor (entrada de inventario)
INSERT INTO documento (id_tipo_documento, id_persona, id_empleado, numero_doc_externo, subtotal, total, estado)
SELECT 2, p_prov.id_persona, p_emp.id_persona, 'FAC-SAM-2024-001', 11000000, 11000000, 'COMPLETADA'
FROM persona p_prov, persona p_emp
WHERE p_prov.documento = '30001' AND p_emp.documento = '10002';

SET @id_doc3 = LAST_INSERT_ID();

INSERT INTO movimiento_inventario (id_documento, id_producto, id_empleado, cantidad, precio_unitario)
SELECT @id_doc3, p.id_producto,
       (SELECT id_persona FROM persona WHERE documento = '10002'),
       10, 1100000
FROM producto p WHERE p.nombre = 'Samsung Galaxy A55';

UPDATE producto SET stock_actual = stock_actual + 10 WHERE nombre = 'Samsung Galaxy A55';

-- =============================================================================
-- PROCEDIMIENTOS ALMACENADOS
-- =============================================================================

DELIMITER $$

-- SP 1: Registrar Venta
CREATE PROCEDURE sp_registrar_venta (
    IN  p_id_cliente      INT,
    IN  p_id_empleado     INT,
    IN  p_id_metodo_pago  INT,
    IN  p_productos_json  JSON,
    OUT p_id_documento    INT,
    OUT p_mensaje         VARCHAR(255)
)
BEGIN
    DECLARE v_total        DECIMAL(14,2) DEFAULT 0;
    DECLARE v_idx          INT DEFAULT 0;
    DECLARE v_count        INT;
    DECLARE v_id_prod      INT;
    DECLARE v_qty          INT;
    DECLARE v_precio       DECIMAL(12,2);
    DECLARE v_stock        INT;
    DECLARE v_stock_ok     TINYINT DEFAULT 1;
    DECLARE v_prod_fallo   INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_mensaje      = 'Error interno al registrar la venta.';
        SET p_id_documento = -1;
    END;

    START TRANSACTION;

    INSERT INTO documento (id_tipo_documento, id_persona, id_empleado, id_metodo_pago, total, estado)
    VALUES (1, p_id_cliente, IF(p_id_empleado > 0, p_id_empleado, NULL), p_id_metodo_pago, 0, 'PENDIENTE');

    SET p_id_documento = LAST_INSERT_ID();
    SET v_count        = JSON_LENGTH(p_productos_json);

    -- Primera pasada: verificar stock de todos los productos
    WHILE v_idx < v_count AND v_stock_ok = 1 DO
        SET v_id_prod = JSON_UNQUOTE(JSON_EXTRACT(p_productos_json, CONCAT('$[', v_idx, '].id')));
        SET v_qty     = JSON_UNQUOTE(JSON_EXTRACT(p_productos_json, CONCAT('$[', v_idx, '].qty')));

        SELECT stock_actual INTO v_stock
        FROM producto
        WHERE id_producto = v_id_prod
        FOR UPDATE;

        IF v_stock < v_qty THEN
            SET v_stock_ok   = 0;
            SET v_prod_fallo = v_id_prod;
        END IF;

        SET v_idx = v_idx + 1;
    END WHILE;

    IF v_stock_ok = 0 THEN
        ROLLBACK;
        SET p_id_documento = -1;
        SET p_mensaje = CONCAT('Stock insuficiente para el producto id=', v_prod_fallo);
    ELSE
        -- Segunda pasada: registrar movimientos
        SET v_idx = 0;
        WHILE v_idx < v_count DO
            SET v_id_prod = JSON_UNQUOTE(JSON_EXTRACT(p_productos_json, CONCAT('$[', v_idx, '].id')));
            SET v_qty     = JSON_UNQUOTE(JSON_EXTRACT(p_productos_json, CONCAT('$[', v_idx, '].qty')));
            SET v_precio  = JSON_UNQUOTE(JSON_EXTRACT(p_productos_json, CONCAT('$[', v_idx, '].precio')));

            INSERT INTO movimiento_inventario (id_documento, id_producto, id_empleado, cantidad, precio_unitario)
            VALUES (p_id_documento, v_id_prod, IF(p_id_empleado > 0, p_id_empleado, NULL), v_qty, v_precio);

            UPDATE producto SET stock_actual = stock_actual - v_qty WHERE id_producto = v_id_prod;

            SET v_total = v_total + (v_qty * v_precio);
            SET v_idx   = v_idx + 1;
        END WHILE;

        UPDATE documento
        SET total = v_total, subtotal = v_total, estado = 'COMPLETADA'
        WHERE id_documento = p_id_documento;

        COMMIT;
        SET p_mensaje = 'Venta registrada exitosamente.';
    END IF;
END$$


-- SP 2: Registrar Compra a Proveedor
CREATE PROCEDURE sp_registrar_compra (
    IN  p_id_proveedor    INT,
    IN  p_id_empleado     INT,
    IN  p_nro_factura_ext VARCHAR(60),
    IN  p_productos_json  JSON,
    OUT p_id_documento    INT,
    OUT p_mensaje         VARCHAR(255)
)
BEGIN
    DECLARE v_total   DECIMAL(14,2) DEFAULT 0;
    DECLARE v_idx     INT DEFAULT 0;
    DECLARE v_count   INT;
    DECLARE v_id_prod INT;
    DECLARE v_qty     INT;
    DECLARE v_precio  DECIMAL(12,2);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_mensaje      = 'Error al registrar la compra.';
        SET p_id_documento = -1;
    END;

    START TRANSACTION;

    INSERT INTO documento (id_tipo_documento, id_persona, id_empleado, numero_doc_externo, total, estado)
    VALUES (2, p_id_proveedor, p_id_empleado, p_nro_factura_ext, 0, 'PENDIENTE');

    SET p_id_documento = LAST_INSERT_ID();
    SET v_count        = JSON_LENGTH(p_productos_json);

    WHILE v_idx < v_count DO
        SET v_id_prod = JSON_UNQUOTE(JSON_EXTRACT(p_productos_json, CONCAT('$[', v_idx, '].id')));
        SET v_qty     = JSON_UNQUOTE(JSON_EXTRACT(p_productos_json, CONCAT('$[', v_idx, '].qty')));
        SET v_precio  = JSON_UNQUOTE(JSON_EXTRACT(p_productos_json, CONCAT('$[', v_idx, '].precio')));

        INSERT INTO movimiento_inventario (id_documento, id_producto, id_empleado, cantidad, precio_unitario)
        VALUES (p_id_documento, v_id_prod, p_id_empleado, v_qty, v_precio);

        UPDATE producto SET stock_actual = stock_actual + v_qty WHERE id_producto = v_id_prod;

        SET v_total = v_total + (v_qty * v_precio);
        SET v_idx   = v_idx + 1;
    END WHILE;

    UPDATE documento
    SET total = v_total, subtotal = v_total, estado = 'COMPLETADA'
    WHERE id_documento = p_id_documento;

    COMMIT;
    SET p_mensaje = 'Compra registrada exitosamente.';
END$$


-- SP 3: Ajuste de Inventario
CREATE PROCEDURE sp_ajuste_inventario (
    IN  p_id_tipo_doc  INT,
    IN  p_id_empleado  INT,
    IN  p_id_producto  INT,
    IN  p_cantidad     INT,
    IN  p_observacion  TEXT,
    OUT p_id_documento INT,
    OUT p_mensaje      VARCHAR(255)
)
BEGIN
    DECLARE v_efecto TINYINT;
    DECLARE v_stock  INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_mensaje      = 'Error en ajuste de inventario.';
        SET p_id_documento = -1;
    END;

    START TRANSACTION;

    SELECT efecto_en_inventario INTO v_efecto FROM tipo_documento WHERE id_tipo_documento = p_id_tipo_doc;
    SELECT stock_actual INTO v_stock FROM producto WHERE id_producto = p_id_producto FOR UPDATE;

    IF v_efecto = -1 AND v_stock < p_cantidad THEN
        ROLLBACK;
        SET p_id_documento = -1;
        SET p_mensaje      = 'Stock insuficiente para registrar la salida.';
    ELSE
        INSERT INTO documento (id_tipo_documento, id_persona, id_empleado, observaciones, total, estado)
        VALUES (p_id_tipo_doc, p_id_empleado, p_id_empleado, p_observacion, 0, 'COMPLETADA');

        SET p_id_documento = LAST_INSERT_ID();

        INSERT INTO movimiento_inventario (id_documento, id_producto, id_empleado, cantidad, precio_unitario)
        VALUES (p_id_documento, p_id_producto, p_id_empleado, p_cantidad, 0);

        UPDATE producto
        SET stock_actual = stock_actual + (v_efecto * p_cantidad)
        WHERE id_producto = p_id_producto;

        COMMIT;
        SET p_mensaje = 'Ajuste registrado correctamente.';
    END IF;
END$$


-- SP 4: Consultar Stock
CREATE PROCEDURE sp_consultar_stock (
    IN p_solo_alertas TINYINT(1)
)
BEGIN
    IF p_solo_alertas = 1 THEN
        SELECT
            p.id_producto,
            p.nombre,
            c.nombre AS categoria,
            p.stock_actual,
            p.stock_minimo,
            (p.stock_actual - p.stock_minimo) AS diferencia
        FROM producto p
        JOIN categoria c ON p.id_categoria = c.id_categoria
        WHERE p.activo = 1 AND p.stock_actual <= p.stock_minimo
        ORDER BY diferencia ASC;
    ELSE
        SELECT
            p.id_producto,
            p.nombre,
            c.nombre AS categoria,
            p.stock_actual,
            p.stock_minimo,
            (p.stock_actual - p.stock_minimo) AS diferencia
        FROM producto p
        JOIN categoria c ON p.id_categoria = c.id_categoria
        WHERE p.activo = 1
        ORDER BY p.nombre;
    END IF;
END$$


-- SP 5: Reporte de Movimientos por periodo
CREATE PROCEDURE sp_reporte_movimientos (
    IN p_id_producto INT,
    IN p_fecha_desde DATE,
    IN p_fecha_hasta DATE
)
BEGIN
    SELECT
        m.id_movimiento,
        m.fecha_movimiento,
        td.descripcion                          AS tipo_documento,
        td.efecto_en_inventario,
        d.numero_doc_externo,
        p.nombre                                AS producto,
        m.cantidad,
        m.precio_unitario,
        m.subtotal_linea,
        CONCAT(pe.nombres,  ' ', pe.apellidos)  AS persona_doc,
        IFNULL(CONCAT(emp.nombres, ' ', emp.apellidos), 'Sin empleado') AS empleado_reg
    FROM movimiento_inventario m
    JOIN documento             d   ON m.id_documento      = d.id_documento
    JOIN tipo_documento        td  ON d.id_tipo_documento  = td.id_tipo_documento
    JOIN producto              p   ON m.id_producto        = p.id_producto
    JOIN persona               pe  ON d.id_persona         = pe.id_persona
    LEFT JOIN empleado         e   ON m.id_empleado        = e.id_persona
    LEFT JOIN persona          emp ON e.id_persona         = emp.id_persona
    WHERE (p_id_producto IS NULL OR m.id_producto = p_id_producto)
      AND DATE(m.fecha_movimiento) BETWEEN p_fecha_desde AND p_fecha_hasta
    ORDER BY m.fecha_movimiento DESC;
END$$


-- SP 6: Historial de un Cliente
CREATE PROCEDURE sp_historial_cliente (
    IN p_id_cliente INT
)
BEGIN
    SELECT
        d.id_documento,
        d.fecha_documento,
        td.descripcion              AS tipo,
        d.total,
        d.estado,
        IFNULL(mp.nombre, 'N/A')   AS metodo_pago,
        COUNT(m.id_movimiento)      AS cant_productos
    FROM documento d
    JOIN  tipo_documento  td  ON d.id_tipo_documento = td.id_tipo_documento
    LEFT JOIN metodo_pago mp  ON d.id_metodo_pago    = mp.id_metodo_pago
    LEFT JOIN movimiento_inventario m ON d.id_documento = m.id_documento
    WHERE d.id_persona = p_id_cliente
    GROUP BY d.id_documento, d.fecha_documento, td.descripcion, d.total, d.estado, mp.nombre
    ORDER BY d.fecha_documento DESC;
END$$

-- SP 7: Dashboard KPIs (consulta unica para el panel de administrador)
CREATE PROCEDURE sp_dashboard_kpis ()
BEGIN
    SELECT
        (SELECT COUNT(*) FROM producto WHERE activo = 1)                               AS total_productos,
        (SELECT COUNT(*) FROM producto WHERE stock_actual <= stock_minimo AND activo = 1) AS productos_bajo_stock,
        (SELECT COUNT(*) FROM documento WHERE id_tipo_documento = 1 AND DATE(fecha_documento) = CURDATE()) AS ventas_hoy,
        (SELECT IFNULL(SUM(total), 0) FROM documento
         WHERE id_tipo_documento = 1
           AND MONTH(fecha_documento) = MONTH(NOW())
           AND YEAR(fecha_documento)  = YEAR(NOW())
           AND estado = 'COMPLETADA')                                                   AS ingresos_mes,
        (SELECT IFNULL(SUM(total), 0) FROM documento
         WHERE id_tipo_documento = 1
           AND MONTH(fecha_documento) = MONTH(NOW() - INTERVAL 1 MONTH)
           AND YEAR(fecha_documento)  = YEAR(NOW() - INTERVAL 1 MONTH)
           AND estado = 'COMPLETADA')                                                   AS ingresos_mes_anterior,
        (SELECT COUNT(DISTINCT id_persona) FROM documento
         WHERE id_tipo_documento = 1
           AND MONTH(fecha_documento) = MONTH(NOW())
           AND YEAR(fecha_documento)  = YEAR(NOW()))                                   AS clientes_activos_mes,
        (SELECT COUNT(*) FROM persona WHERE tipo = 'CLIENTE' AND activo = 1)           AS total_clientes,
        (SELECT COUNT(*) FROM empleado e JOIN persona p ON e.id_persona = p.id_persona WHERE p.activo = 1) AS total_empleados;
END$$

DELIMITER ;

-- =============================================================================
-- VERIFICACION FINAL
-- =============================================================================

SELECT
    p.documento,
    CONCAT(p.nombres, ' ', p.apellidos) AS nombre_completo,
    p.tipo,
    p.email,
    c.nombre AS cargo
FROM persona p
LEFT JOIN empleado e ON p.id_persona = e.id_persona
LEFT JOIN cargo c    ON e.id_cargo   = c.id_cargo
ORDER BY p.tipo, c.id_cargo;

/*
  CREDENCIALES DE ACCESO
  ======================================================
  ROL              EMAIL                    CONTRASENA
  ======================================================
  Administrador    admin@techzone.co        admin123
  Comprador        osoto@techzone.co        comprador123
  Vendedor         lbustamante@techzone.co  vendedor123
  Cajero           cmartinez@techzone.co    cajero123
  Bodeguero        jgaviria@techzone.co     bodeguero123
  Vendedor 2       acaicedo@techzone.co     alan123
  Cliente          laura@gmail.com          laura123
  Cliente 2        pedro@gmail.com          pedro123
*/