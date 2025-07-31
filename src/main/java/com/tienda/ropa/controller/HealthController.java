package com.tienda.ropa.controller;

import com.tienda.ropa.config.FileUploadConfig;
import com.tienda.ropa.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final FileService fileService;

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
        
        // Verificar servicio de archivos
        status.put("fileService", checkFileServiceHealth());
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/uploads")
    public ResponseEntity<Map<String, Object>> uploadsHealth() {
        Map<String, Object> status = new HashMap<>();
        
        // Health del sistema de archivos
        Map<String, Object> storageHealth = fileService.getStorageHealth();
        status.putAll(storageHealth);
        
        // Información adicional
        status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        status.put("uploadDirectory", fileUploadConfig.getUploadDirectory());
        status.put("isHealthy", fileUploadConfig.isUploadDirectoryHealthy());
        
        // URLs de prueba
        status.put("testUrls", Map.of(
            "healthCheck", "http://localhost:8080/health",
            "uploadsHealth", "http://localhost:8080/health/uploads",
            "productsEndpoint", "http://localhost:8080/api/products",
            "uploadEndpoint", "http://localhost:8080/api/products/{id}/image",
            "staticFiles", "http://localhost:8080/uploads/products/"
        ));
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/file-info/{fileName}")
    public ResponseEntity<Map<String, Object>> getFileInfo(@PathVariable String fileName) {
        // Agregar prefijo de productos si no está presente
        String fullFileName = fileName.contains("/") ? fileName : "products/" + fileName;
        
        Map<String, Object> info = fileService.getFileInfo(fullFileName);
        info.put("requestedFile", fileName);
        info.put("fullPath", fullFileName);
        info.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return ResponseEntity.ok(info);
    }

    @GetMapping("/test-upload-endpoint")
    public ResponseEntity<Map<String, Object>> testUploadEndpoint() {
        Map<String, Object> test = new HashMap<>();
        test.put("status", "OK");
        test.put("message", "Upload endpoint está funcionando");
        test.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Información del endpoint
        test.put("uploadEndpoint", "/api/products/{id}/image");
        test.put("method", "POST");
        test.put("contentType", "multipart/form-data");
        test.put("parameterName", "file");
        test.put("maxFileSize", "10MB");
        test.put("allowedTypes", "image/jpeg, image/png, image/gif, image/webp");
        
        // URLs de ejemplo
        test.put("examples", Map.of(
            "uploadUrl", "http://localhost:8080/api/products/1/image",
            "curlExample", "curl -X POST -F \"file=@imagen.jpg\" http://localhost:8080/api/products/1/image",
            "testProductInfo", "http://localhost:8080/api/products/1/info"
        ));
        
        // Estado del sistema de archivos
        test.put("fileSystemStatus", fileUploadConfig.isUploadDirectoryHealthy() ? "HEALTHY" : "UNHEALTHY");
        test.put("uploadDirectory", fileUploadConfig.getUploadDirectory());
        
        return ResponseEntity.ok(test);
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
            "fileService", checkFileServiceHealth(),
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
            dbHealth.put("url", connection.getMetaData().getURL());
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
            fsHealth.put("writable", isHealthy);
        } catch (Exception e) {
            log.error("File system health check failed: {}", e.getMessage());
            fsHealth.put("status", "DOWN");
            fsHealth.put("error", e.getMessage());
        }
        return fsHealth;
    }

    private Map<String, Object> checkFileServiceHealth() {
        Map<String, Object> serviceHealth = new HashMap<>();
        try {
            Map<String, Object> storageHealth = fileService.getStorageHealth();
            serviceHealth.put("status", storageHealth.get("status"));
            serviceHealth.put("details", storageHealth);
        } catch (Exception e) {
            log.error("File service health check failed: {}", e.getMessage());
            serviceHealth.put("status", "DOWN");
            serviceHealth.put("error", e.getMessage());
        }
        return serviceHealth;
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
        memHealth.put("usage_percent", (usedMemory * 100.0) / maxMemory);
        
        return memHealth;
    }
}