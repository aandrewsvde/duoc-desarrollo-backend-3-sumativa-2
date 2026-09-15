# Banco XYZ — Patrón Backend for Frontend (BFF)

**Desarrollo Backend III (PBY2203) — Experiencia 2, Semana 5**
Actividad sumativa: *Implementando el patrón arquitectónico Backend for Frontend (BFF)*
Autor: Agustín Andrews

---

## 1. Objetivo

El Banco XYZ atiende a tres tipos de cliente muy distintos entre sí —navegador
web, aplicación móvil y cajero automático— contra un mismo backend. Este
proyecto implementa el patrón **Backend for Frontend**: un backend dedicado por
canal, cada uno con su propio contrato de salida, su propia autenticación y su
propio despliegue, sobre un core bancario común que no conoce a ninguno de ellos.

El resultado son **cuatro servicios independientes**, todos sobre HTTPS:

| Servicio | Puerto | Rol |
|---|---|---|
| `core-banking-api` | 8843 | Modelo canónico del banco. Única fuente de verdad. |
| `bff-web` | 8443 | Canal web: respuestas completas y compuestas. |
| `bff-movil` | 8444 | Canal móvil: respuestas mínimas. |
| `bff-cajero` | 8445 | Canal cajero: dos operaciones críticas. |

Los datos provienen de <https://github.com/KariVillagran/bank_legacy_data>
(directorio `data/semana_3`), los mismos que procesó el batch de la Experiencia 1.

---

## 2. Análisis de la estrategia de implementación

> Responde al punto 1 de las instrucciones: *"Debes determinar qué estrategia de
> implementación es la que más conviene al proyecto solicitado."*

La guía de la semana 5 plantea tres estrategias. Se evaluaron las tres contra
este caso concreto.

### 2.1 Las tres estrategias

**Estrategia 1 — Backends independientes por tipo de cliente**

| Ventajas | Desventajas |
|---|---|
| Personalización específica | Duplicación de esfuerzos |
| Autonomía de equipos por cliente | Mayor complejidad en la arquitectura |
| Mejor mantenibilidad | Mayores costos de infraestructura |
| Escalabilidad individual | Desafíos de seguridad y consistencia |
| Menos complejidad en el frontend | Más coordinación entre equipos |
| Simplicidad al pensar un solo cliente | Mayor curva de aprendizaje |
| | Más esfuerzo de versionado y despliegue |

**Estrategia 2 — Endpoints personalizados**

| Ventajas | Desventajas |
|---|---|
| Optimización de la experiencia de usuario | Incremento en la complejidad del backend |
| Menos carga sobre las capas de negocio | Posible duplicación de lógica de negocio |
| Flexibilidad en la evolución de frontends | Dificultad en la gestión de versiones |
| Control de acceso centralizado | Riesgo de sobre-ingeniería |
| Mantenimiento más simple | |
| Un equipo puede mantener todos los clientes | |

**Estrategia 3 — Delegación sobre microservicios**

| Ventajas | Desventajas |
|---|---|
| Mejor personalización de respuestas | Aumenta la complejidad en el BFF |
| Escalabilidad independiente | Posible duplicación de lógica |
| Desacoplamiento de lógicas de negocio | Costos adicionales de infraestructura |
| Reducción de carga en el frontend | Riesgos de latencia |
| Flexibilidad para nuevas funcionalidades | Desafíos de seguridad |

### 2.2 Análisis del caso Banco XYZ

| Factor | Situación del proyecto | Estrategia que favorece |
|---|---|---|
| Diferencia entre canales | Extrema. El cajero mueve dinero en sesiones de segundos; la web muestra tablas y gráficos durante horas. No es el mismo producto con otra pantalla. | 1 |
| Reglas de negocio por canal | Distintas. El cajero impone denominación de billetes y montos máximos que no existen en web ni en móvil. | 1 |
| Demanda, latencia y disponibilidad | Distintas. Un cajero caído es dinero que no sale de la máquina; la web tolera un despliegue con mantenimiento. | 1 |
| Escalamiento | Independiente. El móvil concentra los picos de consulta; el cajero, un tráfico bajo pero crítico. | 1 |
| Arquitectura existente | No hay microservicios. | descarta la 3 |
| Seguridad por canal | Mecanismos de autenticación **distintos**: usuario y contraseña en web y móvil, tarjeta y PIN en cajero. | 1 |

