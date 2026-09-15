# Comparativa de canales: optimizacion de respuesta y consumo de recursos

Pregunta de negocio comun a los cuatro escenarios: **estado de la cuenta 101**.

Metodo: 30 mediciones por escenario (mas 5 de calentamiento descartadas). Se reporta la mediana y el percentil 95 de la latencia acumulada de todos los viajes del escenario. Los bytes son los del cuerpo de la respuesta, medidos sin compresion y con `Accept-Encoding: gzip`.

| Escenario | Viajes | Bytes | Bytes gzip | Campos | Mediana | p95 | vs. sin BFF |
|---|---|---|---|---|---|---|---|
| Sin BFF (core directo) | 3 | 2.363 | 805 | 134 | 18.0 ms | 21.9 ms | linea base |
| BFF Web | 1 | 4.390 | 1.078 | 201 | 27.2 ms | 36.1 ms | +85.8% bytes / -2 viajes |
| BFF Movil | 1 | 253 | 161 | 19 | 21.3 ms | 25.2 ms | -89.3% bytes |
| BFF Cajero | 1 | 96 | 117 | 4 | 16.8 ms | 18.5 ms | -95.9% bytes |

## Proyeccion con latencia de red del cliente

Las cifras anteriores se midieron en localhost, donde el tiempo de ida y vuelta es
practicamente cero. En esa condicion un BFF **solo puede perder**: agrega un salto que
antes no existia, y por eso el consumo directo del core aparece como el mas rapido.
Es un resultado honesto y conviene decirlo, porque es tambien la razon por la que el
patron no se justifica cuando cliente y backend comparten centro de datos.

El beneficio aparece cuando el cliente esta lejos. La siguiente proyeccion suma a lo
medido el costo de los viajes que un cliente real haria sobre la red indicada
(latencia medida x numero de viajes del escenario):

| Escenario | Viajes | Medido | + banda ancha (30 ms) | + movil 4G (120 ms) |
|---|---|---|---|---|
| Sin BFF (core directo) | 3 | 18.0 ms | 108.0 ms | 378.0 ms |
| BFF Web | 1 | 27.2 ms | 57.2 ms | 147.2 ms |
| BFF Movil | 1 | 21.3 ms | 51.3 ms | 141.3 ms |
| BFF Cajero | 1 | 16.8 ms | 46.8 ms | 136.8 ms |

Sobre una red movil, el panel web resuelve en **147 ms** lo que sin BFF tomaria
**378 ms**, y el resumen movil baja a **141 ms** transportando ademas
un 89% menos de bytes. La proyeccion es un modelo, no una medicion: se declara como tal.

## Que hace cada escenario

- **Sin BFF (core directo)**: El cliente encadena 3 llamadas al modelo canonico y descarta lo que no usa.
- **BFF Web**: Una llamada: panel compuesto, con derivados y formato listo para la interfaz.
- **BFF Movil**: Una llamada: saldo y 5 movimientos con campos abreviados.
- **BFF Cajero**: Una llamada: solo el saldo disponible.

## Lectura de los resultados

- El canal **movil** transporta un **89%** menos de bytes que el consumo directo del core, y un **94%** menos que el canal web. No es compresion: son campos que no viajan.
- El canal **web** mueve mas bytes que la linea base (4390 frente a 2363) y eso es correcto: incluye los derivados que la interfaz necesitaria calcular por su cuenta -importes formateados, porcentajes, serie mensual, etiquetas-. Su optimizacion no es el volumen sino los viajes: resuelve en **1** lo que sin BFF exige **3**, con una mediana de 27.2 ms frente a 18.0 ms.
- El canal **cajero** entrega la respuesta mas pequena de todas (96 bytes): un cajero no necesita nada mas que el saldo, y lo que no se envia tampoco puede filtrarse en una pantalla publica.
- La compresion ayuda donde hay volumen, pero no sustituye al recorte: el panel web comprimido sigue por encima del resumen movil sin comprimir.
- En el canal cajero la respuesta comprimida pesa **mas** que la original. No es un error de medicion: gzip agrega una cabecera de unas dos decenas de bytes que, sobre un cuerpo de menos de cien, no alcanza a amortizarse. Por eso la compresion se activa a partir de un umbral y no siempre.
