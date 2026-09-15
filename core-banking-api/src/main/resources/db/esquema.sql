-- =====================================================================
-- Banco XYZ - Esquema del core bancario
--
-- El core es la unica fuente de verdad. Los tres BFF no tienen base de
-- datos propia: leen de aqui y adaptan. Esa es la separacion que sostiene
-- el patron, porque si cada canal guardara su copia, la personalizacion
-- por canal se convertiria en tres verdades distintas del mismo saldo.
-- =====================================================================

CREATE TABLE IF NOT EXISTS cuenta (
    numero_cuenta  INTEGER       PRIMARY KEY,
    titular        VARCHAR(120)  NOT NULL,
    tipo_cuenta    VARCHAR(20)   NOT NULL,
    saldo          NUMERIC(15,2) NOT NULL,
    moneda         VARCHAR(3)    NOT NULL DEFAULT 'CLP',
    edad_titular   INTEGER       NOT NULL,
    estado         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVA',
    actualizada_en TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT ck_cuenta_tipo   CHECK (tipo_cuenta IN ('AHORRO', 'PRESTAMO', 'HIPOTECA')),
    CONSTRAINT ck_cuenta_estado CHECK (estado IN ('ACTIVA', 'BLOQUEADA', 'CERRADA'))
);

CREATE TABLE IF NOT EXISTS movimiento (
    id            BIGSERIAL     PRIMARY KEY,
    cuenta_id     INTEGER       NOT NULL REFERENCES cuenta (numero_cuenta),
    fecha         DATE          NOT NULL,
    tipo          VARCHAR(20)   NOT NULL,
    -- El signo contable se guarda resuelto: +1 abona, -1 carga. Se calcula una
    -- vez, al ingresar el dato, y no en cada consulta de cada canal.
    signo         SMALLINT      NOT NULL,
    monto         NUMERIC(15,2) NOT NULL,
    descripcion   VARCHAR(200)  NOT NULL,
    canal_origen  VARCHAR(20)   NOT NULL DEFAULT 'LEGACY',
    registrado_en TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT ck_movimiento_signo CHECK (signo IN (1, -1)),
    CONSTRAINT ck_movimiento_monto CHECK (monto > 0)
);
-- Todo canal consulta movimientos por cuenta y en orden cronologico inverso;
-- el indice cubre esa forma de acceso, que es la unica que existe.
CREATE INDEX IF NOT EXISTS ix_movimiento_cuenta_fecha ON movimiento (cuenta_id, fecha DESC);

CREATE TABLE IF NOT EXISTS transaccion_diaria (
    id    BIGINT        PRIMARY KEY,
    fecha DATE          NOT NULL,
    monto NUMERIC(15,2) NOT NULL,
    tipo  VARCHAR(20)   NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_transaccion_fecha ON transaccion_diaria (fecha);

-- ---------------------------------------------------------------------
-- Identidades
--
-- Las credenciales viven centralizadas porque una persona es la misma en
-- los tres canales. Lo que NO vive aqui es el rol, la vigencia de sesion
-- ni los permisos: eso lo decide cada BFF, y es lo que hace que la
-- autenticacion sea especifica por canal aunque el titular sea uno solo.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario_canal (
    id            BIGSERIAL    PRIMARY KEY,
    usuario       VARCHAR(60)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    canal         VARCHAR(20)  NOT NULL,
    perfil        VARCHAR(30)  NOT NULL,
    cuenta_id     INTEGER      REFERENCES cuenta (numero_cuenta),
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_usuario_canal UNIQUE (usuario, canal),
    CONSTRAINT ck_usuario_canal CHECK (canal IN ('WEB', 'MOVIL'))
);

CREATE TABLE IF NOT EXISTS tarjeta (
    numero_tarjeta VARCHAR(19)  PRIMARY KEY,
    pin_hash       VARCHAR(100) NOT NULL,
    cuenta_id      INTEGER      NOT NULL REFERENCES cuenta (numero_cuenta),
    activa         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------------
-- Retiros
--
-- La columna referencia es UNIQUE y esa restriccion es la que hace
-- idempotente la operacion: si un cajero reenvia la misma orden tras un
-- corte de red, la segunda insercion choca contra el indice y el dinero
-- se descuenta una sola vez. Es la base de datos la que lo garantiza, no
-- una comprobacion previa en el codigo, que dos peticiones simultaneas
-- podrian sortear.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS retiro (
    id                   BIGSERIAL     PRIMARY KEY,
    cuenta_id            INTEGER       NOT NULL REFERENCES cuenta (numero_cuenta),
    monto                NUMERIC(15,2) NOT NULL,
    terminal             VARCHAR(40)   NOT NULL,
    referencia           VARCHAR(80)   NOT NULL,
    codigo_autorizacion  VARCHAR(20)   NOT NULL,
    saldo_anterior       NUMERIC(15,2) NOT NULL,
    saldo_resultante     NUMERIC(15,2) NOT NULL,
    instante             TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT uq_retiro_referencia UNIQUE (referencia)
);
CREATE INDEX IF NOT EXISTS ix_retiro_cuenta ON retiro (cuenta_id, instante DESC);

-- ---------------------------------------------------------------------
-- Reinicio de los datos de demostracion.
--
-- Se ejecuta antes de la semilla para que cada arranque parta del mismo
-- estado conocido y el evaluador obtenga siempre las mismas cifras. Las
-- operaciones hechas durante una sesion (retiros por cajero) se pierden al
-- reiniciar, lo que es deliberado: el entregable debe ser reproducible.
-- ---------------------------------------------------------------------
DELETE FROM retiro;
DELETE FROM movimiento;
DELETE FROM transaccion_diaria;
DELETE FROM tarjeta;
DELETE FROM usuario_canal;
DELETE FROM cuenta;
