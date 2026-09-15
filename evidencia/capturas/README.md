# Capturas de pantalla

Tomadas sobre la plataforma en ejecución, con el navegador confiando en la CA
del proyecto (`certificados/ca-bancoxyz.crt`). En todas se ve la URL `https://`
y el puerto del canal correspondiente.

| # | Archivo | Qué evidencia |
|---|---|---|
| 01 | `01-bff-web-swagger.png` | Canal web publicado en `https://localhost:8443`, con sus endpoints y sus DTO propios (`PanelCuentaWeb`, `ResumenAnualWeb`, `SerieMensualWeb`…). |
| 02 | `02-bff-movil-swagger.png` | Canal móvil en `https://localhost:8444`, con **otros** DTO (`ResumenMovil`, `MovimientoMovil`) y sin el número de cuenta en las rutas. |
| 03 | `03-bff-cajero-swagger.png` | Canal cajero en `https://localhost:8445`: sólo autenticación con PIN, saldo y retiro. La superficie más pequeña de los tres. |
| 04 | `04-core-banking-api-swagger.png` | Core en `https://localhost:8843` con el modelo canónico completo, que los tres canales consumen y ninguno expone tal cual. |
| 05 | `05-movil-login-ejecutado.png` | Login del canal móvil ejecutado en el navegador: HTTP 200 y token del canal `MOVIL`. |
| 06 | `06-movil-authorize-token.png` | Esquema de seguridad del canal: `bearer` / `JWT`, declarado en el OpenAPI de cada BFF por separado. |
| 07 | `07-movil-resumen-ejecutado.png` | Respuesta real del canal móvil: campos abreviados (`f`, `t`, `m`), importes enteros y tipos en una letra. |
| 08 | `08-cajero-retiro-ejecutado.png` | Retiro de $3.000 aprobado, con comprobante y código de autorización. En las cabeceras se ven `strict-transport-security` y `content-encoding: gzip`. |
| 09 | `09-web-panel-ejecutado.png` | Panel compuesto del canal web: cuenta, resumen anual, desglose, serie mensual y movimientos en **una** respuesta. El saldo ya refleja el retiro de la captura 08, lo que evidencia el core compartido entre canales. |
| 10 | `10-web-rechaza-token-movil.png` | Un token **válido y vigente** del canal móvil presentado al canal web: **HTTP 401**. Es la evidencia visual de que un token no cruza de canal. |

Las capturas 05 a 10 son ejecuciones reales contra los servicios, no ejemplos
de la documentación: en cada una se ve el `curl` equivalente, la URL completa y
el cuerpo devuelto por el servidor.

## Cómo reproducirlas

```bash
./scripts/levantar.sh
```

Luego abrir los Swagger UI de cada canal. El navegador advertirá por el
certificado: hay que aceptar la excepción o importar
`certificados/ca-bancoxyz.crt` en el almacén de confianza.
