package com.tienda.ropa.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
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
        logConfiguration();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = getAbsoluteUploadPath();
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/")
                .setCachePeriod(3600)
                .resourceChain(false);
        
        log.info("Handler de archivos configurado:");
        log.info("   URL: /uploads/**");
        log.info("   Path: file:{}/", absolutePath);
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    private void createUploadDirIfNotExists() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Directorio de uploads creado: {}", uploadPath);
            }
            
            createSubDirectories(uploadPath);
            
        } catch (Exception e) {
            log.error("Error creando directorio de uploads: {}", e.getMessage());
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
                    log.info("Subdirectorio creado: {}", subDirPath);
                }
            }
            
        } catch (Exception e) {
            log.warn("Error creando subdirectorios: {}", e.getMessage());
        }
    }

    private void validateUploadDirectory() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            
            if (!Files.isWritable(uploadPath)) {
                throw new RuntimeException("Directorio de uploads no es escribible: " + uploadPath);
            }
            
            log.info("Directorio de uploads validado correctamente");
            
        } catch (Exception e) {
            log.error("Error validando directorio de uploads: {}", e.getMessage());
            throw new RuntimeException("Directorio de uploads no válido", e);
        }
    }

    private void logConfiguration() {
        String absolutePath = getAbsoluteUploadPath();
        log.info("CONFIGURACION DE ARCHIVOS:");
        log.info("   Upload Directory: {}", absolutePath);
        log.info("   URL Base: http://localhost:8080/uploads/");
        log.info("   Directorio writable: {}", Files.isWritable(Paths.get(absolutePath)));
    }

    private String getAbsoluteUploadPath() {
        try {
            return Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            log.error("Error obteniendo path absoluto: {}", e.getMessage());
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