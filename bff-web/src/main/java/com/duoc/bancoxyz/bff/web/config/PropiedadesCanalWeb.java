package com.duoc.bancoxyz.bff.web.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros del canal web.
 *
 * <p>La vigencia de sesion es larga porque acompana a la persona durante su
 * jornada de trabajo frente al navegador, y el tamano de pagina por defecto es
 * generoso porque la interfaz muestra tablas completas.</p>
 */
@ConfigurationProperties(prefix = "canal-web")
public class PropiedadesCanalWeb {

    private String emisorToken = "bff-web";
    private String audienciaToken = "canal-web";
    private String secretoToken;
    private Duration vigenciaToken = Duration.ofHours(8);

    /** Movimientos incluidos en el panel compuesto. */
    private int movimientosEnPanel = 10;

    /** Tamano de pagina por defecto y maximo del historial. */
    private int tamanoPaginaPorDefecto = 25;
    private int tamanoPaginaMaximo = 100;

    public String getEmisorToken() { return emisorToken; }
    public void setEmisorToken(String v) { this.emisorToken = v; }
    public String getAudienciaToken() { return audienciaToken; }
    public void setAudienciaToken(String v) { this.audienciaToken = v; }
    public String getSecretoToken() { return secretoToken; }
    public void setSecretoToken(String v) { this.secretoToken = v; }
    public Duration getVigenciaToken() { return vigenciaToken; }
    public void setVigenciaToken(Duration v) { this.vigenciaToken = v; }
    public int getMovimientosEnPanel() { return movimientosEnPanel; }
    public void setMovimientosEnPanel(int v) { this.movimientosEnPanel = v; }
    public int getTamanoPaginaPorDefecto() { return tamanoPaginaPorDefecto; }
    public void setTamanoPaginaPorDefecto(int v) { this.tamanoPaginaPorDefecto = v; }
    public int getTamanoPaginaMaximo() { return tamanoPaginaMaximo; }
    public void setTamanoPaginaMaximo(int v) { this.tamanoPaginaMaximo = v; }
}
