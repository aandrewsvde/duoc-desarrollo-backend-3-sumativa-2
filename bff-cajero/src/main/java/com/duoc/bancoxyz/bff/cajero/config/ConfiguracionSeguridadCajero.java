package com.duoc.bancoxyz.bff.cajero.config;

import com.duoc.bancoxyz.seguridad.FiltroAutenticacionJwt;
import com.duoc.bancoxyz.seguridad.ManejadorErroresApi;
import com.duoc.bancoxyz.seguridad.ManejadorErroresSeguridad;
import com.duoc.bancoxyz.seguridad.PropiedadesToken;
import com.duoc.bancoxyz.seguridad.ServicioTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad del canal cajero.
 *
 * <p>Es el unico de los tres que separa permisos por operacion:
 * {@code CONSULTAR_SALDO} y {@code RETIRAR_EFECTIVO} son autoridades distintas.
 * La distincion no es decorativa: permite desplegar manana un cajero de solo
 * consulta -en una sucursal sin dispensador de billetes- emitiendo tokens sin el
 * segundo permiso, sin tocar una linea de codigo.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(ManejadorErroresApi.class)
public class ConfiguracionSeguridadCajero {

    @Bean
    public ServicioTokens servicioTokensCajero(PropiedadesCanalCajero propiedades) {
        return new ServicioTokens(new PropiedadesToken(
                propiedades.getEmisorToken(),
                propiedades.getAudienciaToken(),
                propiedades.getSecretoToken(),
                propiedades.getVigenciaToken()));
    }

    @Bean
    public SecurityFilterChain cadenaSeguridadCajero(HttpSecurity http,
                                                     ServicioTokens servicioTokensCajero,
                                                     ObjectMapper objectMapper) throws Exception {
        ManejadorErroresSeguridad manejador = new ManejadorErroresSeguridad(objectMapper, "CAJERO");

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(manejador)
                    .accessDeniedHandler(manejador))
            .authorizeHttpRequests(rutas -> rutas
                    .requestMatchers("/api/cajero/auth/**", "/actuator/health",
                                     "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/api/cajero/**").hasRole("CAJERO")
                    .anyRequest().authenticated())
            .addFilterBefore(new FiltroAutenticacionJwt(servicioTokensCajero),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
