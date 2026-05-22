package com.safesteps.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ExternalApiAuthInterceptor implements HandlerInterceptor {

    // 1. Definim els dos tokens que llegirem del application.properties
    @Value("${api.internal.token}")
    private String internalApiKey;

    @Value("${api.external.token}")
    private String externalApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String path = request.getRequestURI();

        // 2. Deixem passar rutes públiques (com el Swagger o errors) sense demanar token
        if (path.contains("/swagger-ui") || path.contains("/v3/api-docs") || path.contains("/error")) {
            return true;
        }

        // Busquem la capçalera
        String requestApiKey = request.getHeader("X-API-KEY");

        if (requestApiKey == null || requestApiKey.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Error 401: Falta el token de seguretat X-API-KEY.");
            return false;
        }

        // 3. EXCEPCIÓ: El servei que oferim a fora
        if (path.contains("/api/v1/evaluate-route-security")) {
            // Aquí acceptem TANT el token de l'altre grup COM el vostre intern (per si el frontend ho vol provar)
            if (requestApiKey.equals(externalApiKey) || requestApiKey.equals(internalApiKey)) {
                return true;
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Error 401: Token extern invalid.");
                return false;
            }
        }

        // 4. REGLA GENERAL: Tota la resta de la vostra API (calcular rutes, perfils, events, etc.)
        if (requestApiKey.equals(internalApiKey)) {
            return true;
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Error 401: Token intern invalid. Acces denegat a l'API de SafeSteps.");
            return false;
        }
    }
}