La estrategia 3 queda descartada porque no existe una arquitectura de
microservicios sobre la cual delegar. La estrategia 2 mantendría un solo
despliegue: sería más rápida de construir, pero obligaría a que el canal de
cajeros —el único que mueve efectivo— comparta proceso, ciclo de vida y
superficie de ataque con la web. Un despliegue de la web podría dejar los
cajeros fuera de servicio.

> **Decisión: estrategia 1, backends independientes por cliente.**
>
> Es además lo que pide literalmente el enunciado: *"Cada cliente deberá tener su
> propio Backend"*.

### 2.3 Lo que se comparte y lo que no

Backends independientes no significa triplicar todo. La guía advierte que la
duplicación de esfuerzos es la principal desventaja de esta estrategia, así que
la frontera se trazó de forma explícita:

| Se comparte | Por qué |
|---|---|
| `core-contrato` | Los tipos del contrato del core son **uno solo** y pertenecen al productor. Compartirlos impide que productor y consumidores diverjan sin romper la compilación. |
| `core-client` | El cliente HTTPS de ese contrato, con TLS, token de servicio y timeouts ya resueltos. Triplicarlo sería duplicación pura. |
| `seguridad-commons` | Las primitivas de firma y validación de JWT. Compartir el *algoritmo* no compromete la independencia: cada canal configura su propia clave, emisor, audiencia, vigencia y catálogo de roles. |

| No se comparte | Por qué |
|---|---|
| Los DTO de respuesta | Son el contrato de cada canal con su cliente. Es justamente lo que debe poder cambiar sin afectar a los otros. |
| Las reglas de transformación | Cada ensamblador decide qué viaja y con qué forma. |
| Roles, permisos y vigencias | Es la diferencia entre una sesión de 8 horas y una de 120 segundos. |
| El despliegue | Tres JAR, tres puertos, tres certificados, tres ciclos de vida. |

---

## 3. Arquitectura

```
   Navegador          App nativa           Cajero automático
       │                   │                       │
       │ HTTPS             │ HTTPS                 │ HTTPS
       │ JWT canal-web     │ JWT canal-movil       │ JWT canal-cajero
       │ 8 horas           │ 15 minutos            │ 120 segundos
       ▼                   ▼                       ▼
 ┌─────────────┐    ┌─────────────┐        ┌──────────────┐
 │  bff-web    │    │  bff-movil  │        │  bff-cajero  │
 │   :8443     │    │   :8444     │        │    :8445     │
 └──────┬──────┘    └──────┬──────┘        └───────┬──────┘
        │                  │                       │
        │  HTTPS + token de servicio (por canal)   │
        └──────────────────┼───────────────────────┘
                           ▼
                 ┌───────────────────┐
                 │ core-banking-api  │   modelo canónico
                 │      :8843        │   no conoce los canales
                 └─────────┬─────────┘
                           ▼
                    ┌─────────────┐
                    │ PostgreSQL  │  :5434
                    └─────────────┘
```

El core publica el modelo completo y **no sabe qué canales existen**. Esa
ignorancia es lo que permite agregar un cuarto canal sin modificarlo.

---

## 4. Estructura del código

