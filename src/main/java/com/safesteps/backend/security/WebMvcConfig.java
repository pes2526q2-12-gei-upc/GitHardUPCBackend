package com.safesteps.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ExternalApiAuthInterceptor apiAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apliquem l'interceptor NOMÉS a la ruta de l'altre grup
        registry.addInterceptor(apiAuthInterceptor)
                .addPathPatterns("/api/v1/evaluate-route-security");
    }
}