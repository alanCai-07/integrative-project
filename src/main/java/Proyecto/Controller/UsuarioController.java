package Proyecto.Controller;

import Proyecto.Model.Cliente;
import Proyecto.dao.PersonaDAO;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UsuarioController {

    private PersonaDAO personaDAO;

    public UsuarioController() {
        this.personaDAO = new PersonaDAO();
    }

    // Registrar nuevo cliente
    public boolean registrarCliente(String nombre, String apellido, String email, String telefono,
            String tipoDocumento, String password, String direccion) {
        // Validar que el email no exista
        if (personaDAO.emailExiste(email)) {
            System.out.println("El email ya está registrado");
            return false;
        }

        // Validar campos requeridos
        if (nombre == null || nombre.trim().isEmpty() ||
                apellido == null || apellido.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.length() < 6) {
            System.out.println("Datos incompletos o contraseña muy corta");
            return false;
        }

        // Crear nuevo cliente
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setEmail(email);
        cliente.setTelefono(telefono);
        cliente.setPasswordHash(hasearPassword(password));
        cliente.setDireccion(direccion);

        return personaDAO.crearCliente(cliente);
    }

    // Login de cliente
    public Cliente login(String email, String password) {
        // Obtener cliente por email
        Cliente cliente = personaDAO.obtenerClientePorEmail(email);

        if (cliente == null) {
            System.out.println("Cliente no encontrado");
            return null;
        }

        // Verificar contraseña
        if (verificarPassword(password, cliente.getPasswordHash())) {
            return cliente;
        }

        System.out.println("Contraseña incorrecta");
        return null;
    }

    // Obtener perfil del cliente
    public Cliente obtenerPerfil(int idCliente) {
        return personaDAO.obtenerClientePorId(idCliente);
    }

    // Actualizar perfil del cliente
    public boolean actualizarPerfil(int idCliente, String nombre, String apellido, String telefono, String direccion) {
        Cliente cliente = personaDAO.obtenerClientePorId(idCliente);

        if (cliente == null) {
            System.out.println("Cliente no encontrado");
            return false;
        }

        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setTelefono(telefono);
        cliente.setDireccion(direccion);

        return personaDAO.actualizarCliente(cliente);
    }

    // Cambiar contraseña
    public boolean cambiarPassword(int idCliente, String passwordActual, String passwordNueva) {
        Cliente cliente = personaDAO.obtenerClientePorId(idCliente);

        if (cliente == null) {
            System.out.println("Cliente no encontrado");
            return false;
        }

        // Verificar contraseña actual
        if (!verificarPassword(passwordActual, cliente.getPasswordHash())) {
            System.out.println("Contraseña actual incorrecta");
            return false;
        }

        // Validar nueva contraseña
        if (passwordNueva == null || passwordNueva.length() < 6) {
            System.out.println("La nueva contraseña debe tener al menos 6 caracteres");
            return false;
        }

        String passwordHasheada = hasearPassword(passwordNueva);
        return personaDAO.actualizarPassword(idCliente, passwordHasheada);
    }

    // Desactivar cuenta
    public boolean desactivarCuenta(int idCliente) {
        return personaDAO.eliminarCliente(idCliente);
    }

    // Hashear contraseña
    private String hasearPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error al hashear la contraseña: " + e.getMessage());
            return null;
        }
    }

    // Verificar contraseña
    private boolean verificarPassword(String password, String hashAlmacenado) {
        String hashIngresado = hasearPassword(password);
        return hashIngresado != null && hashIngresado.equals(hashAlmacenado);
    }
}
