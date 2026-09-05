package Proyecto.Model;

public class Proveedor extends Persona {

    // Constructor sin parámetros
    public Proveedor() {
        super();
    }

    // Constructor con parámetros
    public Proveedor(int id, String nombre, String apellido, String documento, String telefono,
            String email, String direccion) {
        super(id, nombre, apellido, documento, telefono, email, direccion);
    }

    @Override
    public String toString() {
        return String.format("Proveedor - ID: %d, Nombre: %s, Apellido: %s, Email: %s, Dirección: %s, Activo: %s",
                id, nombre, apellido, email, direccion, activo);
    }
}
