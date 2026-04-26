package com.safesteps.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ExternalApiAuthInterceptor implements HandlerInterceptor {

    // Llegim el token segur des del application.properties
    @Value("${api.external.token:1234}")
    private String validToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Busquem la capçalera anomenada "X-API-KEY"
        String providedToken = request.getHeader("X-API-KEY");

        // Si el token és correcte, deixem passar la petició (return true)
        if (validToken.equals(providedToken)) {
            return true;
        }

        // Si no hi ha token o és incorrecte, retornem un error 401 (No Autoritzat)
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Error 401: No tens autoritzacio. Falta el X-API-KEY correcte.");
        return false;
    }
}