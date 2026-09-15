package com.duoc.bancoxyz.bff.web.panel;

import com.duoc.bancoxyz.bff.web.config.PropiedadesCanalWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.PaginaMovimientosWeb;
import com.duoc.bancoxyz.bff.web.panel.dto.PanelCuentaWeb;
import com.duoc.bancoxyz.seguridad.SolicitudInvalidaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del canal web.
 *
 * <p>La autorizacion ocurre en dos niveles: la cadena de seguridad exige un rol
 * del canal, y {@link ControlAccesoWeb} comprueba ademas que la cuenta pedida
 * sea una que esa sesion puede ver. El primero responde "estas en el canal
 * correcto"; el segundo, "y ademas este recurso es tuyo".</p>
 */
@RestController
@RequestMapping("/api/web/cuentas")
@Tag(name = "Panel web", description = "Vistas completas optimizadas para navegadores")
public class PanelWebControlador {

    private final ServicioPanelWeb servicio;
    private final ControlAccesoWeb controlAcceso;
    private final PropiedadesCanalWeb propiedades;

    public PanelWebControlador(ServicioPanelWeb servicio,
                               ControlAccesoWeb controlAcceso,
                               PropiedadesCanalWeb propiedades) {
        this.servicio = servicio;
        this.controlAcceso = controlAcceso;
        this.propiedades = propiedades;
    }

    @GetMapping("/{numeroCuenta}/panel")
    @PreAuthorize("hasAuthority('VER_PANEL')")
    @Operation(summary = "Panel completo de la cuenta en una sola llamada",
               description = "Compone cuenta, resumen anual, desglose por tipo, serie mensual "
                           + "y ultimos movimientos, pidiendolos al core en paralelo.")
    public PanelCuentaWeb panel(@PathVariable int numeroCuenta) {
        controlAcceso.exigirAccesoA(numeroCuenta);
        return servicio.panel(numeroCuenta);
    }

    @GetMapping("/{numeroCuenta}/movimientos")
    @PreAuthorize("hasAuthority('VER_MOVIMIENTOS')")
    @Operation(summary = "Historial paginado con descripciones completas")
    public PaginaMovimientosWeb movimientos(@PathVariable int numeroCuenta,
                                            @RequestParam(defaultValue = "0") int pagina,
                                            @RequestParam(required = false) Integer tamano) {
        controlAcceso.exigirAccesoA(numeroCuenta);

        int tamanoEfectivo = tamano == null ? propiedades.getTamanoPaginaPorDefecto() : tamano;
        if (tamanoEfectivo < 1 || tamanoEfectivo > propiedades.getTamanoPaginaMaximo()) {
            throw new SolicitudInvalidaException(
                    "El tamano de pagina debe estar entre 1 y " + propiedades.getTamanoPaginaMaximo());
        }
        return servicio.movimientos(numeroCuenta, pagina, tamanoEfectivo);
    }
}