```
Exp2_S5_Agustin_Andrews/
├── pom.xml                       reactor de 6 módulos
├── docker-compose.yml            PostgreSQL
├── certificados/                 CA propia + un certificado por servicio
│   └── generar-certificados.sh
├── shared/
│   ├── core-contrato/            tipos del contrato del core (sin framework)
│   ├── core-client/              cliente HTTPS del core: TLS, token, timeouts
│   └── seguridad-commons/        emisión y validación de JWT, errores uniformes
├── core-banking-api/             modelo canónico + PostgreSQL
│   ├── config/                   seguridad, permisos por cliente, OpenAPI
│   ├── cuenta/                   repositorios y controlador de cuentas
│   ├── identidad/                verificación de credenciales
│   ├── retiro/                   operación transaccional de retiro
│   └── resources/db/             esquema y semilla generada
├── bff-web/
│   ├── autenticacion/            login y emisión del token del canal
│   ├── panel/                    ensamblador, servicio y controlador
│   │   └── dto/                  contrato de salida del canal web
│   └── config/                   seguridad, CORS, concurrencia, OpenAPI
├── bff-movil/                    misma forma, contrato propio
├── bff-cajero/                   misma forma, contrato propio
├── scripts/                      arranque, evidencia y comparativa
└── evidencia/                    salidas de ejecución
```

Cada BFF repite la misma **forma** (`autenticacion/`, dominio, `config/`) pero
no comparte contenido. Un desarrollador que conozca uno entiende los tres, y aun
así puede cambiar uno sin tocar los otros. Agregar un canal nuevo —una API para
terceros, un totem de sucursal— es copiar esa forma y escribir su propio
ensamblador: no se modifica ninguno de los existentes ni el core.

---

## 5. Requisitos y ejecución

### Requisitos

- **JDK 21**
- **Docker** y **Docker Compose**
- Maven no es necesario si ya existen los JAR; para compilar, `mvn` 3.9+

### Puesta en marcha

```bash
# 1. Generar los certificados (una sola vez)
#    El ZIP de entrega ya los trae; en el repositorio de GitHub no, porque los
#    almacenes .p12 contienen claves privadas y no se versionan.
./certificados/generar-certificados.sh

# 2. Compilar
mvn clean package

# 3. Levantar PostgreSQL y los cuatro servicios
./scripts/levantar.sh
```

El script espera a que cada servicio responda antes de continuar, y si alguno no
arranca muestra las últimas líneas de su log. Para detener todo:

```bash
./scripts/detener.sh --todo
```

### Documentación navegable

Cada servicio publica su OpenAPI:

- <https://localhost:8443/swagger-ui.html> — canal web
- <https://localhost:8444/swagger-ui.html> — canal móvil
- <https://localhost:8445/swagger-ui.html> — canal cajero
- <https://localhost:8843/swagger-ui.html> — core

> Los certificados los firma una CA propia. En el navegador hay que aceptar la
> advertencia o importar `certificados/ca-bancoxyz.crt`; con `curl`, usar
> `--cacert certificados/ca-bancoxyz.crt`.

### Credenciales de demostración

Se siembran con BCrypt en el primer arranque. Están definidas en
`core-banking-api/src/main/resources/application.yml`.

| Canal | Credencial | Perfil | Cuenta |
|---|---|---|---|
| Web | `cliente101` / `Web2024$101` | CLIENTE | 101 |
| Web | `cliente102` / `Web2024$102` | CLIENTE | 102 |
| Web | `ejecutivo` / `Ejecutivo2024$` | EJECUTIVO | todas |
| Móvil | `movil101` / `Movil2024$101` | CLIENTE | 101 |
| Móvil | `movil102` / `Movil2024$102` | CLIENTE | 102 |
| Cajero | tarjeta `4051885600000101`, PIN `1101` | TARJETAHABIENTE | 101 |
| Cajero | tarjeta `4051885600000102`, PIN `1102` | TARJETAHABIENTE | 102 |

### Ejemplo completo

