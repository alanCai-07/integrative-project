CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE SCHEMA IF NOT EXISTS techzone;
SET search_path TO techzone, public;

CREATE TABLE categoria (
    id_categoria INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE metodo_pago (
    id_metodo_pago INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tipo_documento (
    id_tipo_documento INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL UNIQUE,
    efecto_en_inventario SMALLINT NOT NULL CHECK (efecto_en_inventario IN (1, -1))
);

CREATE TABLE cargo (
    id_cargo INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE persona (
    id_persona INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CLIENTE', 'EMPLEADO', 'PROVEEDOR')),
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    documento VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(30),
    email VARCHAR(120) UNIQUE,
    direccion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_persona_tipo UNIQUE (id_persona, tipo),
    CONSTRAINT ck_email_empleado CHECK (tipo <> 'EMPLEADO' OR email IS NOT NULL)
);

CREATE TABLE cliente (
    id_persona INT PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL DEFAULT 'CLIENTE' CHECK (tipo = 'CLIENTE'),
    fecha_registro DATE NOT NULL DEFAULT CURRENT_DATE,
    contrasena_hash VARCHAR(60),
    CONSTRAINT fk_cliente_persona FOREIGN KEY (id_persona, tipo)
        REFERENCES persona(id_persona, tipo) ON DELETE RESTRICT
);

CREATE TABLE empleado (
    id_persona INT PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL DEFAULT 'EMPLEADO' CHECK (tipo = 'EMPLEADO'),
    id_cargo INT NOT NULL,
    fecha_ingreso DATE NOT NULL DEFAULT CURRENT_DATE,
    contrasena_hash VARCHAR(60) NOT NULL,
    CONSTRAINT fk_empleado_persona FOREIGN KEY (id_persona, tipo)
        REFERENCES persona(id_persona, tipo) ON DELETE RESTRICT,
    CONSTRAINT fk_empleado_cargo FOREIGN KEY (id_cargo)
        REFERENCES cargo(id_cargo) ON DELETE RESTRICT
);

CREATE TABLE proveedor (
    id_persona INT PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL DEFAULT 'PROVEEDOR' CHECK (tipo = 'PROVEEDOR'),
    nombre_empresa VARCHAR(150) NOT NULL,
    nit VARCHAR(30) UNIQUE,
    CONSTRAINT fk_proveedor_persona FOREIGN KEY (id_persona, tipo)
        REFERENCES persona(id_persona, tipo) ON DELETE RESTRICT
);

CREATE TABLE producto (
    id_producto INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_categoria INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio_compra NUMERIC(12,2) NOT NULL CHECK (precio_compra > 0),
    precio_venta NUMERIC(12,2) NOT NULL CHECK (precio_venta >= precio_compra),
    stock_actual INT NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    stock_minimo INT NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    imagen_url VARCHAR(255) NOT NULL DEFAULT 'assets/img/default.png',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria)
        REFERENCES categoria(id_categoria) ON DELETE RESTRICT
);

CREATE TABLE documento (
    id_documento INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tipo_documento INT NOT NULL,
    numero_comprobante INT NOT NULL,
    id_persona INT,
    id_empleado INT,
    id_metodo_pago INT,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_documento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    numero_doc_externo VARCHAR(60),
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    descuento NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0 AND descuento <= subtotal),
    iva NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (iva >= 0),
    total NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (total = subtotal - descuento + iva),
    estado VARCHAR(20) NOT NULL DEFAULT 'COMPLETADA' CHECK (estado IN ('PENDIENTE', 'COMPLETADA', 'ANULADA')),
    observaciones TEXT,
    CONSTRAINT uq_tipo_numero UNIQUE (id_tipo_documento, numero_comprobante),
    CONSTRAINT fk_documento_tipo FOREIGN KEY (id_tipo_documento)
        REFERENCES tipo_documento(id_tipo_documento) ON DELETE RESTRICT,
    CONSTRAINT fk_documento_persona FOREIGN KEY (id_persona)
        REFERENCES persona(id_persona) ON DELETE RESTRICT,
    CONSTRAINT fk_documento_empleado FOREIGN KEY (id_empleado)
        REFERENCES empleado(id_persona) ON DELETE SET NULL,
    CONSTRAINT fk_documento_metodo FOREIGN KEY (id_metodo_pago)
        REFERENCES metodo_pago(id_metodo_pago) ON DELETE RESTRICT
);

CREATE TABLE movimiento_inventario (
    id_movimiento INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_documento INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    costo_unitario NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo_unitario >= 0),
    precio_unitario NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (precio_unitario >= 0),
    subtotal_linea NUMERIC(14,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
    fecha_movimiento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mov_documento FOREIGN KEY (id_documento)
        REFERENCES documento(id_documento) ON DELETE RESTRICT,
    CONSTRAINT fk_mov_producto FOREIGN KEY (id_producto)
        REFERENCES producto(id_producto) ON DELETE RESTRICT
);

CREATE TABLE cotizacion (
    id_cotizacion INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_empleado INT NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vigencia_hasta DATE NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    iva NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (iva >= 0),
    total NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (total = subtotal + iva),
    estado VARCHAR(20) NOT NULL DEFAULT 'VIGENTE' CHECK (estado IN ('VIGENTE', 'CONVERTIDA', 'VENCIDA', 'ANULADA')),
    id_documento_venta INT,
    CONSTRAINT fk_cotizacion_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente(id_persona) ON DELETE RESTRICT,
    CONSTRAINT fk_cotizacion_empleado FOREIGN KEY (id_empleado)
        REFERENCES empleado(id_persona) ON DELETE RESTRICT,
    CONSTRAINT fk_cotizacion_doc_venta FOREIGN KEY (id_documento_venta)
        REFERENCES documento(id_documento) ON DELETE SET NULL
);

CREATE TABLE detalle_cotizacion (
    id_detalle_cotizacion INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cotizacion INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal_linea NUMERIC(14,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
    CONSTRAINT uq_cotizacion_producto UNIQUE (id_cotizacion, id_producto),
    CONSTRAINT fk_det_cotizacion FOREIGN KEY (id_cotizacion)
        REFERENCES cotizacion(id_cotizacion) ON DELETE CASCADE,
    CONSTRAINT fk_det_producto FOREIGN KEY (id_producto)
        REFERENCES producto(id_producto) ON DELETE RESTRICT
);

CREATE TABLE carrito (
    id_carrito INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cliente INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'CONVERTIDO', 'ABANDONADO')),
    id_documento INT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_carrito_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente(id_persona) ON DELETE RESTRICT,
    CONSTRAINT fk_carrito_documento FOREIGN KEY (id_documento)
        REFERENCES documento(id_documento) ON DELETE SET NULL
);

