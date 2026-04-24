package com.safesteps.backend.domain.routecalculator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgisCalculationServiceTest {

    // Mockeamos la dependencia principal
    @Mock
    private JdbcTemplate jdbcTemplate;

    // Mockeamos el DataSource para aislar la conexión a DB
    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    // Inyectamos los mocks mediante el constructor que acabamos de refactorizar
    @InjectMocks
    private PostgisCalculationService service;

    @Test
    void whenExecutePreAnalysis_thenExecutesAnalyzeCommands() {
        // ACT
        service.executePreAnalysis();

        // ASSERT
        verify(jdbcTemplate, times(1)).execute("ANALYZE bcn_grafvial_nodes;");
        verify(jdbcTemplate, times(1)).execute("ANALYZE bcn_grafvial_trams;");
    }

    @Test
    void whenExecutePreAnalysisFails_thenDoesNotThrowException() {
        // ARRANGE
        // Simulamos que PostgreSQL explota en el primer comando
        doThrow(new RuntimeException("DB Connection Lost"))
                .when(jdbcTemplate).execute(anyString());

        // ACT & ASSERT
        // Validamos que el catch() funciona y no propaga el error hacia arriba
        assertDoesNotThrow(() -> service.executePreAnalysis());

        // Verificamos que al fallar el primero, el segundo comando ya no se ejecuta
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void whenPerformDatabaseCalculationsFails_thenThrowsRuntimeException() throws Exception {
        // ARRANGE
        // Engañamos a Spring para que nos dé nuestro DataSource falso
        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);

        // Cuando Spring intente sacar una conexión real, lanzamos una excepción catastrófica
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("Simulated DB Outage"));

        // ACT & ASSERT
        // Validamos que nuestro try-catch captura el error y lanza nuestro RuntimeException específico
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.performDatabaseCalculations());

        // Verificamos que el mensaje del throw coincide con el diseñado
        assert(exception.getMessage().contains("Geometry calculation failed, rolling back."));
    }
}