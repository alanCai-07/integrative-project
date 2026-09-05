package Proyecto.Model;

public abstract class Persona {

    protected int id;
    protected String nombre;
    protected String apellido;
    protected String documento;
    protected String telefono;
    protected String email;
    protected String direccion;
    protected Boolean activo;

    public Persona() {
    }

    public Persona(int id, String nombre, String apellido, String documento,
            String telefono, String email, String direccion) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.activo = true;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public String getDireccion() {
        return direccion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setId(int id) {
        if (id < 0)
            throw new IllegalArgumentException("El ID no puede ser negativo");
        this.id = id;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty())
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        this.nombre = nombre.trim();
    }

    public void setApellido(String apellido) {
        if (apellido == null || apellido.trim().isEmpty())
            throw new IllegalArgumentException("El apellido no puede estar vacio");
        this.apellido = apellido.trim();
    }

    public void setDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty())
            throw new IllegalArgumentException("El documento no puede estar vacio");
        this.documento = documento.trim();
    }

    public void setTelefono(String telefono) {
        if (telefono != null && !telefono.trim().isEmpty()) {
            String limpio = telefono.trim();
            // Acepta digitos, espacios, guiones, parentesis, signo +
            if (!limpio.matches("^[\\d\\s\\-\\(\\)\\+]+$")) {
                throw new IllegalArgumentException("Formato de telefono invalido: " + telefono);
            }
            this.telefono = limpio;
        } else {
            this.telefono = null;
        }
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            this.email = null;
            return;
        }
        String limpio = email.trim().toLowerCase();
        if (!limpio.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Formato de email invalido: " + email);
        }
        this.email = limpio;
    }

    /**
     * CORRECCION: direccion opcional. No lanza excepcion con null o vacio.
     * En la BD la columna acepta NULL.
     */
    public void setDireccion(String direccion) {
        this.direccion = (direccion != null && !direccion.trim().isEmpty())
                ? direccion.trim()
                : null;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Nombre: %s %s, Email: %s, Activo: %s",
                id, nombre, apellido, email, activo);
    }
}