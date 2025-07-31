package com.tienda.ropa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // IMPORTANTE: Permitir todos los orígenes para desarrollo móvil
        // En producción, especifica las IPs exactas de tus dispositivos
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Métodos HTTP permitidos - TODOS los necesarios para la app móvil
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        // Headers permitidos - TODOS los headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // CRÍTICO: Permitir cookies/credentials para autenticación JWT
        configuration.setAllowCredentials(true);
        
        // Headers expuestos para que el cliente móvil pueda leerlos
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Authorization",
            "Content-Type",
            "Content-Length",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Cache-Control",
            "Pragma"
        ));
        
        // IMPORTANTE: Aumentar el tiempo de cache para requests preflight
        // Esto reduce la cantidad de requests OPTIONS innecesarios
        configuration.setMaxAge(3600L); // 1 hora
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // Aplicar configuración CORS a TODAS las rutas
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}