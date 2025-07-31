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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    // Configuraciones de validación
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", 
            "image/webp", "image/bmp"
    );

    /**
     * Guarda un archivo en el sistema de archivos
     */
    public String saveFile(MultipartFile file) throws IOException {
        log.info("=== INICIANDO GUARDADO DE ARCHIVO ===");
        log.info("Archivo recibido:");
        log.info("  - Nombre original: {}", file.getOriginalFilename());
        log.info("  - Tamaño: {} bytes ({} KB)", file.getSize(), file.getSize() / 1024);
        log.info("  - Content-Type: {}", file.getContentType());
        log.info("  - Vacío: {}", file.isEmpty());
        
        // Validaciones básicas
        if (file.isEmpty()) {
            throw new IOException("El archivo está vacío");
        }

        // Crear directorio principal si no existe
        Path uploadPath = createUploadDirectory();
        log.info("Directorio de upload: {}", uploadPath.toAbsolutePath());
        
        // Crear subdirectorio para productos
        Path productsPath = uploadPath.resolve("products");
        if (!Files.exists(productsPath)) {
            Files.createDirectories(productsPath);
            log.info("Subdirectorio 'products' creado: {}", productsPath);
        }

        // Validar archivo
        validateImageFile(file);
        log.info("Archivo validado correctamente");
        
        // Generar nombre único para el archivo
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileName = generateUniqueFileName(fileExtension);
        
        log.info("Nombres de archivo:");
        log.info("  - Original: {}", originalFilename);
        log.info("  - Generado: {}", uniqueFileName);
        log.info("  - Extensión: {}", fileExtension);
        
        try {
            // Ruta completa donde se guardará el archivo
            Path targetLocation = productsPath.resolve(uniqueFileName);
            log.info("Guardando en: {}", targetLocation.toAbsolutePath());
            
            // Copiar archivo
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Verificar que el archivo se guardó correctamente
            if (Files.exists(targetLocation)) {
                long savedFileSize = Files.size(targetLocation);
                log.info("=== ARCHIVO GUARDADO EXITOSAMENTE ===");
                log.info("  - Ubicación: {}", targetLocation.toAbsolutePath());
                log.info("  - Tamaño verificado: {} bytes", savedFileSize);
                log.info("  - URL relativa: /uploads/products/{}", uniqueFileName);
                log.info("  - URL completa: http://localhost:8080/uploads/products/{}", uniqueFileName);
                
                // Verificar integridad del archivo
                if (savedFileSize != file.getSize()) {
                    log.warn("Tamaño del archivo guardado ({}) diferente al original ({})", 
                            savedFileSize, file.getSize());
                } else {
                    log.info("Integridad del archivo verificada correctamente");
                }
            } else {
                throw new IOException("El archivo no se guardó correctamente en la ubicación esperada");
            }
            
            // Retornar path relativo para almacenar en BD
            String relativePath = "products/" + uniqueFileName;
            log.info("Path relativo retornado: {}", relativePath);
            return relativePath;
            
        } catch (IOException ex) {
            log.error("Error al guardar archivo '{}': {}", originalFilename, ex.getMessage());
            throw new IOException("No se pudo guardar el archivo: " + ex.getMessage(), ex);
        }
    }

    /**
     * Genera URL pública para acceder al archivo
     */
    public String getFileUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            log.debug("getFileUrl called with null/empty fileName");
            return null;
        }
        
        // Asegurar que el path comience con /uploads/
        String url = fileName.startsWith("/uploads/") ? fileName : "/uploads/" + fileName;
        
        log.info("URL generada:");
        log.info("  - Input fileName: {}", fileName);
        log.info("  - Generated URL: {}", url);
        log.info("  - Full URL: http://localhost:8080{}", url);
        
        return url;
    }

    /**
     * Verifica si un archivo existe
     */
    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            boolean exists = Files.exists(filePath);
            log.debug("Verificando existencia de '{}': {} (path: {})", 
                     fileName, exists, filePath.toAbsolutePath());
            return exists;
        } catch (Exception e) {
            log.error("Error verificando existencia del archivo '{}': {}", fileName, e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un archivo del sistema de archivos
     */
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("Intento de eliminar archivo con nombre vacío");
            return;
        }
        
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            log.info("Intentando eliminar archivo: {}", filePath.toAbsolutePath());
            
            if (Files.exists(filePath)) {
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    log.info("Archivo eliminado exitosamente: {}", fileName);
                } else {
                    log.warn("No se pudo eliminar el archivo: {}", fileName);
                }
            } else {
                log.warn("Archivo no encontrado para eliminar: {}", filePath.toAbsolutePath());
            }
            
        } catch (IOException ex) {
            log.error("Error al eliminar archivo '{}': {}", fileName, ex.getMessage());
        }
    }

    // =================== MÉTODOS PRIVADOS ===================

    /**
     * Crea el directorio de uploads si no existe
     */
    private Path createUploadDirectory() throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Directorio principal de uploads creado: {}", uploadPath);
        }
        
        // Verificar permisos de escritura
        if (!Files.isWritable(uploadPath)) {
            throw new IOException("El directorio de uploads no tiene permisos de escritura: " + uploadPath);
        }
        
        return uploadPath;
    }

    /**
     * Valida que el archivo sea una imagen válida
     */
    private void validateImageFile(MultipartFile file) throws IOException {
        log.debug("Validando archivo de imagen...");
        
        // Validar Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Content-Type no permitido: {}", contentType);
            throw new IOException("Tipo de archivo no permitido. Tipos permitidos: " + 
                    String.join(", ", ALLOWED_CONTENT_TYPES));
        }

        // Validar tamaño
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("Archivo demasiado grande: {} bytes (máximo: {} bytes)", 
                    file.getSize(), MAX_FILE_SIZE);
            throw new IOException("El archivo es demasiado grande. Tamaño máximo permitido: " + 
                    (MAX_FILE_SIZE / (1024 * 1024)) + " MB");
        }

        // Validar extensión del archivo
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("El archivo no tiene nombre");
        }
        
        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            log.warn("Extensión no permitida: {}", fileExtension);
            throw new IOException("Extensión de archivo no permitida. Extensiones permitidas: " + 
                    String.join(", ", ALLOWED_EXTENSIONS));
        }
        
        // Validar tamaño mínimo (evitar archivos corruptos)
        if (file.getSize() < 100) { // 100 bytes mínimo
            throw new IOException("El archivo es demasiado pequeño, puede estar corrupto");
        }
        
        log.debug("Archivo validado correctamente:");
        log.debug("  - Content-Type: {}", contentType);
        log.debug("  - Extensión: {}", fileExtension);
        log.debug("  - Tamaño: {} KB", file.getSize() / 1024);
    }

    /**
     * Extrae la extensión del archivo incluyendo el punto
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex).toLowerCase();
    }

    /**
     * Genera un nombre único para el archivo
     */
    private String generateUniqueFileName(String extension) {
        // Usar UUID + timestamp para garantizar unicidad
        String uuid = UUID.randomUUID().toString();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Combinar UUID (primeros 8 caracteres) + timestamp + extensión
        String shortUuid = uuid.substring(0, 8);
        String uniqueName = shortUuid + "_" + timestamp + extension;
        
        log.debug("Nombre único generado: {}", uniqueName);
        return uniqueName;
    }

    /**
     * Método para inicialización del servicio
     */
    @jakarta.annotation.PostConstruct
    public void initialize() {
        log.info("=== INICIALIZANDO FileService ===");
        log.info("Directorio de uploads configurado: {}", uploadDir);
        
        try {
            createUploadDirectory();
            
            // Crear subdirectorios necesarios
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            String[] subdirectories = {"products", "categories", "profiles", "temp"};
            
            for (String subDir : subdirectories) {
                Path subDirPath = uploadPath.resolve(subDir);
                if (!Files.exists(subDirPath)) {
                    Files.createDirectories(subDirPath);
                    log.info("Subdirectorio creado: {}", subDirPath);
                }
            }
            
            // Log de configuración
            log.info("Configuración del FileService:");
            log.info("  - Tamaño máximo de archivo: {} MB", MAX_FILE_SIZE / (1024 * 1024));
            log.info("  - Extensiones permitidas: {}", String.join(", ", ALLOWED_EXTENSIONS));
            log.info("  - Content-Types permitidos: {}", String.join(", ", ALLOWED_CONTENT_TYPES));
            log.info("  - Directorio absoluto: {}", uploadPath);
            
            // Verificar salud del storage
            long freeSpace = uploadPath.toFile().getFreeSpace();
            log.info("  - Espacio libre: {} MB", freeSpace / (1024 * 1024));
            log.info("  - Directorio escribible: {}", Files.isWritable(uploadPath));
            
        } catch (Exception e) {
            log.error("Error inicializando FileService: {}", e.getMessage());
            throw new RuntimeException("No se pudo inicializar el servicio de archivos", e);
        }
        
        log.info("=== FileService inicializado correctamente ===");
    }

    /**
     * Obtiene información detallada de un archivo
     */
    public java.util.Map<String, Object> getFileInfo(String fileName) {
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        
        if (fileName == null || fileName.isEmpty()) {
            info.put("exists", false);
            info.put("error", "Nombre de archivo vacío");
            return info;
        }
        
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            
            if (Files.exists(filePath)) {
                info.put("exists", true);
                info.put("fileName", fileName);
                info.put("absolutePath", filePath.toAbsolutePath().toString());
                info.put("size", Files.size(filePath));
                info.put("lastModified", Files.getLastModifiedTime(filePath).toString());
                info.put("isReadable", Files.isReadable(filePath));
                info.put("url", getFileUrl(fileName));
            } else {
                info.put("exists", false);
                info.put("fileName", fileName);
                info.put("searchedPath", filePath.toAbsolutePath().toString());
            }
            
        } catch (Exception e) {
            info.put("exists", false);
            info.put("error", e.getMessage());
        }
        
        return info;
    }

    /**
     * Verifica la salud del sistema de archivos
     */
    public java.util.Map<String, Object> getStorageHealth() {
        java.util.Map<String, Object> health = new java.util.HashMap<>();
        
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            
            // Información básica
            health.put("uploadDirectory", uploadPath.toString());
            health.put("exists", Files.exists(uploadPath));
            health.put("writable", Files.isWritable(uploadPath));
            health.put("readable", Files.isReadable(uploadPath));
            
            if (Files.exists(uploadPath)) {
                // Información de espacio
                long totalSpace = uploadPath.toFile().getTotalSpace();
                long freeSpace = uploadPath.toFile().getFreeSpace();
                long usedSpace = totalSpace - freeSpace;
                
                health.put("totalSpaceBytes", totalSpace);
                health.put("freeSpaceBytes", freeSpace);
                health.put("usedSpaceBytes", usedSpace);
                health.put("totalSpaceMB", totalSpace / (1024 * 1024));
                health.put("freeSpaceMB", freeSpace / (1024 * 1024));
                health.put("usedSpaceMB", usedSpace / (1024 * 1024));
                health.put("freeSpacePercent", (freeSpace * 100.0) / totalSpace);
                
                // Conteo de archivos
                Path productsPath = uploadPath.resolve("products");
                if (Files.exists(productsPath)) {
                    long fileCount = Files.list(productsPath)
                        .filter(Files::isRegularFile)
                        .count();
                    health.put("productFilesCount", fileCount);
                } else {
                    health.put("productFilesCount", 0);
                }
                
                // Estado general
                boolean healthy = Files.isWritable(uploadPath) && 
                                freeSpace > (100 * 1024 * 1024); // 100MB mínimo
                health.put("healthy", healthy);
                health.put("status", healthy ? "UP" : "DOWN");
                
            } else {
                health.put("healthy", false);
                health.put("status", "DOWN");
                health.put("error", "Upload directory does not exist");
            }
            
        } catch (Exception e) {
            health.put("healthy", false);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}