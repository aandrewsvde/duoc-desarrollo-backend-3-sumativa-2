package com.duoc.bancoxyz.core.client;

import com.duoc.bancoxyz.core.contrato.CuentaCore;
import com.duoc.bancoxyz.core.contrato.IdentidadCore;
import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import com.duoc.bancoxyz.core.contrato.PaginaCore;
import com.duoc.bancoxyz.core.contrato.ResultadoRetiroCore;
import com.duoc.bancoxyz.core.contrato.ResumenAnualCore;
import com.duoc.bancoxyz.core.contrato.SolicitudRetiroCore;

/**
 * Contrato del core visto por los canales.
 *
 * <p>Es una interfaz y no una clase concreta para que cada BFF pueda sustituirla
 * en sus pruebas sin levantar el core, y para que un cambio de transporte
 * -pasar de HTTP a mensajeria, por ejemplo- no obligue a tocar los canales.</p>
 */
public interface ClienteCoreBancario {

    CuentaCore obtenerCuenta(int numeroCuenta);

    PaginaCore<MovimientoCore> listarMovimientos(int numeroCuenta, int pagina, int tamano);

    ResumenAnualCore resumenAnual(int numeroCuenta, int anio);

    ResultadoRetiroCore registrarRetiro(int numeroCuenta, SolicitudRetiroCore solicitud);

    IdentidadCore validarCredencial(String usuario, String password, String canal);

    IdentidadCore validarTarjeta(String numeroTarjeta, String pin);
}
