package com.duoc.bancoxyz.bff.movil.config;

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
 * Seguridad del canal movil.
 *
 * <p>Sin CORS: el cliente es una aplicacion nativa, no un navegador, de modo que
 * la politica del mismo origen no interviene. Un unico rol, porque en el
 * telefono nadie consulta cuentas de terceros.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(ManejadorErroresApi.class)
public class ConfiguracionSeguridadMovil {

    @Bean
    public ServicioTokens servicioTokensMovil(PropiedadesCanalMovil propiedades) {
        return new ServicioTokens(new PropiedadesToken(
                propiedades.getEmisorToken(),
                propiedades.getAudienciaToken(),
                propiedades.getSecretoToken(),
                propiedades.getVigenciaToken()));
    }

    @Bean
    public SecurityFilterChain cadenaSeguridadMovil(HttpSecurity http,
                                                    ServicioTokens servicioTokensMovil,
                                                    ObjectMapper objectMapper) throws Exception {
        ManejadorErroresSeguridad manejador = new ManejadorErroresSeguridad(objectMapper, "MOVIL");

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(manejador)
                    .accessDeniedHandler(manejador))
            .authorizeHttpRequests(rutas -> rutas
                    .requestMatchers("/api/movil/auth/**", "/actuator/health",
                                     "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/api/movil/**").hasRole("CLIENTE_MOVIL")
                    .anyRequest().authenticated())
            .addFilterBefore(new FiltroAutenticacionJwt(servicioTokensMovil),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
