package com.duoc.bancoxyz.bff.movil.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros del canal movil.
 *
 * <p>La vigencia es corta porque un telefono se pierde o se presta con mucha
 * mas facilidad que un computador de escritorio, y el limite de movimientos es
 * bajo porque la pantalla no muestra mas: pedir cien registros para dibujar
 * cinco es trafico que el usuario paga sin recibir nada a cambio.</p>
 */
@ConfigurationProperties(prefix = "canal-movil")
public class PropiedadesCanalMovil {

    private String emisorToken = "bff-movil";
    private String audienciaToken = "canal-movil";
    private String secretoToken;
    private Duration vigenciaToken = Duration.ofMinutes(15);

    /** Movimientos incluidos en la pantalla de inicio. */
    private int movimientosEnResumen = 5;

    /** Tope de movimientos que la app puede pedir de una vez. */
    private int movimientosMaximo = 20;

    public String getEmisorToken() { return emisorToken; }
    public void setEmisorToken(String v) { this.emisorToken = v; }
    public String getAudienciaToken() { return audienciaToken; }
    public void setAudienciaToken(String v) { this.audienciaToken = v; }
    public String getSecretoToken() { return secretoToken; }
    public void setSecretoToken(String v) { this.secretoToken = v; }
    public Duration getVigenciaToken() { return vigenciaToken; }
    public void setVigenciaToken(Duration v) { this.vigenciaToken = v; }
    public int getMovimientosEnResumen() { return movimientosEnResumen; }
    public void setMovimientosEnResumen(int v) { this.movimientosEnResumen = v; }
    public int getMovimientosMaximo() { return movimientosMaximo; }
    public void setMovimientosMaximo(int v) { this.movimientosMaximo = v; }
}
