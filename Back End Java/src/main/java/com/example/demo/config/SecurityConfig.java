package com.example.demo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/password-reset/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/auth/me").hasRole("MEMBRE")
                        .requestMatchers(HttpMethod.PUT, "/api/auth/me/password").hasRole("MEMBRE")
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/demandes-adhesion").permitAll()
                        .requestMatchers("/api/demandes-adhesion/**").hasRole("ADMIN")
                        .requestMatchers("/api/promotions/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/categories/actives").hasRole("MEMBRE")
                        .requestMatchers(HttpMethod.GET, "/api/produits/actives").hasRole("MEMBRE")
                        .requestMatchers(HttpMethod.GET, "/api/points-collecte/actifs").hasRole("MEMBRE")
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/produits", "/api/produits/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/points-collecte", "/api/points-collecte/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/commandes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/commandes/*/statut").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/commandes/moi").hasRole("MEMBRE")
                        .requestMatchers(HttpMethod.POST, "/api/commandes").hasRole("MEMBRE")
                        .requestMatchers(HttpMethod.PUT, "/api/commandes/*/annuler").authenticated()
                        .requestMatchers("/api/membres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/produits/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/produits/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/produits/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/points-collecte/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/points-collecte/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/points-collecte/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/creneaux-collecte/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/creneaux-collecte/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/creneaux-collecte/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler()))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/h2-console/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Authentification requise.\"}");
        };
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Acces refuse.\"}");
        };
    }
}
