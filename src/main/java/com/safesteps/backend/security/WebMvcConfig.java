package com.safesteps.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.safesteps.backend.security.AdminSessionInterceptor;
import com.safesteps.backend.security.ExternalApiAuthInterceptor;

@Configuration
@EnableSpringDataWebSupport
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ExternalApiAuthInterceptor apiAuthInterceptor;

    @Autowired
    private AdminSessionInterceptor adminSessionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor per a l'API externa de calcul de rutes
        registry.addInterceptor(apiAuthInterceptor)
                .addPathPatterns("/api/v1/evaluate-route-security");

        // Interceptor que protegeix el panell d'administracio amb sessio HTTP.
        // Protegim /api/admin/** i /admin/index.html pero excloem:
        //   - /admin/login.html  -> pagina publica de login
        //   - /admin/login       -> endpoint POST de login
        //   - /admin/logout      -> endpoint POST de logout
        //   - /admin/check-auth  -> endpoint GET de comprovacio de sessio
        //   - /admin/css/**      -> recursos estatics (fulls d'estil)
        //   - /admin/js/**       -> recursos estatics (scripts)
        registry.addInterceptor(adminSessionInterceptor)
                .addPathPatterns("/api/admin/**", "/admin/index.html")
                .excludePathPatterns(
                        "/admin/login.html",
                        "/admin/login",
                        "/admin/logout",
                        "/admin/check-auth",
                        "/admin/css/**",
                        "/admin/js/**"
                );
    }
}