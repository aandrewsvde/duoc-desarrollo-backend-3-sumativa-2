package com.duoc.bancoxyz.core.cuenta;

import com.duoc.bancoxyz.core.config.PropiedadesCoreApi;
import com.duoc.bancoxyz.core.contrato.CuentaCore;
import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import com.duoc.bancoxyz.core.contrato.PaginaCore;
import com.duoc.bancoxyz.core.contrato.ResultadoRetiroCore;
import com.duoc.bancoxyz.core.contrato.ResumenAnualCore;
import com.duoc.bancoxyz.core.contrato.SolicitudRetiroCore;
import com.duoc.bancoxyz.core.retiro.RetiroServicio;
import com.duoc.bancoxyz.seguridad.RecursoNoEncontradoException;
import com.duoc.bancoxyz.seguridad.SolicitudInvalidaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modelo canonico del banco.
 *
 * <p>Entrega la informacion completa, sin recortar ni reordenar para nadie: no
 * sabe que canales existen. Un cliente que consuma este API directamente tendria
 * que hacer tres llamadas y descartar los campos que no usa, que es exactamente
 * el problema que cada BFF resuelve en su capa.</p>
 */
@RestController
@RequestMapping("/api/v1/cuentas")
@Tag(name = "Cuentas", description = "Modelo canonico de cuentas, movimientos y operaciones")
public class CuentaControlador {

    private static final int TAMANO_MAXIMO_PAGINA = 200;

    private final CuentaRepositorio cuentas;
    private final MovimientoRepositorio movimientos;
    private final ResumenRepositorio resumenes;
    private final RetiroServicio retiros;
    private final PropiedadesCoreApi propiedades;

    public CuentaControlador(CuentaRepositorio cuentas,
                             MovimientoRepositorio movimientos,
                             ResumenRepositorio resumenes,
                             RetiroServicio retiros,
                             PropiedadesCoreApi propiedades) {
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.resumenes = resumenes;
        this.retiros = retiros;
        this.propiedades = propiedades;
    }

    @GetMapping("/{numeroCuenta}")
    @Operation(summary = "Datos completos de una cuenta")
    public CuentaCore obtener(@PathVariable int numeroCuenta) {
        return cuentas.buscar(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La cuenta " + numeroCuenta + " no existe"));
    }

    @GetMapping("/{numeroCuenta}/movimientos")
    @Operation(summary = "Historial paginado de movimientos, del mas reciente al mas antiguo")
    public PaginaCore<MovimientoCore> movimientos(@PathVariable int numeroCuenta,
                                                  @RequestParam(defaultValue = "0") int pagina,
                                                  @RequestParam(defaultValue = "20") int tamano) {
        exigirQueExista(numeroCuenta);
        if (pagina < 0) {
            throw new SolicitudInvalidaException("El numero de pagina no puede ser negativo");
        }
        // Un tamano sin tope permitiria que un solo cliente pidiera el historial
        // entero y convirtiera una consulta en una descarga masiva.
        if (tamano < 1 || tamano > TAMANO_MAXIMO_PAGINA) {
            throw new SolicitudInvalidaException(
                    "El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO_PAGINA);
        }
        return movimientos.listar(numeroCuenta, pagina, tamano);
    }

    @GetMapping("/{numeroCuenta}/resumen-anual")
    @Operation(summary = "Agregados del ejercicio: totales, desglose por tipo y serie mensual")
    public ResumenAnualCore resumenAnual(@PathVariable int numeroCuenta,
                                         @RequestParam(required = false) Integer anio) {
        exigirQueExista(numeroCuenta);
        return resumenes.resumir(numeroCuenta,
                anio == null ? propiedades.getAnioEjercicio() : anio);
    }

    @PostMapping("/{numeroCuenta}/retiros")
    @Operation(summary = "Registra un retiro de efectivo y actualiza el saldo")
    public ResultadoRetiroCore retirar(@PathVariable int numeroCuenta,
                                       @RequestBody SolicitudRetiroCore solicitud) {
        exigirQueExista(numeroCuenta);
        if (solicitud.monto() == null || solicitud.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException("El monto del retiro debe ser mayor que cero");
        }
        if (solicitud.referencia() == null || solicitud.referencia().isBlank()) {
            throw new SolicitudInvalidaException(
                    "La referencia es obligatoria: es lo que hace idempotente la operacion");
        }
        return retiros.registrar(numeroCuenta, solicitud);
    }

    private void exigirQueExista(int numeroCuenta) {
        if (!cuentas.existe(numeroCuenta)) {
            throw new RecursoNoEncontradoException("La cuenta " + numeroCuenta + " no existe");
        }
    }
}