CREATE TABLE item_carrito (
    id_item INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_carrito INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    CONSTRAINT uq_carrito_producto UNIQUE (id_carrito, id_producto),
    CONSTRAINT fk_item_carrito FOREIGN KEY (id_carrito)
        REFERENCES carrito(id_carrito) ON DELETE CASCADE,
    CONSTRAINT fk_item_producto FOREIGN KEY (id_producto)
        REFERENCES producto(id_producto) ON DELETE RESTRICT
);

CREATE TABLE alerta_stock (
    id_alerta INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_producto INT NOT NULL,
    fecha_generacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stock_al_generar INT NOT NULL,
    stock_minimo_vigente INT NOT NULL,
    fecha_atencion TIMESTAMP,
    id_documento_reposicion INT,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'ATENDIDA')),
    CONSTRAINT fk_alerta_producto FOREIGN KEY (id_producto)
        REFERENCES producto(id_producto) ON DELETE RESTRICT,
    CONSTRAINT fk_alerta_doc_reposicion FOREIGN KEY (id_documento_reposicion)
        REFERENCES documento(id_documento) ON DELETE SET NULL
);

CREATE TABLE parametro (
    clave VARCHAR(50) PRIMARY KEY,
    valor NUMERIC(10,2) NOT NULL CHECK (valor >= 0),
    descripcion VARCHAR(255)
);

-- Índices B-Tree y GIN Trigram (RNF-06)
CREATE INDEX idx_empleado_cargo ON empleado(id_cargo);
CREATE INDEX idx_producto_categoria ON producto(id_categoria);
CREATE INDEX idx_producto_nombre_trgm ON producto USING gin (nombre gin_trgm_ops);
CREATE INDEX idx_producto_activos_nombre ON producto(nombre) WHERE activo = TRUE;
CREATE INDEX idx_documento_tipo ON documento(id_tipo_documento);
CREATE INDEX idx_documento_persona ON documento(id_persona);
CREATE INDEX idx_documento_empleado ON documento(id_empleado);
CREATE INDEX idx_documento_fecha ON documento(fecha_documento);
CREATE INDEX idx_mov_documento ON movimiento_inventario(id_documento);
CREATE INDEX idx_mov_producto ON movimiento_inventario(id_producto);
CREATE INDEX idx_cotizacion_cliente ON cotizacion(id_cliente);
CREATE INDEX idx_cotizacion_empleado ON cotizacion(id_empleado);
CREATE INDEX idx_det_cotizacion_cot ON detalle_cotizacion(id_cotizacion);
CREATE INDEX idx_det_cotizacion_prod ON detalle_cotizacion(id_producto);
CREATE INDEX idx_carrito_cliente ON carrito(id_cliente);
CREATE INDEX idx_item_carrito_car ON item_carrito(id_carrito);
CREATE INDEX idx_alerta_producto ON alerta_stock(id_producto);

