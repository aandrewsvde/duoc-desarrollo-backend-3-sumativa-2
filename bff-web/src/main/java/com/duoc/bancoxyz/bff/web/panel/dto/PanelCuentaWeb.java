package com.duoc.bancoxyz.bff.web.panel.dto;

import java.util.List;
import java.util.Map;

/**
 * Respuesta compuesta del canal web: todo lo que la pantalla principal necesita
 * en una sola llamada.
 *
 * <p>Este objeto es la razon de ser del BFF web. El core publica la cuenta, el
 * historial y los agregados en tres recursos distintos, como corresponde a un
 * modelo canonico. Un navegador que los consumiera directamente encadenaria tres
 * viajes y pagaria tres latencias antes de pintar nada. Aqui esos tres recursos
 * se piden en paralelo y se entregan juntos, ya formateados y con los derivados
 * que la interfaz necesita.</p>
 */
public record PanelCuentaWeb(
        CuentaDetalleWeb cuenta,
        ResumenAnualWeb resumenAnual,
        List<DesgloseTipoWeb> desglosePorTipo,
        List<SerieMensualWeb> serieMensual,
        List<MovimientoWeb> ultimosMovimientos,
        Map<String, String> enlaces) {
}
