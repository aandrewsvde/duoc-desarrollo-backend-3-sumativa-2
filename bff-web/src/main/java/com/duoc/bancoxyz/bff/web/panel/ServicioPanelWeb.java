package com.duoc.bancoxyz.bff.web.panel;

import com.duoc.bancoxyz.bff.web.config.PropiedadesCanalWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.PaginaMovimientosWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.PanelCuentaWeb;
import com.duoc.bancoxyz.core.client.ClienteCoreBancario;
import com.duoc.bancoxyz.core.contrato.CuentaCore;
import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import com.duoc.bancoxyz.core.contrato.PaginaCore;
import com.duoc.bancoxyz.core.contrato.ResumenAnualCore;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Composicion del panel web.
 *
 * <p>Las tres consultas al core son independientes entre si, de modo que se
 * lanzan en paralelo y la respuesta tarda lo que la mas lenta en vez de la suma
 * de las tres. Es la optimizacion central de este canal: no reduce bytes -la web
 * los tolera- sino viajes y latencia acumulada.</p>
 */
@Service
public class ServicioPanelWeb {

    private static final Logger log = LoggerFactory.getLogger(ServicioPanelWeb.class);
    private static final int ANIO_EJERCICIO = 2024;

    private final ClienteCoreBancario core;
    private final EnsambladorPanelWeb ensamblador;
    private final PropiedadesCanalWeb propiedades;
    private final ExecutorService ejecutor;

    public ServicioPanelWeb(ClienteCoreBancario core,
                            EnsambladorPanelWeb ensamblador,
                            PropiedadesCanalWeb propiedades,
                            ExecutorService ejecutorLlamadasCore) {
        this.core = core;
        this.ensamblador = ensamblador;
        this.propiedades = propiedades;
        this.ejecutor = ejecutorLlamadasCore;
    }

    public PanelCuentaWeb panel(int numeroCuenta) {
        long inicio = System.nanoTime();

        CompletableFuture<CuentaCore> cuenta =
                CompletableFuture.supplyAsync(() -> core.obtenerCuenta(numeroCuenta), ejecutor);
        CompletableFuture<ResumenAnualCore> resumen =
                CompletableFuture.supplyAsync(() -> core.resumenAnual(numeroCuenta, ANIO_EJERCICIO), ejecutor);
        CompletableFuture<PaginaCore<MovimientoCore>> movimientos =
                CompletableFuture.supplyAsync(() -> core.listarMovimientos(
                        numeroCuenta, 0, propiedades.getMovimientosEnPanel()), ejecutor);

        try {
            CompletableFuture.allOf(cuenta, resumen, movimientos).join();
        } catch (CompletionException e) {
            // Se desenvuelve la causa real: dejar viajar la CompletionException
            // haria que el manejador de errores viera una excepcion de
            // concurrencia en lugar del 404 o el 503 que de verdad ocurrio.
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw e;
        }

        PanelCuentaWeb panel = ensamblador.ensamblar(cuenta.join(), resumen.join(), movimientos.join());
        log.info("Panel de la cuenta {} compuesto en {} ms a partir de 3 recursos del core",
                numeroCuenta, (System.nanoTime() - inicio) / 1_000_000);
        return panel;
    }

    public PaginaMovimientosWeb movimientos(int numeroCuenta, int pagina, int tamano) {
        return ensamblador.pagina(core.listarMovimientos(numeroCuenta, pagina, tamano));
    }
}
