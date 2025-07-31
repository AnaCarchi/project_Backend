package com.tienda.ropa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        
                        // ⚠️ IMPORTANTE: Permitir acceso a archivos estáticos
                        .requestMatchers("/uploads/**").permitAll()
                        
                        // Endpoints de desarrollo
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/health/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        
                        // Endpoints de productos (lectura para usuarios, escritura para admins)
                        .requestMatchers("GET", "/api/products/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("POST", "/api/products/**").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/products/**").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/api/products/**").hasRole("ADMIN")
                        .requestMatchers("PATCH", "/api/products/**").hasRole("ADMIN")
                        
                        // Endpoints de categorías (lectura para usuarios, escritura para admins)
                        .requestMatchers("GET", "/api/categories/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("POST", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers("PATCH", "/api/categories/**").hasRole("ADMIN")
                        
                        // Endpoints de administración
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")
                        
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}