-- Trigger 1: Actualización atómica de stock (RNF-07)
CREATE OR REPLACE FUNCTION fn_actualizar_stock()
RETURNS TRIGGER AS $$
DECLARE
    v_efecto SMALLINT;
BEGIN
    SELECT td.efecto_en_inventario INTO v_efecto
    FROM tipo_documento td
    JOIN documento d ON d.id_tipo_documento = td.id_tipo_documento
    WHERE d.id_documento = NEW.id_documento;

    UPDATE producto
    SET stock_actual = stock_actual + (NEW.cantidad * v_efecto)
    WHERE id_producto = NEW.id_producto;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_movimiento_stock
AFTER INSERT ON movimiento_inventario
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_stock();

-- Trigger 2: Trazabilidad de alertas de stock (RF-06 / RF-12)
CREATE OR REPLACE FUNCTION fn_gestionar_alerta_stock()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.stock_actual <= NEW.stock_minimo THEN
        IF NOT EXISTS (
            SELECT 1 FROM alerta_stock
            WHERE id_producto = NEW.id_producto AND estado = 'ACTIVA'
        ) THEN
            INSERT INTO alerta_stock (id_producto, stock_al_generar, stock_minimo_vigente, estado)
            VALUES (NEW.id_producto, NEW.stock_actual, NEW.stock_minimo, 'ACTIVA');
        END IF;
    ELSE
        UPDATE alerta_stock
        SET estado = 'ATENDIDA', fecha_atencion = CURRENT_TIMESTAMP
        WHERE id_producto = NEW.id_producto AND estado = 'ACTIVA';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_alerta_stock
AFTER UPDATE OF stock_actual ON producto
FOR EACH ROW
EXECUTE FUNCTION fn_gestionar_alerta_stock();

-- Vista de perfil de cliente sin exponer contrasena_hash (RNF-02 / RNF-09)
CREATE OR REPLACE VIEW vista_persona_cliente AS
SELECT p.id_persona, p.nombres AS nombre, p.apellidos AS apellido,
       p.documento, p.telefono, p.email, p.direccion, p.activo, c.fecha_registro
FROM persona p
JOIN cliente c ON p.id_persona = c.id_persona
WHERE p.tipo = 'CLIENTE';

-- Datos maestros iniciales
INSERT INTO tipo_documento (descripcion, efecto_en_inventario) VALUES
    ('Venta', -1),
    ('Compra', 1),
    ('Devolucion cliente', 1),
    ('Devolucion proveedor', -1),
    ('Ajuste entrada', 1),
    ('Ajuste salida', -1),
    ('Baja por dano/perdida', -1);

INSERT INTO cargo (nombre, descripcion) VALUES
    ('Administrador', 'Acceso total y configuracion general'),
    ('Comprador', 'Gestion de compras, proveedores y catalogo'),
    ('Vendedor', 'Generacion de cotizaciones y atencion comercial'),
    ('Cajero', 'Procesamiento de ventas y cobro en caja'),
    ('Bodeguero', 'Recepcion de mercancia y ajustes de inventario');

INSERT INTO metodo_pago (nombre) VALUES
    ('Efectivo'),
    ('Tarjeta Debito'),
    ('Tarjeta Credito'),
    ('Transferencia'),
    ('Nequi / Daviplata');

INSERT INTO parametro (clave, valor, descripcion) VALUES
    ('TIEMPO_REPOSICION_DIAS', 10.00, 'Dias estimados de entrega del proveedor (L)'),
    ('STOCK_SEGURIDAD_DIAS', 3.00, 'Dias de cobertura de seguridad para imprevistos (S)');

INSERT INTO categoria (nombre, descripcion) VALUES
    ('Computadores', 'Equipos portatiles y de escritorio'),
    ('Celulares', 'Smartphones y accesorios moviles'),
    ('Accesorios', 'Perifericos, teclados y mouses'),
    ('Componentes', 'Hardware interno: RAM, discos, tarjetas'),
    ('Audio', 'Audifonos, parlantes y microfonos'),
    ('Redes', 'Routers, switches y cables de red');

-- usuarios y contrasenas de prueba para la aplicacion Java, encriptadas con SHA-256:

-- Rol	        Correo	                Contraseña
--Administrador	admin@techzone.co	    admin123
--Comprador 	osoto@techzone.co	    comprador123
--Vendedor	    lbustamante@techzone.co	vendedor123
--Cajero	    cmartinez@techzone.co	techzone
--Bodeguero 	jgaviria@techzone.co	bodeguero123
--Vendedor 2	acaicedo@techzone.co	alan123