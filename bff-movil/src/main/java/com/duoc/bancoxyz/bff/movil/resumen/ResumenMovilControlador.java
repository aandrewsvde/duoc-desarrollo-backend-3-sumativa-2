package com.duoc.bancoxyz.bff.movil.resumen;

import com.duoc.bancoxyz.bff.movil.config.PropiedadesCanalMovil;
import com.duoc.bancoxyz.bff.movil.resumen.dto.MovimientoMovil;
import com.duoc.bancoxyz.bff.movil.resumen.dto.ResumenMovil;
import com.duoc.bancoxyz.seguridad.SolicitudInvalidaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del canal movil.
 *
 * <p>Ninguno recibe el numero de cuenta: sale del token. Eso elimina de raiz la
 * posibilidad de que la app consulte una cuenta ajena cambiando un parametro, y
 * de paso acorta las URL.</p>
 */
@RestController
@RequestMapping("/api/movil")
@Tag(name = "Resumen movil", description = "Respuestas minimas para la aplicacion nativa")
public class ResumenMovilControlador {

    private final ServicioResumenMovil servicio;
    private final ControlAccesoMovil controlAcceso;
    private final PropiedadesCanalMovil propiedades;

    public ResumenMovilControlador(ServicioResumenMovil servicio,
                                   ControlAccesoMovil controlAcceso,
                                   PropiedadesCanalMovil propiedades) {
        this.servicio = servicio;
        this.controlAcceso = controlAcceso;
        this.propiedades = propiedades;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('VER_RESUMEN')")
    @Operation(summary = "Pantalla de inicio: saldo y ultimos movimientos de la cuenta de la sesion")
    public ResumenMovil resumen() {
        return servicio.resumen(controlAcceso.cuentaDeLaSesion());
    }

    @GetMapping("/movimientos")
    @PreAuthorize("hasAuthority('VER_MOVIMIENTOS')")
    @Operation(summary = "Ultimos movimientos, acotados por el tope del canal")
    public List<MovimientoMovil> movimientos(@RequestParam(required = false) Integer limite) {
        int efectivo = limite == null ? propiedades.getMovimientosEnResumen() : limite;
        if (efectivo < 1 || efectivo > propiedades.getMovimientosMaximo()) {
            throw new SolicitudInvalidaException(
                    "El limite debe estar entre 1 y " + propiedades.getMovimientosMaximo()
                            + " en el canal movil");
        }
        return servicio.movimientos(controlAcceso.cuentaDeLaSesion(), efectivo);
    }
}