```bash
CA="--cacert certificados/ca-bancoxyz.crt"

# Canal web
TOKEN=$(curl -s $CA -X POST https://localhost:8443/api/web/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usuario":"cliente101","password":"Web2024$101"}' | jq -r .token)
curl -s $CA https://localhost:8443/api/web/cuentas/101/panel \
  -H "Authorization: Bearer $TOKEN" | jq

# Canal cajero
TOKEN=$(curl -s $CA -X POST https://localhost:8445/api/cajero/auth/pin \
  -H 'Content-Type: application/json' \
  -d '{"numeroTarjeta":"4051885600000101","pin":"1101","terminal":"ATM-07"}' | jq -r .token)
curl -s $CA https://localhost:8445/api/cajero/saldo -H "Authorization: Bearer $TOKEN"
curl -s $CA -X POST https://localhost:8445/api/cajero/retiro \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"monto":2000}'
```

---

## 6. Los tres canales

### 6.1 Endpoints

**`bff-web` — el canal que compone**

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/web/auth/login` | Sesión de 8 horas |
| GET | `/api/web/cuentas/{n}/panel` | Cuenta + resumen anual + desglose + serie mensual + últimos movimientos, en **una** llamada |
| GET | `/api/web/cuentas/{n}/movimientos` | Historial paginado con descripciones completas |

**`bff-movil` — el canal que recorta**

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/movil/auth/login` | Sesión de 15 minutos, ligada al dispositivo |
| GET | `/api/movil/resumen` | Saldo y últimos 5 movimientos |
| GET | `/api/movil/movimientos` | Hasta 20 movimientos abreviados |

**`bff-cajero` — el canal que opera**

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/cajero/auth/pin` | Tarjeta + PIN, sesión de 120 segundos |
| GET | `/api/cajero/saldo` | Solo el saldo disponible |
| POST | `/api/cajero/retiro` | Retiro con comprobante |

Ni el canal móvil ni el de cajero reciben el número de cuenta: **sale del token**.
Eso elimina de raíz que un cliente consulte u opere sobre una cuenta ajena
cambiando un parámetro.

### 6.2 La misma cuenta, tres respuestas

Canal web (recortado):

```json
{
  "cuenta": { "numeroCuenta": 101, "titular": "Diana Prince", "tipoCuenta": "AHORRO",
              "tipoCuentaDescripcion": "Cuenta de ahorro", "saldoActual": 8000.0,
              "saldoFormateado": "$8.000", "moneda": "CLP", "edadTitular": 35, ... },
  "resumenAnual": { "anio": 2024, "totalMovimientos": 29, "totalAbonos": 37000.0,
                    "totalAbonosFormateado": "$37.000", ... },
  "desglosePorTipo": [ { "tipo": "DEPOSITO", "etiqueta": "Deposito", "cantidad": 16,
                         "montoFormateado": "$37.000", "porcentaje": 61.67 }, ... ],
  "serieMensual": [ { "mes": "2024-01", "etiquetaMes": "Enero 2024", ... }, ... ],
  "ultimosMovimientos": [ ... ],
  "enlaces": { ... }
}
```

Canal móvil (respuesta completa, 253 bytes):

```json
{"cuenta":101,"titular":"D. Prince","saldo":8000,"moneda":"CLP",
 "movs":[{"f":"2024-12-22","t":"D","m":2500},{"f":"2024-11-23","t":"C","m":1500}, ...]}
