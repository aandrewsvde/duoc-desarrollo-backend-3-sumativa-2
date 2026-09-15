package com.duoc.bancoxyz.bff.web.config;

import com.duoc.bancoxyz.seguridad.FiltroAutenticacionJwt;
import com.duoc.bancoxyz.seguridad.ManejadorErroresApi;
import com.duoc.bancoxyz.seguridad.ManejadorErroresSeguridad;
import com.duoc.bancoxyz.seguridad.PropiedadesToken;
import com.duoc.bancoxyz.seguridad.ServicioTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad del canal web.
 *
 * <p>Dos rasgos lo distinguen de los otros dos canales:</p>
 * <ul>
 *   <li><b>CORS.</b> Es el unico canal que lo necesita, porque es el unico cuyo
 *       cliente es un navegador sujeto a la politica del mismo origen. Las apps
 *       nativas y los cajeros no pasan por ese control, de modo que configurarlo
 *       en ellos seria ruido sin efecto.</li>
 *   <li><b>Dos roles.</b> El titular ve su cuenta; el ejecutivo ve cualquiera.
 *       Esa distincion solo existe aqui: en movil y cajero hay un unico perfil,
 *       porque nadie atiende a terceros desde el telefono de otro ni desde un
 *       cajero.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(ManejadorErroresApi.class)
public class ConfiguracionSeguridadWeb {

    @Bean
    public ServicioTokens servicioTokensWeb(PropiedadesCanalWeb propiedades) {
        return new ServicioTokens(new PropiedadesToken(
                propiedades.getEmisorToken(),
                propiedades.getAudienciaToken(),
                propiedades.getSecretoToken(),
                propiedades.getVigenciaToken()));
    }

    @Bean
    public SecurityFilterChain cadenaSeguridadWeb(HttpSecurity http,
                                                  ServicioTokens servicioTokensWeb,
                                                  ObjectMapper objectMapper) throws Exception {
        ManejadorErroresSeguridad manejador = new ManejadorErroresSeguridad(objectMapper, "WEB");

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(origenesPermitidos()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(manejador)
                    .accessDeniedHandler(manejador))
            .authorizeHttpRequests(rutas -> rutas
                    .requestMatchers("/api/web/auth/**", "/actuator/health",
                                     "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/api/web/**").hasAnyRole("CLIENTE_WEB", "EJECUTIVO_WEB")
                    .anyRequest().authenticated())
            .addFilterBefore(new FiltroAutenticacionJwt(servicioTokensWeb),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource origenesPermitidos() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of("https://localhost:3000", "https://localhost:5173"));
        configuracion.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/web/**", configuracion);
        return fuente;
    }
}
