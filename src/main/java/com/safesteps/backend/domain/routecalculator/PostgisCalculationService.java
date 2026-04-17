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

    @Transactional
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