```

Canal cajero (respuesta completa, 96 bytes):

```json
{"disponible":8000,"moneda":"CLP","terminal":"ATM-PROVIDENCIA-07","instante":"2026-09-14 22:10:55"}
```

---

## 7. Optimización por canal: medición

`scripts/comparativa-canales.py` mide los cuatro escenarios respondiendo **la
misma pregunta de negocio** ("estado de la cuenta 101"), con 30 repeticiones y
conexiones persistentes.

| Escenario | Viajes | Bytes | Bytes gzip | Campos | Mediana | vs. sin BFF |
|---|---|---|---|---|---|---|
| Sin BFF (core directo) | 3 | 2.363 | 805 | 134 | 18,0 ms | línea base |
| BFF Web | 1 | 4.390 | 1.078 | 201 | 27,2 ms | +85,8 % bytes / −2 viajes |
| BFF Móvil | 1 | **253** | 161 | 19 | 21,3 ms | **−89,3 % bytes** |
| BFF Cajero | 1 | **96** | 117 | 4 | 16,8 ms | **−95,9 % bytes** |

Cada canal optimiza una cosa distinta, porque cada canal tiene un cuello de
botella distinto:

- **Móvil optimiza bytes.** 253 contra 2.363: un 89 % menos. No es compresión,
  son campos que no viajan. De los siete atributos de una cuenta se envían tres;
  de los siete de un movimiento, tres. Las claves son de una letra porque en una
  lista se repiten una vez por elemento, y los importes van sin decimales porque
  el peso chileno no los usa.
- **Web optimiza viajes.** Transporta *más* bytes que la línea base, y es
  correcto: incluye los derivados que la interfaz tendría que calcular
  —importes formateados, porcentajes, serie mensual, etiquetas—. Su ganancia es
  resolver en **1 viaje** lo que sin BFF exige **3**, pidiendo los tres recursos
  al core **en paralelo** con hilos virtuales.
- **Cajero optimiza superficie.** 96 bytes y cuatro campos. Lo que no se envía
  tampoco puede quedar expuesto en una pantalla pública.

### 7.1 Un resultado que conviene declarar

En localhost, el consumo directo del core es el **más rápido** (18,0 ms contra
27,2 ms del panel web). Es esperable: el BFF agrega un salto que antes no
existía, y con latencia de red cercana a cero ese salto es puro costo.

El beneficio aparece cuando el cliente está lejos, que es donde viven los
clientes reales. Sumando a lo medido el costo de los viajes sobre la red del
cliente:

| Escenario | Viajes | Medido | + banda ancha (30 ms) | + móvil 4G (120 ms) |
|---|---|---|---|---|
| Sin BFF (core directo) | 3 | 18,0 ms | 108,0 ms | **378,0 ms** |
| BFF Web | 1 | 27,2 ms | 57,2 ms | **147,2 ms** |
| BFF Móvil | 1 | 21,3 ms | 51,3 ms | **141,3 ms** |
| BFF Cajero | 1 | 16,8 ms | 46,8 ms | 136,8 ms |

Sobre una red móvil, el panel web resuelve en 147 ms lo que sin BFF tomaría
378 ms. La proyección es un modelo, no una medición, y está declarada como tal
en el informe.

Otro hallazgo honesto: en el canal cajero la respuesta **comprimida pesa más**
que la original (117 contra 96 bytes). No es un error de medición: gzip agrega
una cabecera que sobre un cuerpo de menos de cien bytes no alcanza a
amortizarse. Por eso la compresión se activa a partir de un umbral.

---

## 8. Autenticación y autorización por canal

### 8.1 Un mecanismo distinto en cada canal

| | Web | Móvil | Cajero |
|---|---|---|---|
| Credencial | usuario + contraseña | usuario + contraseña + dispositivo | **tarjeta + PIN** |
| Endpoint del core | `/identidades/validar` | `/identidades/validar` | `/identidades/validar-tarjeta` |
| Vigencia | **8 horas** | **15 minutos** | **120 segundos** |
| Roles | `CLIENTE_WEB`, `EJECUTIVO_WEB` | `CLIENTE_MOVIL` | `CAJERO` |
| Permisos | `VER_PANEL`, `VER_MOVIMIENTOS`, `VER_RESUMEN_ANUAL`, `VER_CUALQUIER_CUENTA` | `VER_RESUMEN`, `VER_MOVIMIENTOS` | `CONSULTAR_SALDO`, `RETIRAR_EFECTIVO` |
| Contexto en el token | navegador | identificador del dispositivo | identificador del terminal |
| CORS | sí (único cliente que es navegador) | no | no |
| Cuenta | en la URL, validada contra el token | **del token** | **del token** |

Las vigencias no son arbitrarias: un cajero opera frente a una persona de pie en
la calle y si esa persona se aleja, la sesión ya expiró; un teléfono se pierde o
se presta con mucha más facilidad que un equipo de escritorio; una sesión web
acompaña la jornada.

### 8.2 Un token no cruza de canal

Verificado en `evidencia/funcional/recorrido-completo.txt`:

```
  TOKEN        -> bff-web     -> bff-movil   -> bff-cajero
  WEB          HTTP 200       HTTP 401       HTTP 401
  MOVIL        HTTP 401       HTTP 200       HTTP 401
  CAJERO       HTTP 401       HTTP 401       HTTP 200
