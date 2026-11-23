package Proyecto.Model;

/**
 * Representa un proveedor de productos tecnológicos de TechZone.
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class Proveedor extends Persona {

    private String nombreEmpresa;
    private String nit;

    public Proveedor() {
        super();
    }

    public Proveedor(int id, String nombre, String apellido, String documento,
                     String telefono, String email, String direccion, boolean activo,
                     String nombreEmpresa, String nit) {
        super(id, nombre, apellido, documento, telefono, email, direccion);
        setActivo(activo);
        this.nombreEmpresa = nombreEmpresa;
        this.nit = nit;
    }

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }

    public void setNombreEmpresa(String nombreEmpresa) {
        this.nombreEmpresa = nombreEmpresa;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    @Override
    public String toString() {
        return nombreEmpresa + (nit != null ? " (NIT: " + nit + ")" : "");
    }
}