package com.duoc.bancoxyz.core.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.web.client.RestClient;

/**
 * Cableado del cliente del core. Cada BFF la importa y obtiene un
 * {@link ClienteCoreBancario} listo, sin repetir la configuracion de TLS ni la
 * de credenciales de servicio.
 *
 * <p>Los tiempos de espera son explicitos a proposito: un cliente HTTP sin
 * timeout deja hilos ocupados de forma indefinida cuando el upstream se
 * degrada, y convierte una lentitud del core en una caida del canal.</p>
 */
@Configuration
@EnableConfigurationProperties(PropiedadesCore.class)
public class ConfiguracionClienteCore {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionClienteCore.class);

    @Bean
    public ProveedorTokenServicio proveedorTokenServicio(PropiedadesCore propiedades) {
        return new ProveedorTokenServicio(propiedades);
    }

    @Bean
    public RestClient restClientCore(PropiedadesCore propiedades, SslBundles sslBundles) {
        ClientHttpRequestFactorySettings ajustes = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(propiedades.getTimeoutConexion())
                .withReadTimeout(propiedades.getTimeoutLectura())
                .withSslBundle(sslBundles.getBundle(propiedades.getBundleSsl()));

        log.info("Cliente del core: url={} clienteId={} timeouts={}s/{}s bundleSsl={}",
                propiedades.getUrl(), propiedades.getClienteId(),
                propiedades.getTimeoutConexion().toSeconds(),
                propiedades.getTimeoutLectura().toSeconds(),
                propiedades.getBundleSsl());

        return RestClient.builder()
                .baseUrl(propiedades.getUrl())
                .requestFactory(ClientHttpRequestFactories.get(ajustes))
                .build();
    }

    @Bean
    public ClienteCoreBancario clienteCoreBancario(RestClient restClientCore,
                                                   ProveedorTokenServicio proveedorTokenServicio) {
        return new ClienteCoreBancarioHttp(restClientCore, proveedorTokenServicio);
    }
}
