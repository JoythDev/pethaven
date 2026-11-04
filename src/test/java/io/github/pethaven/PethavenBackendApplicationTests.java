package io.github.pethaven;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// El perfil "test" usa una base en memoria: la prueba ya no borra el archivo pethaven-database.
@SpringBootTest
@ActiveProfiles("test")
class PethavenBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
