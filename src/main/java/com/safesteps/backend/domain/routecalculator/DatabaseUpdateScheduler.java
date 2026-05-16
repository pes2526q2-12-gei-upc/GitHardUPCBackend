package com.safesteps.backend.domain.routecalculator;

import com.safesteps.backend.domain.admin.service.AdminMetricsService;
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
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SUCCESS = "SUCCESS";

    @Value("${backend.scheduler.python.command:python}")
    private String pythonCommand;

    @Value("${backend.scheduler.scripts.path:src/scripts/}")
    private String scriptsDir;

    @Value("${backend.scheduler.enabled:true}")
    private boolean schedulerEnabled;

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
            "DataLoad_Infraccions.py",
            "DataLoad_Qualitat_aire.py",
            "DataLoad_Refugis_climatics.py",
            "DataLoad_Soroll.py"
    );

    private final PostgisCalculationService postgisCalculationService;
    private final AdminMetricsService adminMetricsService;

    @Autowired
    public DatabaseUpdateScheduler(PostgisCalculationService postgisCalculationService,
                                   AdminMetricsService adminMetricsService) {
        this.postgisCalculationService = postgisCalculationService;
        this.adminMetricsService = adminMetricsService;
    }

    //@org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Scheduled(cron = "${backend.scheduler.cron:0 0 2 * * *}")
    public void updateDatabaseAndCalculations() {
        if (!schedulerEnabled) {
            logger.info("El scheduler esta deshabilitado. Saliendo de updateDatabaseAndCalculations.");
            return;
        }

        long pipelineStart = System.nanoTime();
        Long pipelineRunId = adminMetricsService.startPipelineRun();
        String pipelineStatus = STATUS_SUCCESS;
        String pipelineError = null;

        logger.info("Iniciando pipeline nocturno de actualizacion de OpenData BCN");
        try {
            boolean dataLoadSuccess = executePythonDataLoad(pipelineRunId);

            if (!dataLoadSuccess) {
                pipelineStatus = STATUS_FAILED;
                pipelineError = "Data load scripts failed.";
                logger.error("Se abortan los calculos espaciales porque la ingesta de datos fallo criticamente.");
                return;
            }

            postgisCalculationService.executePreAnalysis();
            postgisCalculationService.performDatabaseCalculations();
            logger.info("Pipeline completado con exito.");

        } catch (Exception e) {
            pipelineStatus = STATUS_FAILED;
            pipelineError = e.getMessage();
            logger.error("Error critico durante la actualizacion automatica: ", e);
        } finally {
            long durationMs = (System.nanoTime() - pipelineStart) / 1_000_000;
            adminMetricsService.finishPipelineRun(pipelineRunId, pipelineStatus, durationMs, pipelineError);
        }
    }

    private boolean executePythonDataLoad(Long pipelineRunId) {
        logger.info("Directorio raiz de scripts: {}", scriptsDir);

        for (String script : PYTHON_SCRIPTS) {
            if (!executeSingleScript(script, pipelineRunId)) {
                return false;
            }
        }
        return true;
    }

    private boolean executeSingleScript(String scriptName, Long pipelineRunId) {
        long scriptStart = System.nanoTime();
        try {
            Path scriptAbsPath = Paths.get(scriptsDir, scriptName).toAbsolutePath();
            File scriptFile = scriptAbsPath.toFile();

            if (!scriptFile.exists()) {
                logger.error("El script no existe en disco: {}", scriptAbsPath);
                recordScript(pipelineRunId, scriptName, STATUS_FAILED, scriptStart, null, "Script file not found.");
                return false;
            }

            logger.info("Ejecutando [{}]...", scriptName);
            ProcessBuilder pb = new ProcessBuilder(pythonCommand, scriptAbsPath.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            if (!process.waitFor(15, TimeUnit.MINUTES)) {
                logger.error("[TIMEOUT] El script {} excedio los 15 minutos en OS. Forzando SIGKILL.", scriptName);
                process.destroyForcibly();
                recordScript(pipelineRunId, scriptName, STATUS_FAILED, scriptStart, null, "Timeout after 15 minutes.");
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("[FALLO] Script {} retorno codigo {}.", scriptName, exitCode);
                recordScript(pipelineRunId, scriptName, STATUS_FAILED, scriptStart, exitCode, "Script returned non-zero exit code.");
                return false;
            }

            recordScript(pipelineRunId, scriptName, STATUS_SUCCESS, scriptStart, exitCode, null);
            return true;

        } catch (InterruptedException e) {
            logger.error("[INTERRUPCION] El hilo fue interrumpido mientras esperaba al script {}.", scriptName);
            Thread.currentThread().interrupt();
            recordScript(pipelineRunId, scriptName, STATUS_FAILED, scriptStart, null, e.getMessage());
            return false;

        } catch (java.io.IOException e) {
            logger.error("Error de I/O despachando el script {}: {}", scriptName, e.getMessage());
            recordScript(pipelineRunId, scriptName, STATUS_FAILED, scriptStart, null, e.getMessage());
            return false;
        }
    }

    private void recordScript(Long pipelineRunId, String scriptName, String status, long scriptStart, Integer exitCode, String errorMessage) {
        long durationMs = (System.nanoTime() - scriptStart) / 1_000_000;
        adminMetricsService.recordPipelineScript(pipelineRunId, scriptName, status, durationMs, exitCode, errorMessage);
    }
}
