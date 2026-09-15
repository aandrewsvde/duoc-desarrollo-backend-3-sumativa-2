package com.duoc.bancoxyz.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del core: quien puede llamarlo y con que alcance.
 */
@ConfigurationProperties(prefix = "core-api")
public class PropiedadesCoreApi {

    /** Secreto con el que se verifica la firma de los tokens de servicio. */
    private String secretoServicio;

    /** Ejercicio contable por defecto para los resumenes anuales. */
    private int anioEjercicio = 2024;

    /** Clientes de servicio autorizados y el alcance de cada uno. */
    private List<ClienteServicio> clientes = new ArrayList<>();

    private CredencialesDemo credencialesDemo = new CredencialesDemo();

    public String getSecretoServicio() { return secretoServicio; }
    public void setSecretoServicio(String v) { this.secretoServicio = v; }
    public int getAnioEjercicio() { return anioEjercicio; }
    public void setAnioEjercicio(int v) { this.anioEjercicio = v; }
    public List<ClienteServicio> getClientes() { return clientes; }
    public void setClientes(List<ClienteServicio> v) { this.clientes = v; }
    public CredencialesDemo getCredencialesDemo() { return credencialesDemo; }
    public void setCredencialesDemo(CredencialesDemo v) { this.credencialesDemo = v; }

    public Optional<ClienteServicio> buscarCliente(String id) {
        return clientes.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    /**
     * Un BFF autorizado y los permisos que el core le reconoce.
     *
     * <p>Los permisos se leen de aqui y <b>nunca</b> del token recibido. Es
     * deliberado: el secreto de firma es compartido entre el core y los BFF, de
     * modo que un BFF podria emitirse a si mismo un token declarando cualquier
     * permiso. Quien decide el alcance de cada cliente es el core, en su propia
     * configuracion, que el cliente no puede modificar.</p>
     */
    public static class ClienteServicio {
        private String id;
        private List<String> permisos = new ArrayList<>();
        private String descripcion;

        public String getId() { return id; }
        public void setId(String v) { this.id = v; }
        public List<String> getPermisos() { return permisos; }
        public void setPermisos(List<String> v) { this.permisos = v; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String v) { this.descripcion = v; }
    }

    /**
     * Credenciales de demostracion que se siembran al arrancar si la base no
     * tiene ninguna. Estan en configuracion y no en el codigo para que sean
     * visibles y reemplazables sin recompilar; en un entorno real vendrian del
     * directorio corporativo y este bloque no existiria.
     */
    public static class CredencialesDemo {
        private List<UsuarioDemo> usuarios = new ArrayList<>();
        private List<TarjetaDemo> tarjetas = new ArrayList<>();

        public List<UsuarioDemo> getUsuarios() { return usuarios; }
        public void setUsuarios(List<UsuarioDemo> v) { this.usuarios = v; }
        public List<TarjetaDemo> getTarjetas() { return tarjetas; }
        public void setTarjetas(List<TarjetaDemo> v) { this.tarjetas = v; }
    }

    public static class UsuarioDemo {
        private String usuario;
        private String password;
        private String canal;
        private String perfil;
        private Integer cuenta;

        public String getUsuario() { return usuario; }
        public void setUsuario(String v) { this.usuario = v; }
        public String getPassword() { return password; }
        public void setPassword(String v) { this.password = v; }
        public String getCanal() { return canal; }
        public void setCanal(String v) { this.canal = v; }
        public String getPerfil() { return perfil; }
        public void setPerfil(String v) { this.perfil = v; }
        public Integer getCuenta() { return cuenta; }
        public void setCuenta(Integer v) { this.cuenta = v; }
    }

    public static class TarjetaDemo {
        private String numero;
        private String pin;
        private Integer cuenta;

        public String getNumero() { return numero; }
        public void setNumero(String v) { this.numero = v; }
        public String getPin() { return pin; }
        public void setPin(String v) { this.pin = v; }
        public Integer getCuenta() { return cuenta; }
        public void setCuenta(Integer v) { this.cuenta = v; }
    }
}
