package com.duoc.bancoxyz.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce las excepciones de negocio a respuestas HTTP coherentes en los cuatro
 * servicios. Se activa importandola desde la configuracion de cada aplicacion.
 *
 * <p>Las excepciones no previstas se registran con su traza en el log y se
 * devuelven como un 500 sin detalle: al llamador externo no se le entregan
 * nombres de clase ni de tabla.</p>
 */
@RestControllerAdvice
public class ManejadorErroresApi {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresApi.class);

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ErrorRespuesta> noPermitida(OperacionNoPermitidaException e,
                                                      HttpServletRequest peticion) {
        return construir(HttpStatus.FORBIDDEN, "Acceso denegado", e.getMessage(), peticion);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException e,
                                                       HttpServletRequest peticion) {
        return construir(HttpStatus.NOT_FOUND, "Recurso no encontrado", e.getMessage(), peticion);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> invalida(SolicitudInvalidaException e,
                                                   HttpServletRequest peticion) {
        return construir(HttpStatus.BAD_REQUEST, "Solicitud invalida", e.getMessage(), peticion);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e,
                                                     HttpServletRequest peticion) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Datos de entrada invalidos");
        return construir(HttpStatus.BAD_REQUEST, "Solicitud invalida", detalle, peticion);
    }

    /**
     * Una ruta inexistente no es un fallo del servicio. Sin este manejador, el
     * comodin de mas abajo la convertiria en un 500 y dejaria una traza completa
     * en el log por cada peticion a una URL equivocada.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException e,
                                                          HttpServletRequest peticion) {
        return construir(HttpStatus.NOT_FOUND, "Recurso no encontrado",
                "La ruta solicitada no existe en este servicio", peticion);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e, HttpServletRequest peticion) {
        log.error("Error no controlado en {} {}", peticion.getMethod(), peticion.getRequestURI(), e);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "La solicitud no pudo completarse. Revise el log del servicio.", peticion);
    }

    private ResponseEntity<ErrorRespuesta> construir(HttpStatus estado, String error,
                                                     String detalle, HttpServletRequest peticion) {
        return ResponseEntity.status(estado).body(
                ErrorRespuesta.de(estado.value(), error, detalle, peticion.getRequestURI()));
    }
}
