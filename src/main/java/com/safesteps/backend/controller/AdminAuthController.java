package com.safesteps.backend.controller;

import com.safesteps.backend.security.AdminSessionInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador que gestiona l'autenticacio del panell d'administracio.
 * Exposa endpoints per fer login, logout i comprovar l'estat de la sessio.
 *
 * Les rutes estan sota /admin (recursos estatics) i no passen per l'interceptor
 * d'administracio perque estan excloses explicitament a WebMvcConfig.
 */
@RestController
@RequiredArgsConstructor
public class AdminAuthController {

    @Value("${admin.password}")
    private String adminPassword;

    /**
     * POST /admin/login
     * Body: { "password": "..." }
     * Crea una sessio HTTP si la contrasenya es correcta.
     */
    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String provided = body.get("password");

        if (adminPassword.equals(provided)) {
            // Invalidem qualsevol sessio anterior per seguretat (session fixation)
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(AdminSessionInterceptor.SESSION_ATTR, true);
            return ResponseEntity.ok(Map.of("status", "OK", "message", "Login correcte"));
        }

        return ResponseEntity.status(401)
                .body(Map.of("status", "ERROR", "message", "Contrasenya incorrecta"));
    }

    /**
     * POST /admin/logout
     * Invalida la sessio activa.
     */
    @PostMapping("/admin/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("status", "OK", "message", "Sessio tancada correctament"));
    }

    /**
     * GET /admin/check-auth
     * Retorna 200 si la sessio es valida, 401 si no.
     * Usat pel frontend per saber si cal redirigir al login.
     */
    @GetMapping("/admin/check-auth")
    public ResponseEntity<Map<String, String>> checkAuth(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(AdminSessionInterceptor.SESSION_ATTR))) {
            return ResponseEntity.ok(Map.of("status", "OK"));
        }
        return ResponseEntity.status(401)
                .body(Map.of("status", "UNAUTHORIZED"));
    }
}
