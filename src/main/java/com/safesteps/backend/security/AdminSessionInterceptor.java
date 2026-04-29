package com.safesteps.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que protegeix totes les rutes /api/admin/** i /admin/**
 * (excepte /admin/login.html i POST /admin/login) exigint que hi hagi
 * una sessio HTTP activa amb l'atribut adminAuthenticated = true.
 */
@Component
public class AdminSessionInterceptor implements HandlerInterceptor {

    public static final String SESSION_ATTR = "adminAuthenticated";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_ATTR))) {
            return true;
        }

        // Peticio AJAX (Accept: application/json) -> 401 JSON
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"NOT_AUTHENTICATED\",\"message\":\"Sessio d'administrador no valida. Fes login a /admin/login.html\"}");
        } else {
            // Peticio de navegador -> redirigir al login
            response.sendRedirect("/admin/login.html");
        }
        return false;
    }
}
