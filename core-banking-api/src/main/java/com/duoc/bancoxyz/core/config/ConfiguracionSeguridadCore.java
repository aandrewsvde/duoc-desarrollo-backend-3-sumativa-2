package com.duoc.bancoxyz.core.config;

import com.duoc.bancoxyz.seguridad.ConstantesServicio;
import com.duoc.bancoxyz.seguridad.ManejadorErroresApi;
import com.duoc.bancoxyz.seguridad.ManejadorErroresSeguridad;
import com.duoc.bancoxyz.seguridad.PropiedadesToken;
import com.duoc.bancoxyz.seguridad.ServicioTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad del core.
 *
 * <p>El core no atiende usuarios finales: su unica clientela son los tres BFF.
 * Por eso toda la superficie de negocio exige un token de servicio y, ademas,
 * un permiso concreto. Que el canal movil no pueda siquiera invocar el endpoint
 * de retiros no es una comodidad, es una segunda linea de defensa: aunque un
 * error en el BFF movil expusiera una ruta de retiro, el core la rechazaria.</p>
 */
@Configuration
@EnableWebSecurity
@Import(ManejadorErroresApi.class)
public class ConfiguracionSeguridadCore {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionSeguridadCore.class);

    @Bean
    public ServicioTokens servicioTokensCore(PropiedadesCoreApi propiedades) {
        propiedades.getClientes().forEach(c ->
                log.info("Cliente de servicio autorizado: {} -> {}", c.getId(), c.getPermisos()));
        return new ServicioTokens(new PropiedadesToken(
                ConstantesServicio.EMISOR_SERVICIO,
                ConstantesServicio.AUDIENCIA_CORE,
                propiedades.getSecretoServicio(), Duration.ofMinutes(5)));
    }

    @Bean
    public PasswordEncoder codificadorPassword() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadenaSeguridadCore(HttpSecurity http,
                                                   ServicioTokens servicioTokensCore,
                                                   PropiedadesCoreApi propiedades,
                                                   ObjectMapper objectMapper) throws Exception {
        ManejadorErroresSeguridad manejador = new ManejadorErroresSeguridad(objectMapper, "SERVICIO");

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(manejador)
                    .accessDeniedHandler(manejador))
            .authorizeHttpRequests(rutas -> rutas
                    .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
                                     "/swagger-ui.html").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.POST,
                                     "/api/v1/cuentas/*/retiros").hasAuthority("REGISTRAR_RETIRO")
                    .requestMatchers("/api/v1/cuentas/*/resumen-anual").hasAuthority("LEER_RESUMEN")
                    .requestMatchers("/api/v1/cuentas/*/movimientos").hasAuthority("LEER_MOVIMIENTOS")
                    .requestMatchers("/api/v1/cuentas/**").hasAuthority("LEER_CUENTAS")
                    .requestMatchers("/api/v1/identidades/validar-tarjeta").hasAuthority("VALIDAR_TARJETA")
                    .requestMatchers("/api/v1/identidades/**").hasAuthority("VALIDAR_IDENTIDAD")
                    .anyRequest().authenticated())
            .addFilterBefore(new FiltroTokenServicio(servicioTokensCore, propiedades),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
