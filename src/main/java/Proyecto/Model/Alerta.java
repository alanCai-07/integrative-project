package Proyecto.Model;

/**
 * Clase auxiliar para mostrar alertas simples
 */
public class Alerta {
    private String titulo;
    private String mensaje;
    private String tipo; // INFO, WARNING, ERROR

    public Alerta(String titulo, String mensaje, String tipo) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return "Alerta [titulo=" + titulo + ", mensaje=" + mensaje + ", tipo=" + tipo + "]";
    }
}
