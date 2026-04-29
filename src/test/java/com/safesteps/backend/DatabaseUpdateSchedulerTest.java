package com.safesteps.backend.domain.routecalculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseUpdateSchedulerTest {

    @Mock
    private PostgisCalculationService postgisCalculationService;

    @InjectMocks
    private DatabaseUpdateScheduler scheduler;

    // JUnit 5 se encarga de crear esta carpeta antes del test y borrarla al terminar
    @TempDir
    Path tempScriptsDir;

    @BeforeEach
    void setUp() {
        // Sustituimos el comando de Python por 'echo' para aislar el test del SO.
        // 'echo' siempre devuelve un código de salida 0 (éxito).
        ReflectionTestUtils.setField(scheduler, "pythonCommand", "echo");
        // Habilitamos el scheduler para las pruebas
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
    }

    @Test
    void whenScriptIsMissing_thenDatabaseCalculationsAreAborted() {
        // ARRANGE
        // Configuramos una ruta de scripts que sabemos que no existe
        ReflectionTestUtils.setField(scheduler, "scriptsDir", "ruta/falsa/absolutamente/inexistente");

        // ACT
        scheduler.updateDatabaseAndCalculations();

        // ASSERT
        // La Guard Clause debió actuar al no encontrar los archivos.
        // Verificamos que el servicio de BD nunca fue tocado.
        verifyNoInteractions(postgisCalculationService);
    }

    @Test
    void whenDatabaseServiceThrowsException_thenSchedulerDoesNotCrash() throws Exception {
        List<String> scripts = List.of(
                "DataLoad_GrafViari.py", "DataLoad_Comisaries.py",
                "DataLoad_Fonts_beure.py", "DataLoad_Cameres.py",
                "DataLoad_Escales_mecaniques.py", "DataLoad_Bancs.py",
                "DataLoad_Arbrat_viari.py", "DataLoad_Arbrat_zona.py",
                "DataLoad_Fets_Penals.py");

        for (String script : scripts) {
            java.nio.file.Files.createFile(tempScriptsDir.resolve(script));
        }

        // Detectamos si estamos en Windows o Linux/Mac
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        // Creamos un ejecutable falso que siempre devuelve 0 (Éxito)
        Path dummyCommand = tempScriptsDir.resolve(isWindows ? "dummy.bat" : "dummy.sh");
        java.nio.file.Files.writeString(dummyCommand, isWindows ? "@echo off\nexit 0" : "#!/bin/sh\nexit 0");
        dummyCommand.toFile().setExecutable(true);

        // Inyectamos la ruta absoluta de nuestro ejecutable falso y la carpeta temporal
        ReflectionTestUtils.setField(scheduler, "pythonCommand", dummyCommand.toAbsolutePath().toString());
        ReflectionTestUtils.setField(scheduler, "scriptsDir", tempScriptsDir.toString());

        // ARRANGE 2: Simulamos una caída catastrófica en la base de datos
        doThrow(new RuntimeException("Caída de PostGIS simulada"))
                .when(postgisCalculationService).performDatabaseCalculations();

        // ACT
        scheduler.updateDatabaseAndCalculations();

        // ASSERT
        verify(postgisCalculationService, times(1)).performDatabaseCalculations();
    }
}