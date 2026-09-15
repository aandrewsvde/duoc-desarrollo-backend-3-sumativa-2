package com.duoc.bancoxyz.core.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del enlace con el core.
 *
 * <p>Cada BFF se identifica ante el core con su propio {@code clienteId}, de
 * modo que el core puede autorizar por canal y, sobre todo, saber quien pidio
 * que: si manana hay que revocarle el acceso al canal movil, se revoca a ese
 * cliente sin tocar a los otros dos.</p>
 */
@ConfigurationProperties(prefix = "core")
public class PropiedadesCore {

    /** URL base del core, siempre https. */
    private String url = "https://localhost:8843";

    /** Identificador de este BFF ante el core. */
    private String clienteId;

    /** Secreto compartido con el que se firma el token de servicio. */
    private String secretoServicio;

    /** Vigencia del token de servicio; corta porque se renueva solo. */
    private Duration vigenciaToken = Duration.ofMinutes(5);

    private Duration timeoutConexion = Duration.ofSeconds(3);
    private Duration timeoutLectura = Duration.ofSeconds(8);

    /** Nombre del bundle SSL que aporta el almacen de confianza. */
    private String bundleSsl = "bancoxyz";

    public String getUrl() { return url; }
    public void setUrl(String v) { this.url = v; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String v) { this.clienteId = v; }
    public String getSecretoServicio() { return secretoServicio; }
    public void setSecretoServicio(String v) { this.secretoServicio = v; }
    public Duration getVigenciaToken() { return vigenciaToken; }
    public void setVigenciaToken(Duration v) { this.vigenciaToken = v; }
    public Duration getTimeoutConexion() { return timeoutConexion; }
    public void setTimeoutConexion(Duration v) { this.timeoutConexion = v; }
    public Duration getTimeoutLectura() { return timeoutLectura; }
    public void setTimeoutLectura(Duration v) { this.timeoutLectura = v; }
    public String getBundleSsl() { return bundleSsl; }
    public void setBundleSsl(String v) { this.bundleSsl = v; }
}
