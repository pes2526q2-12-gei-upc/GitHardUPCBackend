package com.safesteps.backend.domain.routecalculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostgisCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(PostgisCalculationService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Ejecuta ANALYZE fuera del bloque transaccional, ya que Postgres 
    // no permite comandos de mantenimiento dentro de bloques transaccionales.
    public void executePreAnalysis() {
        logger.info("Executing Pre-Analysis statistics on newly downloaded tables...");
        try {
            jdbcTemplate.execute("ANALYZE bcn_grafvial_nodes;");
            jdbcTemplate.execute("ANALYZE bcn_grafvial_trams;");
        } catch (Exception e) {
            logger.warn("Could not execute ANALYZE. Proceeding anyway. Error: " + e.getMessage());
        }
    }

    public void performDatabaseCalculations() {
        logger.info("Performing database post-calculations from external SQL script...");
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/sql/refresh_grafvial_scores.sql"));
            // ResourceDatabasePopulator reads and executes the statements sequentially
            // Since we are wrapping it in @Transactional, if any block fails, everything
            // rolls back.
            populator.execute(jdbcTemplate.getDataSource());

            logger.info("Finished database post-calculations successfully.");
        } catch (Exception e) {
            logger.error("Failed executing post-calculations script", e);
            throw new RuntimeException("Geometry calculation failed, rolling back.", e);
        }
    }
}
