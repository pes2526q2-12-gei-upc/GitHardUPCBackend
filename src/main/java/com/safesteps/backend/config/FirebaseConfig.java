package com.safesteps.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.FileInputStream;

@Configuration
@Profile("!test")
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.firebase.config:}")
    private String configPath;

    @PostConstruct
    public void init() {
        if (configPath == null || configPath.isEmpty()) {
            log.error("Firebase not initialized as path is null or empty.");
            return;
        }

        File file = new File(configPath);
        if (!file.exists()) {
            log.error("Firebase not initialized as file not found at path: {}", configPath);
            return;
        }

        try(FileInputStream serviceAccount = new FileInputStream(configPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (Exception ignored) {
            log.error("Firebase not initialized.");
        }
    }
}