package com.tienda.ropa.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        createUploadDirIfNotExists();
        validateUploadDirectory();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + getAbsoluteUploadPath() + "/")
                .setCachePeriod(3600);
        
        log.info("✅ Configurado handler de archivos estáticos: /uploads/** -> {}", getAbsoluteUploadPath());
    }

    private void createUploadDirIfNotExists() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("✅ Directorio de uploads creado: {}", uploadPath);
                createSubDirectories(uploadPath);
            } else {
                log.info("📁 Directorio de uploads ya existe: {}", uploadPath);
            }
            
        } catch (Exception e) {
            log.error("❌ Error creando directorio de uploads: {}", e.getMessage());
            throw new RuntimeException("No se pudo crear directorio de uploads", e);
        }
    }

    private void createSubDirectories(Path uploadPath) {
        try {
            String[] subDirs = {"products", "categories", "profiles", "temp"};
            
            for (String subDir : subDirs) {
                Path subDirPath = uploadPath.resolve(subDir);
                if (!Files.exists(subDirPath)) {
                    Files.createDirectories(subDirPath);
                    log.debug("📁 Subdirectorio creado: {}", subDirPath);
                }
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Error creando subdirectorios: {}", e.getMessage());
        }
    }

    private void validateUploadDirectory() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            
            if (!Files.isWritable(uploadPath)) {
                throw new RuntimeException("Directorio de uploads no es escribible: " + uploadPath);
            }
            
            long freeSpace = uploadPath.toFile().getFreeSpace();
            long minRequiredSpace = 100 * 1024 * 1024; // 100MB mínimo
            
            if (freeSpace < minRequiredSpace) {
                log.warn("⚠️ Poco espacio disponible en directorio de uploads: {} MB", 
                        freeSpace / (1024 * 1024));
            }
            
            log.info("✅ Directorio de uploads validado correctamente");
            log.info("📊 Espacio disponible: {} MB", freeSpace / (1024 * 1024));
            
        } catch (Exception e) {
            log.error("❌ Error validando directorio de uploads: {}", e.getMessage());
            throw new RuntimeException("Directorio de uploads no válido", e);
        }
    }

    private String getAbsoluteUploadPath() {
        try {
            return Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            log.error("❌ Error obteniendo path absoluto: {}", e.getMessage());
            return uploadDir;
        }
    }

    public String getUploadDirectory() {
        return getAbsoluteUploadPath();
    }

    public boolean isUploadDirectoryHealthy() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            return Files.exists(uploadPath) && 
                   Files.isDirectory(uploadPath) && 
                   Files.isWritable(uploadPath);
        } catch (Exception e) {
            return false;
        }
    }
}