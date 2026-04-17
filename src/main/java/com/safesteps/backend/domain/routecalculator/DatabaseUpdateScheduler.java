package com.safesteps.backend.domain.routecalculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DatabaseUpdateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUpdateScheduler.class);

    @Value("${backend.scheduler.python.command:python}")
    private String pythonCommand;

    @Value("${backend.scheduler.scripts.path:src/scripts/}")
    private String scriptsDir;

    private static final List<String> PYTHON_SCRIPTS = List.of(
            "DataLoad_GrafViari.py",
            "DataLoad_Comisaries.py",
            "DataLoad_Fonts_beure.py",
            "DataLoad_Cameres.py",
            "DataLoad_Escales_mecaniques.py",
            "DataLoad_Bancs.py",
            "DataLoad_Arbrat_viari.py",
            "DataLoad_Arbrat_zona.py",
            "DataLoad_Fets_Penals.py",
            "DataLoad_Infraccions_Administratives.py"
    );

    private final PostgisCalculationService postgisCalculationService;

    // INYECCIÓN DE DEPENDENCIAS (D de SOLID)
    @Autowired
    public DatabaseUpdateScheduler(PostgisCalculationService postgisCalculationService) {
        this.postgisCalculationService = postgisCalculationService;
    }

    @Scheduled(cron = "${backend.scheduler.cron:0 0 2 * * *}")
    public void updateDatabaseAndCalculations() {
        logger.info("Iniciando pipeline nocturno de actualización de OpenData BCN");
        try {
            boolean dataLoadSuccess = executePythonDataLoad();
            
            // GUARD CLAUSE (Bouncer Pattern)
            if (!dataLoadSuccess) {
                logger.error("Se abortan los cálculos geométricos espaciales porque la ingesta de datos falló críticamente.");
                return;
            }
            
            postgisCalculationService.performDatabaseCalculations();
            logger.info("Pipeline completado con éxito.");

        } catch (Exception e) {
            logger.error("Error crítico durante la actualización automática: ", e);
        }
    }

    private boolean executePythonDataLoad() {
        logger.info("Directorio raíz de scripts: {}", scriptsDir);

        for (String script : PYTHON_SCRIPTS) {
            if (!executeSingleScript(script)) {
                return false; // Si prefieres detener todo por un solo fallo, si no, puedes obviar este return.
            }
        }
        return true;
    }

    private boolean executeSingleScript(String scriptName) {
        try {
            // USANDO PATH BUILDERS PARA PREVENIR ERRORES DE RUTAS ENTRE WINDOWS/LINUX
            Path scriptAbsPath = Paths.get(scriptsDir, scriptName).toAbsolutePath();
            File scriptFile = scriptAbsPath.toFile();

            if (!scriptFile.exists()) {
                logger.error("El script no existe en disco: {}", scriptAbsPath);
                return false;
            }

            logger.info("Ejecutando [{}]...", scriptName);
            ProcessBuilder pb = new ProcessBuilder(pythonCommand, scriptAbsPath.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            if (!process.waitFor(15, TimeUnit.MINUTES)) {
                logger.error("[TIMEOUT] El script {} excedió los 15 minutos en OS. Forzando SIGKILL.", scriptName);
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("[FALLO] Script {} retornó código {}.", scriptName, exitCode);
                return false;
            }
            return true;

        } catch (Exception e) {
            logger.error("Error de I/O despachando el script {}: {}", scriptName, e.getMessage());
            return false;
        }
    }
}