```

Dos barreras independientes lo garantizan:

1. **Clave de firma distinta por canal.** La firma de un token ajeno ni siquiera
   *verifica*: es una barrera criptográfica, no una comprobación que se pueda
   olvidar.
2. **Audiencia declarada.** Aunque dos canales llegaran a compartir clave, la
   audiencia del token debe coincidir con la del servicio que lo recibe.

Ambas están cubiertas por pruebas unitarias en `ServicioTokensTest`.

### 8.3 Autorización a nivel de recurso

Que el token sea válido solo dice que la persona entró por la puerta correcta;
no dice que pueda mirar *esa* cuenta:

```
cliente101 → GET /api/web/cuentas/102/panel
  HTTP 403  "Su sesion no esta habilitada para consultar la cuenta 102"

ejecutivo  → GET /api/web/cuentas/102/panel
  HTTP 200  (tiene el permiso VER_CUALQUIER_CUENTA)
```

---

## 9. Seguridad

### 9.1 HTTPS y certificados

Los **cuatro** servicios sirven exclusivamente sobre TLS. `generar-certificados.sh`
crea una **CA propia** que firma un certificado por servicio:

```
core-banking-api  CN=core-banking-api   issuer=Banco XYZ Dev CA   Verify return code: 0 (ok)
bff-web           CN=bff-web            issuer=Banco XYZ Dev CA   Verify return code: 0 (ok)
bff-movil         CN=bff-movil          issuer=Banco XYZ Dev CA   Verify return code: 0 (ok)
bff-cajero        CN=bff-cajero         issuer=Banco XYZ Dev CA   Verify return code: 0 (ok)
```

Un certificado por servicio, no uno compartido: rotar el del canal móvil no debe
obligar a reiniciar los otros tres. Los clientes confían en **un** emisor, que es
como funciona una PKI real.

### 9.2 Tokens

- **JWT HMAC-SHA256**, clave distinta por canal, mínimo 256 bits (validado en el
  constructor de `PropiedadesToken`).
- **Token de usuario** entre el cliente y su BFF; **token de servicio** entre el
  BFF y el core. El core nunca ve el token del usuario final: comprometer el core
  no expone las sesiones de los clientes, ni al revés.

### 9.3 Defensa en profundidad: el core también autoriza por canal

El core reconoce permisos distintos a cada BFF y los lee de **su propia
configuración**, nunca del token recibido. Es deliberado: el secreto de servicio
es compartido, de modo que un BFF podría emitirse un token declarando cualquier
permiso. Quien decide el alcance es el core.

```
  OPERACION                                    bff-web   bff-movil   bff-cajero
  GET  /api/v1/cuentas/101                         200         200          200
  GET  /api/v1/cuentas/101/resumen-anual           200         403          403
  POST /api/v1/cuentas/101/retiros                 403         403          200
  POST /api/v1/identidades/validar-tarjeta         403         403          200
