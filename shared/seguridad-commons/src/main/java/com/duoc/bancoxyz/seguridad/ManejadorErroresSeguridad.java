package com.duoc.bancoxyz.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Respuestas 401 y 403 en el mismo formato JSON que el resto de la API.
 *
 * <p>Sin esto, Spring Security devuelve una pagina de error del contenedor:
 * HTML donde el cliente espera JSON. Distinguir 401 de 403 tambien importa para
 * el cliente: el primero significa "identificate o renueva el token" y el
 * segundo "estas identificado, pero este canal no te habilita para esto".</p>
 */
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final String canal;

    public ManejadorErroresSeguridad(ObjectMapper objectMapper, String canal) {
        this.objectMapper = objectMapper;
        this.canal = canal;
    }

    @Override
    public void commence(HttpServletRequest peticion,
                         HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {
        escribir(peticion, respuesta, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado",
                "Se requiere un token valido del canal " + canal
                        + ". Obtenlo en el endpoint de autenticacion de este BFF.");
    }

    @Override
    public void handle(HttpServletRequest peticion,
                       HttpServletResponse respuesta,
                       AccessDeniedException excepcion) throws IOException {
        escribir(peticion, respuesta, HttpServletResponse.SC_FORBIDDEN, "Acceso denegado",
                "El token es valido en el canal " + canal
                        + ", pero no habilita esta operacion.");
    }

    private void escribir(HttpServletRequest peticion, HttpServletResponse respuesta,
                          int estado, String error, String detalle) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(respuesta.getOutputStream(),
                ErrorRespuesta.de(estado, error, detalle, peticion.getRequestURI()));
    }
}
