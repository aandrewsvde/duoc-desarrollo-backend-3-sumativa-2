package com.duoc.bancoxyz.bff.movil.resumen;

import com.duoc.bancoxyz.bff.movil.config.PropiedadesCanalMovil;
import com.duoc.bancoxyz.bff.movil.resumen.dto.MovimientoMovil;
import com.duoc.bancoxyz.bff.movil.resumen.dto.ResumenMovil;
import com.duoc.bancoxyz.core.client.ClienteCoreBancario;
import com.duoc.bancoxyz.core.contrato.CuentaCore;
import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import com.duoc.bancoxyz.core.contrato.PaginaCore;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import org.springframework.stereotype.Service;

/**
 * Consulta del canal movil.
 *
 * <p>Pide al core solo los movimientos que va a mostrar. Traer una pagina
 * completa para quedarse con cinco desperdiciaria ancho de banda entre el BFF y
 * el core, y ese trafico interno tambien cuesta.</p>
 *
 * <p>Las dos consultas que arma el resumen -la cuenta y sus movimientos- son
 * independientes, de modo que viajan en paralelo. Encadenarlas haria que el
 * usuario esperara la suma de ambas para ver una pantalla que necesita las dos
 * por igual.</p>
 */
@Service
public class ServicioResumenMovil {

    private final ClienteCoreBancario core;
    private final EnsambladorMovil ensamblador;
    private final PropiedadesCanalMovil propiedades;
    private final ExecutorService ejecutor;

    public ServicioResumenMovil(ClienteCoreBancario core,
                                EnsambladorMovil ensamblador,
                                PropiedadesCanalMovil propiedades,
                                ExecutorService ejecutorLlamadasCoreMovil) {
        this.core = core;
        this.ensamblador = ensamblador;
        this.propiedades = propiedades;
        this.ejecutor = ejecutorLlamadasCoreMovil;
    }

    public ResumenMovil resumen(int numeroCuenta) {
        CompletableFuture<CuentaCore> cuenta =
                CompletableFuture.supplyAsync(() -> core.obtenerCuenta(numeroCuenta), ejecutor);
        CompletableFuture<PaginaCore<MovimientoCore>> movimientos =
                CompletableFuture.supplyAsync(() -> core.listarMovimientos(
                        numeroCuenta, 0, propiedades.getMovimientosEnResumen()), ejecutor);

        try {
            CompletableFuture.allOf(cuenta, movimientos).join();
        } catch (CompletionException e) {
            // Se desenvuelve la causa real para que el manejador de errores vea
            // el 404 o el 503 que de verdad ocurrio.
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw e;
        }

        return ensamblador.ensamblar(cuenta.join(), movimientos.join().contenido());
    }

    public List<MovimientoMovil> movimientos(int numeroCuenta, int limite) {
        return ensamblador.movimientos(
                core.listarMovimientos(numeroCuenta, 0, limite).contenido());
    }
}