```

El BFF móvil no puede retirar dinero **ni aunque su propio código lo intentara**.

### 9.4 Otras medidas

| Medida | Dónde |
|---|---|
| Contraseñas y PIN con **BCrypt**, nunca en claro | `InicializadorCredenciales` |
| Mensaje de error genérico ante credencial inválida | `IdentidadServicio` |
| Enmascaramiento de identificadores en el log (`**** **** **** 0101`) | `IdentidadServicio` |
| Sin rutas, trazas ni nombres de tabla hacia el exterior | `ManejadorErroresApi` |
| Retiro **transaccional** con bloqueo de fila (`FOR UPDATE`) | `RetiroServicio` |
| Retiro **idempotente** por referencia única | restricción `uq_retiro_referencia` |
| Timeouts explícitos en todas las llamadas al core | `ConfiguracionClienteCore` |
| Límites de paginación en los tres canales | controladores |
| CORS restringido a orígenes conocidos, solo en web | `ConfiguracionSeguridadWeb` |
| Sesiones sin estado (`STATELESS`) | los cuatro servicios |

---

## 10. Datos

`scripts/generar-semilla.py` construye la semilla desde los CSV legacy aplicando
**las mismas reglas de saneamiento que el batch de la Experiencia 1**: fechas
normalizadas desde los cuatro formatos con rechazo estricto de fechas imposibles,
montos no positivos descartados, tipos canonizados sin acentos y catálogos
cerrados.

| Origen | Filas | Resultado |
|---|---|---|
| `intereses.csv` | 1.000 | **50 cuentas** (una fila válida por cuenta) |
| `cuentas_anuales.csv` | 1.000 | **686 movimientos** |
| `transacciones.csv` | 1.000 | **401 transacciones** |

Las cifras coinciden con las del proceso batch de la Experiencia 1, y el resumen
anual de la cuenta 101 que devuelve el canal web (29 movimientos, $37.000 en
abonos, $23.000 en cargos) es idéntico al `estado_cuenta_anual` que aquel batch
dejó en la base. Son la misma verdad servida por dos medios distintos.

> **Nota sobre el saldo.** El saldo vigente proviene de `intereses.csv` y **no**
> se recalcula desde los movimientos de `cuentas_anuales.csv`: son dos fuentes
> legacy que no reconcilian entre sí, y recalcular dejaría cuentas de ahorro en
> negativo. Cada fuente conserva su significado y el resumen anual expone el neto
> del período como cifra separada del saldo. Las operaciones que la API ejecuta
> —un retiro por cajero— sí actualizan saldo y movimiento de forma consistente.

---

## 11. Pruebas

```bash
mvn test
```

38 pruebas unitarias, sin base de datos ni contexto de Spring:

| Módulo | Pruebas | Qué cubren |
|---|---|---|
| `seguridad-commons` | 11 | Aislamiento entre canales: clave distinta, audiencia, emisor, vencimiento, token manipulado. Alcance del titular frente al del ejecutivo. |
| `bff-movil` | 7 | Las reducciones del canal: abreviación del nombre, importes enteros, códigos de una letra, campos que no viajan. |
| `bff-web` | 10 | Formateo de importes en pesos y traducción de códigos a etiquetas. |
| `bff-cajero` | 10 | Reglas de denominación, referencia única por orden, traducción de rechazos del core. |

---

## 12. Evidencia de ejecución

```
evidencia/
├── capturas/                       10 capturas de pantalla + su indice
│   ├── README.md                   que evidencia cada una
│   ├── 01-bff-web-swagger.png      ... 04-core-banking-api-swagger.png
│   └── 05-movil-login-ejecutado.png ... 10-web-rechaza-token-movil.png
├── funcional/
│   └── recorrido-completo.txt      recorrido de los 3 canales en 8 secciones
├── comparativa/
│   └── comparativa-canales.md      medición de bytes, campos y latencia
├── base_de_datos/
│   └── estado-del-core.txt         datos cargados, credenciales BCrypt, retiros
└── logs/
    ├── 00-arranque.txt
    ├── core-banking-api.log
    ├── bff-web.log
    ├── bff-movil.log
    └── bff-cajero.log
```

### Capturas de pantalla

Diez capturas tomadas sobre la plataforma en ejecución, todas con la URL
`https://` visible. Las cuatro primeras muestran el OpenAPI de cada servicio
—con los DTO propios de cada canal, que es lo que hace visible su independencia—;
las seis restantes son **ejecuciones reales** desde el navegador:

