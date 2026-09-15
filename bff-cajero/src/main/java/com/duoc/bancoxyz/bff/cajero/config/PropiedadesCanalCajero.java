package com.duoc.bancoxyz.bff.cajero.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros del canal cajero.
 *
 * <p>Los limites de retiro viven aqui y no en el core a proposito: son una regla
 * <b>del canal</b>, no del banco. La misma cuenta puede tener otros limites en
 * una transferencia web, y el core no deberia conocer la denominacion de los
 * billetes que carga un cajero.</p>
 */
@ConfigurationProperties(prefix = "canal-cajero")
public class PropiedadesCanalCajero {

    private String emisorToken = "bff-cajero";
    private String audienciaToken = "canal-cajero";
    private String secretoToken;

    /** Dos minutos: lo que dura una persona frente al cajero. */
    private Duration vigenciaToken = Duration.ofSeconds(120);

    /** Monto minimo por operacion. */
    private BigDecimal montoMinimo = new BigDecimal("1000");

    /** Monto maximo por operacion. */
    private BigDecimal montoMaximo = new BigDecimal("200000");

    /** Denominacion: el cajero solo entrega multiplos de este valor. */
    private BigDecimal multiplo = new BigDecimal("1000");

    public String getEmisorToken() { return emisorToken; }
    public void setEmisorToken(String v) { this.emisorToken = v; }
    public String getAudienciaToken() { return audienciaToken; }
    public void setAudienciaToken(String v) { this.audienciaToken = v; }
    public String getSecretoToken() { return secretoToken; }
    public void setSecretoToken(String v) { this.secretoToken = v; }
    public Duration getVigenciaToken() { return vigenciaToken; }
    public void setVigenciaToken(Duration v) { this.vigenciaToken = v; }
    public BigDecimal getMontoMinimo() { return montoMinimo; }
    public void setMontoMinimo(BigDecimal v) { this.montoMinimo = v; }
    public BigDecimal getMontoMaximo() { return montoMaximo; }
    public void setMontoMaximo(BigDecimal v) { this.montoMaximo = v; }
    public BigDecimal getMultiplo() { return multiplo; }
    public void setMultiplo(BigDecimal v) { this.multiplo = v; }
}
