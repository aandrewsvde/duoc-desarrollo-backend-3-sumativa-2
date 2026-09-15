package com.duoc.bancoxyz.bff.web.panel;

import com.duoc.bancoxyz.bff.web.panel.dto.CuentaDetalleWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.DesgloseTipoWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.MovimientoWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.PaginaMovimientosWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.PanelCuentaWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.ResumenAnualWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.SerieMensualWeb;
import com.duoc.bancoxyz.core.contrato.CuentaCore;
import com.duoc.bancoxyz.core.contrato.MovimientoCore;
import com.duoc.bancoxyz.core.contrato.PaginaCore;
import com.duoc.bancoxyz.core.contrato.ResumenAnualCore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Traduce el modelo canonico al contrato del canal web.
 *
 * <p>Es el punto donde el patron BFF hace su trabajo: aqui se decide que campos
 * viajan, con que nombre y con que derivados. El core no participa de esta
 * decision y por eso puede servir a los tres canales sin cambiar.</p>
 */
@Component
public class EnsambladorPanelWeb {

    private static final DateTimeFormatter FECHA_LEGIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PanelCuentaWeb ensamblar(CuentaCore cuenta,
                                    ResumenAnualCore resumen,
                                    PaginaCore<MovimientoCore> ultimos) {
        return new PanelCuentaWeb(
                detalleCuenta(cuenta),
                resumenAnual(resumen),
                desglosePorTipo(resumen),
                serieMensual(resumen),
                ultimos.contenido().stream().map(this::movimiento).toList(),
                enlaces(cuenta.numeroCuenta()));
    }

    public PaginaMovimientosWeb pagina(PaginaCore<MovimientoCore> origen) {
        return new PaginaMovimientosWeb(
                origen.contenido().stream().map(this::movimiento).toList(),
                origen.pagina(),
                origen.tamano(),
                origen.totalElementos(),
                origen.totalPaginas(),
                origen.pagina() > 0,
                origen.pagina() + 1 < origen.totalPaginas());
    }

    private CuentaDetalleWeb detalleCuenta(CuentaCore cuenta) {
        return new CuentaDetalleWeb(
                cuenta.numeroCuenta(),
                cuenta.titular(),
                cuenta.tipoCuenta(),
                EtiquetasCatalogo.tipoCuenta(cuenta.tipoCuenta()),
                cuenta.saldo(),
                FormateadorMonto.formatear(cuenta.saldo()),
                cuenta.moneda(),
                cuenta.edadTitular(),
                cuenta.estado(),
                OffsetDateTime.now());
    }

    private ResumenAnualWeb resumenAnual(ResumenAnualCore resumen) {
        return new ResumenAnualWeb(
                resumen.anio(),
                resumen.totalMovimientos(),
                resumen.totalAbonos(), FormateadorMonto.formatear(resumen.totalAbonos()),
                resumen.totalCargos(), FormateadorMonto.formatear(resumen.totalCargos()),
                resumen.netoPeriodo(), FormateadorMonto.formatear(resumen.netoPeriodo()),
                resumen.primerMovimiento(),
                resumen.ultimoMovimiento());
    }

    private List<DesgloseTipoWeb> desglosePorTipo(ResumenAnualCore resumen) {
        BigDecimal total = resumen.porTipo().stream()
                .map(t -> t.monto() == null ? BigDecimal.ZERO : t.monto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return resumen.porTipo().stream()
                .map(t -> new DesgloseTipoWeb(
                        t.tipo(),
                        EtiquetasCatalogo.tipoMovimiento(t.tipo()),
                        t.cantidad(),
                        t.monto(),
                        FormateadorMonto.formatear(t.monto()),
                        porcentaje(t.monto(), total)))
                .toList();
    }

    private List<SerieMensualWeb> serieMensual(ResumenAnualCore resumen) {
        return resumen.porMes().stream()
                .map(m -> new SerieMensualWeb(
                        m.mes(),
                        EtiquetasCatalogo.etiquetaMes(m.mes()),
                        m.abonos(),
                        m.cargos(),
                        m.abonos().subtract(m.cargos())))
                .toList();
    }

    private MovimientoWeb movimiento(MovimientoCore origen) {
        return new MovimientoWeb(
                origen.id(),
                origen.fecha(),
                origen.fecha().format(FECHA_LEGIBLE),
                origen.tipo(),
                EtiquetasCatalogo.tipoMovimiento(origen.tipo()),
                origen.descripcion(),
                origen.monto(),
                FormateadorMonto.formatear(origen.monto()),
                origen.signo(),
                origen.signo() > 0 ? "ABONO" : "CARGO");
    }

    /**
     * Enlaces a los recursos relacionados. Permiten que la interfaz navegue sin
     * construir rutas a mano, que es la forma habitual de que un cambio de URL
     * rompa un cliente sin que nadie lo note hasta produccion.
     */
    private Map<String, String> enlaces(int numeroCuenta) {
        Map<String, String> enlaces = new LinkedHashMap<>();
        enlaces.put("movimientos", "/api/web/cuentas/" + numeroCuenta + "/movimientos");
        enlaces.put("panel", "/api/web/cuentas/" + numeroCuenta + "/panel");
        return enlaces;
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (parte == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }
}
