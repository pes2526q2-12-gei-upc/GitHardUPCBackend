package com.safesteps.backend.config;

/**
 * Fitxer reservat per a futura configuracio de seguretat.
 *
 * La proteccio del panell WebAdmin es gestiona mitjancant un interceptor
 * de sessio HTTP (AdminSessionInterceptor), registrat a WebMvcConfig.
 *
 * Si en el futur s'afegeix Spring Security al pom.xml, aqui es
 * configuraria el SecurityFilterChain.
 */
public class SecurityConfig {
    // Intencionalment buit - veure AdminSessionInterceptor i WebMvcConfig
}
