package com.tienda.ropa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("El archivo está vacío");
        }

        // Crear directorio si no existe
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Obtener extensión del archivo
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        
        // Generar nombre único
        String fileName = UUID.randomUUID().toString() + fileExtension;
        
        // Validar tipo de archivo
        validateImageFile(file);
        
        try {
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Archivo guardado: {}", fileName);
            return fileName;
            
        } catch (IOException ex) {
            log.error("Error al guardar archivo: {}", ex.getMessage());
            throw new IOException("No se pudo guardar el archivo " + fileName, ex);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            Files.deleteIfExists(filePath);
            log.info("Archivo eliminado: {}", fileName);
        } catch (IOException ex) {
            log.error("Error al eliminar archivo: {}", ex.getMessage());
        }
    }

    public String getFileUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        return "/uploads/" + fileName;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private void validateImageFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Solo se permiten archivos de imagen");
        }

        // Validar tamaño (máximo 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IOException("El archivo es demasiado grande. Máximo permitido: 10MB");
        }

        // Validar extensiones permitidas
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
        String fileExtension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        
        boolean isValidExtension = false;
        for (String ext : allowedExtensions) {
            if (fileExtension.equals(ext)) {
                isValidExtension = true;
                break;
            }
        }
        
        if (!isValidExtension) {
            throw new IOException("Extensión de archivo no permitida. Permitidas: jpg, jpeg, png, gif, webp");
        }
    }

    public boolean fileExists(String fileName) {
        Path filePath = Paths.get(uploadDir).resolve(fileName);
        return Files.exists(filePath);
    }

    public long getFileSize(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            return Files.size(filePath);
        } catch (IOException e) {
            log.error("Error obteniendo tamaño del archivo: {}", e.getMessage());
            return 0;
        }
    }
}