package Proyecto.Model;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

class ClienteTest {

    @Test
    void clearsPasswordHashAfterSuccessfulAuthentication() {
        String password = "TechZone123";
        Cliente cliente = new Cliente();
        cliente.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));

        assertTrue(BCrypt.checkpw(password, cliente.getPasswordHash()));

        cliente.setPasswordHash(null);

        assertNull(cliente.getPasswordHash());
    }
}
