package Proyecto.Controller;

import Proyecto.Model.Cliente;
import Proyecto.dao.PersonaDAO;

public class UsuarioController {

    private final PersonaDAO personaDAO;

    public UsuarioController() {
        this.personaDAO = new PersonaDAO();
    }

    public boolean registrarCliente(String nombre, String apellido, String email, String telefono,
            String documento, String password, String direccion) {
        if (email == null || personaDAO.emailExiste(email)) {
            return false;
        }
        if (nombre == null || nombre.trim().isEmpty()
                || apellido == null || apellido.trim().isEmpty()
                || email.trim().isEmpty()
                || password == null || password.length() < 6) {
            return false;
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(nombre.trim());
        cliente.setApellido(apellido.trim());
        cliente.setEmail(email.trim().toLowerCase());
        cliente.setTelefono(telefono);
        cliente.setDocumento(documento);
        cliente.setPasswordHash(password);
        cliente.setDireccion(direccion);

        return personaDAO.crearCliente(cliente);
    }

    public Cliente login(String email, String password) {
        if (email == null || password == null) {
            return null;
        }
        Cliente cliente = personaDAO.obtenerCredencialesClientePorEmail(email);
        if (cliente != null && PersonaDAO.verificarPassword(password, cliente.getPasswordHash())) {
            cliente.setPasswordHash(null);
            return cliente;
        }
        Cliente empleado = personaDAO.obtenerEmpleadoPorEmail(email);
        if (empleado != null && PersonaDAO.verificarPassword(password, empleado.getPasswordHash())) {
            empleado.setPasswordHash(null);
            return empleado;
        }
        return null;
    }

    public Cliente obtenerPerfil(int idCliente) {
        return personaDAO.obtenerClientePorId(idCliente);
    }

    public boolean actualizarPerfil(int idCliente, String nombre, String apellido, String telefono, String direccion) {
        Cliente cliente = personaDAO.obtenerClientePorId(idCliente);
        if (cliente == null) {
            return false;
        }
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setTelefono(telefono);
        cliente.setDireccion(direccion);
        return personaDAO.actualizarCliente(cliente);
    }

    public boolean cambiarPassword(int idCliente, String passwordActual, String passwordNueva) {
        Cliente perfil = personaDAO.obtenerClientePorId(idCliente);
        if (perfil == null || perfil.getEmail() == null || passwordNueva == null || passwordNueva.length() < 6) {
            return false;
        }
        Cliente credenciales = personaDAO.obtenerCredencialesClientePorEmail(perfil.getEmail());
        if (credenciales == null || !PersonaDAO.verificarPassword(passwordActual, credenciales.getPasswordHash())) {
            return false;
        }
        return personaDAO.actualizarPassword(idCliente, passwordNueva);
    }

    public boolean desactivarCuenta(int idCliente) {
        return personaDAO.eliminarCliente(idCliente);
    }
}