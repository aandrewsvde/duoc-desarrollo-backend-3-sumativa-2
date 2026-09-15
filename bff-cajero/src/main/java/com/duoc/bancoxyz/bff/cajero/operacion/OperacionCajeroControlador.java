package com.duoc.bancoxyz.bff.cajero.operacion;

import com.duoc.bancoxyz.bff.cajero.operacion.dto.ComprobanteRetiro;
import com.duoc.bancoxyz.bff.cajero.operacion.dto.SaldoCajero;
import com.duoc.bancoxyz.bff.cajero.operacion.dto.SolicitudRetiroCajero;
import com.duoc.bancoxyz.seguridad.OperacionNoPermitidaException;
import com.duoc.bancoxyz.seguridad.PrincipalCanal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las dos unicas operaciones del cajero.
 *
 * <p>Ninguna recibe la cuenta: sale del token de la sesion. En un canal que
 * mueve efectivo, aceptar el numero de cuenta como parametro seria entregarle al
 * cliente la decision de sobre que cuenta operar.</p>
 *
 * <p>Cada operacion exige su propio permiso, no solo el rol del canal. Un token
 * emitido sin {@code RETIRAR_EFECTIVO} puede consultar saldo pero no sacar
 * dinero, sin que haya que cambiar el codigo.</p>
 */
@RestController
@RequestMapping("/api/cajero")
@Tag(name = "Operaciones de cajero", description = "Consulta de saldo y retiro de efectivo")
public class OperacionCajeroControlador {

    private final ServicioOperacionCajero servicio;

    public OperacionCajeroControlador(ServicioOperacionCajero servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/saldo")
    @PreAuthorize("hasAuthority('CONSULTAR_SALDO')")
    @Operation(summary = "Saldo disponible de la cuenta de la sesion")
    public SaldoCajero saldo() {
        return servicio.consultarSaldo(sesion());
    }

    @PostMapping("/retiro")
    @PreAuthorize("hasAuthority('RETIRAR_EFECTIVO')")
    @Operation(summary = "Retira efectivo y devuelve el comprobante")
    public ComprobanteRetiro retirar(@Valid @RequestBody SolicitudRetiroCajero solicitud) {
        return servicio.retirar(sesion(), solicitud.monto());
    }

    private PrincipalCanal sesion() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PrincipalCanal canal && canal.cuentaAutorizada() != null) {
            return canal;
        }
        throw new OperacionNoPermitidaException("La sesion no corresponde al canal cajero");
    }
}