| Captura | Qué evidencia |
|---|---|
| `05-movil-login-ejecutado.png` | Login del canal móvil: HTTP 200 y token del canal `MOVIL` |
| `07-movil-resumen-ejecutado.png` | Respuesta mínima real: `f`, `t`, `m`, importes enteros |
| `08-cajero-retiro-ejecutado.png` | Retiro de $3.000 aprobado con comprobante; cabeceras `strict-transport-security` y `gzip` |
| `09-web-panel-ejecutado.png` | Panel compuesto en una sola respuesta; el saldo **ya refleja** el retiro de la captura 08, lo que evidencia el core compartido |
| `10-web-rechaza-token-movil.png` | Token válido y vigente del canal móvil presentado al canal web: **HTTP 401** |

El detalle de cada una está en `evidencia/capturas/README.md`.

`recorrido-completo.txt` cubre, en orden: verificación de los certificados TLS de
los cuatro servicios, autenticación en los tres canales con el contenido de cada
token, las respuestas de cada canal con sus tamaños, las reglas del cajero
(denominación, máximo, saldo insuficiente), la matriz de cruce de tokens, el
acceso a una cuenta ajena y la matriz de autorización del core por canal.

Para regenerarla:

```bash
./scripts/evidencia-funcional.sh   > evidencia/funcional/recorrido-completo.txt
./scripts/evidencia-base-datos.sh  > evidencia/base_de_datos/estado-del-core.txt
python3 scripts/comparativa-canales.py
```

---

## 13. Supuestos documentados

1. **Estrategia elegida**: backends independientes (§2), frente al ejemplo de
   endpoints personalizados que resuelve la guía para *su* caso —un monolito de
   blog con un equipo pequeño—, distinto de este.
2. **Certificados autofirmados** por una CA propia de desarrollo. En producción
   los emitiría una CA corporativa; la configuración de los servicios no cambia.
   Los almacenes `.p12` están excluidos del repositorio —contienen claves
   privadas— pero sí se incluyen en el ZIP de entrega para que el proyecto corra
   sin pasos previos. El certificado público de la CA (`ca-bancoxyz.crt`) sí se
   versiona: es lo que un cliente necesita para confiar, y no es secreto.
3. **Secretos en archivos de configuración** para que el entregable sea
   ejecutable por el evaluador. En producción vendrían de un gestor de secretos.
4. **Credenciales de demostración** documentadas a propósito: el profesor debe
   poder probar los tres canales. Se almacenan siempre con BCrypt.
5. **Límites del cajero**: mínimo 1.000, máximo 200.000 y múltiplos de 1.000,
   por denominación de billetes. Son reglas *del canal*, parametrizadas en
   `canal-cajero`.
6. **Latencias de la proyección** (30 ms banda ancha, 120 ms móvil 4G): valores
   representativos, declarados como modelo y no como medición.
7. **Reinicio de datos en cada arranque**: el esquema limpia y resiembra para que
   el evaluador obtenga siempre las mismas cifras. Los retiros de una sesión se
   pierden al reiniciar, lo que es deliberado.

---

## 14. Referencias

- Newman, S. (2015). *Pattern: Backends For Frontends.* <https://samnewman.io/patterns/architectural/bff/>
- Microsoft. *Patrón Backends for Frontends.* <https://learn.microsoft.com/es-es/azure/architecture/patterns/backends-for-frontends>
- AWS. *Backends for Frontends pattern.* <https://aws.amazon.com/es/blogs/mobile/backends-for-frontends-pattern/>
- Caules, A. (2021). *¿Qué es el patrón BFF?* <https://www.arquitecturajava.com/que-es-el-patron-bff/>
- Villagrán, K. *bank_legacy_data.* <https://github.com/KariVillagran/bank_legacy_data>
