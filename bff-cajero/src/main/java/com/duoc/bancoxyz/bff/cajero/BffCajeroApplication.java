package com.duoc.bancoxyz.bff.cajero;

import com.duoc.bancoxyz.bff.cajero.config.PropiedadesCanalCajero;
import com.duoc.bancoxyz.core.client.ConfiguracionClienteCore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * BFF del canal de cajeros automaticos.
 *
 * <p>Es el canal mas pequeno de los tres y el mas restrictivo, y ambas cosas
 * tienen el mismo origen: un cajero hace dos cosas -consultar saldo y entregar
 * efectivo- frente a una persona de pie en la calle. De ahi se derivan sus tres
 * rasgos distintivos:</p>
 *
 * <ul>
 *   <li><b>Superficie minima.</b> Dos endpoints de negocio. Todo lo que no sea
 *       saldo o retiro simplemente no existe en este canal, de modo que tampoco
 *       puede ser atacado.</li>
 *   <li><b>Sesion de dos minutos.</b> Si el usuario se aleja del cajero, la
 *       sesion ya expiro. Comparese con las ocho horas del canal web.</li>
 *   <li><b>La cuenta sale del token.</b> Ningun endpoint la recibe, igual que en
 *       movil, pero aqui importa mas: es dinero.</li>
 * </ul>
 */
@SpringBootApplication
@EnableConfigurationProperties(PropiedadesCanalCajero.class)
@Import(ConfiguracionClienteCore.class)
public class BffCajeroApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffCajeroApplication.class, args);
    }
}
