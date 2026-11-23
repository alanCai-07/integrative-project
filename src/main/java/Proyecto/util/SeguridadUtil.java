package Proyecto.util;

import java.security.SecureRandom;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad para el cifrado seguro de contraseñas mediante BCrypt con sal aleatoria.
 * Atiende el requisito de seguridad RNF-02.
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class SeguridadUtil {

    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Genera un hash BCrypt de 60 caracteres con sal aleatoria.
     *
     * @param passwordPlano Contraseña en texto plano.
     * @return Hash BCrypt listo para almacenar en base de datos.
     */
    public static String hashPassword(String passwordPlano) {
        if (passwordPlano == null || passwordPlano.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(10));
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un hash BCrypt almacenado.
     *
     * @param passwordPlano Contraseña ingresada por el usuario.
     * @param hashAlmacenado Hash BCrypt recuperado de PostgreSQL.
     * @return true si coincide, false en caso contrario.
     */
    public static boolean verificarPassword(String passwordPlano, String hashAlmacenado) {
        if (passwordPlano == null || hashAlmacenado == null || !hashAlmacenado.startsWith("$2")) {
            return false;
        }
        return BCrypt.checkpw(passwordPlano, hashAlmacenado);
    }

    /**
     * Genera una contraseña aleatoria segura para registros rápidos de clientes.
     *
     * @param longitud Longitud deseada (ej. 12).
     * @return Cadena aleatoria segura.
     */
    public static String generarPasswordAleatoria(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(CARACTERES.charAt(RANDOM.nextInt(CARACTERES.length())));
        }
        return sb.toString();
    }
}