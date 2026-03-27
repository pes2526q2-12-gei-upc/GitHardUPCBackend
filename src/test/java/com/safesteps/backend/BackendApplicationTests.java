package com.safesteps.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
		assertDoesNotThrow(() -> {
			// El mètode pot estar buit, l'objectiu és que el context arrenqui
		}, "El context de l'aplicació hauria d'arrencar sense errors");
	}

}
