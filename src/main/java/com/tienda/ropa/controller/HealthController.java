package com.tienda.ropa.controller;

import com.tienda.ropa.config.FileUploadConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HealthController {

    private final DataSource dataSource;
    private final FileUploadConfig fileUploadConfig;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        status.put("service", "Catálogo Tienda Ropa API");
        status.put("version", "1.0.0");
        
        // Verificar base de datos
        status.put("database", checkDatabaseHealth());
        
        // Verificar sistema de archivos
        status.put("fileSystem", checkFileSystemHealth());
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        health.put("application", "Catálogo Tienda Ropa");
        health.put("version", "1.0.0");
        health.put("java_version", System.getProperty("java.version"));
        health.put("spring_profile", System.getProperty("spring.profiles.active", "default"));
        
        // Checks detallados
        health.put("checks", Map.of(
            "database", checkDatabaseHealth(),
            "fileSystem", checkFileSystemHealth(),
            "memory", checkMemoryHealth()
        ));
        
        return ResponseEntity.ok(health);
    }

    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        try {
            Connection connection = dataSource.getConnection();
            dbHealth.put("status", "UP");
            dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
            connection.close();
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        return dbHealth;
    }

    private Map<String, Object> checkFileSystemHealth() {
        Map<String, Object> fsHealth = new HashMap<>();
        try {
            boolean isHealthy = fileUploadConfig.isUploadDirectoryHealthy();
            fsHealth.put("status", isHealthy ? "UP" : "DOWN");
            fsHealth.put("uploadDirectory", fileUploadConfig.getUploadDirectory());
        } catch (Exception e) {
            log.error("File system health check failed: {}", e.getMessage());
            fsHealth.put("status", "DOWN");
            fsHealth.put("error", e.getMessage());
        }
        return fsHealth;
    }

    private Map<String, Object> checkMemoryHealth() {
        Map<String, Object> memHealth = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        memHealth.put("status", "UP");
        memHealth.put("max_mb", maxMemory / (1024 * 1024));
        memHealth.put("total_mb", totalMemory / (1024 * 1024));
        memHealth.put("used_mb", usedMemory / (1024 * 1024));
        memHealth.put("free_mb", freeMemory / (1024 * 1024));
        
        return memHealth;
    }
}