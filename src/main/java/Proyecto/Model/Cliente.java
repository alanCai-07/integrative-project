package Proyecto.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente — CORRECCIÓN APLICADA:
 * Se agrega el campo 'rol' para transportar el tipo de usuario
 * (CLIENTE, ADMINISTRADOR, VENDEDOR, CAJERO, COMPRADOR, BODEGUERO)
 * desde la autenticación hasta MenuPrincipalView.
 */
public class Cliente extends Persona {

    private String        passwordHash;
    private LocalDateTime fechaRegistro;
    private List<Documento> historialCompras;

    // Campo nuevo: rol del usuario autenticado
    private String rol = "CLIENTE";

    // ── Constructores ──────────────────────────────────────────────────────────
    public Cliente() {
        super();
        this.fechaRegistro   = LocalDateTime.now();
        this.historialCompras = new ArrayList<>();
    }

    public Cliente(int id, String nombre, String apellido, String documento,
            String telefono, String email, String direccion, String passwordHash) {
        super(id, nombre, apellido, documento, telefono, email, direccion);
        this.passwordHash    = passwordHash;
        this.fechaRegistro   = LocalDateTime.now();
        this.historialCompras = new ArrayList<>();
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────
    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty())
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        this.passwordHash = passwordHash.trim();
    }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        if (fechaRegistro == null)
            throw new IllegalArgumentException("La fecha de registro no puede ser nula");
        this.fechaRegistro = fechaRegistro;
    }

    public List<Documento> getHistorialCompras() { return historialCompras; }

    public void setHistorialCompras(List<Documento> historialCompras) {
        this.historialCompras = historialCompras != null ? historialCompras : new ArrayList<>();
    }

    // ── Rol del usuario ────────────────────────────────────────────────────────
    public String getRol() { return rol; }

    public void setRol(String rol) {
        this.rol = (rol != null) ? rol.toUpperCase().trim() : "CLIENTE";
    }

    /** Devuelve true si el usuario tiene algún rol de empleado */
    public boolean esEmpleado() {
        return !"CLIENTE".equals(rol);
    }

    /** Devuelve true si el usuario es administrador */
    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(rol);
    }

    public void agregarCompra(Documento documento) {
        if (documento != null) this.historialCompras.add(documento);
    }
}
