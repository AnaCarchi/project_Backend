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
     * @param file Archivo a guardar
     * @return Nombre del archivo guardado (path relativo)
     * @throws IOException Si hay error al guardar
     */
    public String saveFile(MultipartFile file) throws IOException {
        log.info(" Iniciando guardado de archivo: {}", file.getOriginalFilename());
        log.debug(" Información del archivo:");
        log.debug("    Nombre original: {}", file.getOriginalFilename());
        log.debug("    Tamaño: {} bytes ({} KB)", file.getSize(), file.getSize() / 1024);
        log.debug("    Content-Type: {}", file.getContentType());
        
        // Validaciones básicas
        if (file.isEmpty()) {
            throw new IOException("El archivo está vacío");
        }

        // Crear directorio principal si no existe
        Path uploadPath = createUploadDirectory();
        
        // Crear subdirectorio para productos
        Path productsPath = uploadPath.resolve("products");
        if (!Files.exists(productsPath)) {
            Files.createDirectories(productsPath);
            log.info(" Subdirectorio 'products' creado: {}", productsPath);
        }

        // Validar archivo
        validateImageFile(file);
        
        // Generar nombre único para el archivo
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileName = generateUniqueFileName(fileExtension);
        
        log.info(" Nombres de archivo:");
        log.info("    Original: {}", originalFilename);
        log.info("    Generado: {}", uniqueFileName);
        
        try {
            // Ruta completa donde se guardará el archivo
            Path targetLocation = productsPath.resolve(uniqueFileName);
            log.info(" Guardando en: {}", targetLocation.toAbsolutePath());
            
            // Copiar archivo
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Verificar que el archivo se guardó correctamente
            if (Files.exists(targetLocation)) {
                long savedFileSize = Files.size(targetLocation);
                log.info(" Archivo guardado exitosamente:");
                log.info("    Ubicación: {}", targetLocation.toAbsolutePath());
                log.info("    Tamaño verificado: {} bytes", savedFileSize);
                log.info("    URL relativa: /uploads/products/{}", uniqueFileName);
                
                // Verificar integridad del archivo
                if (savedFileSize != file.getSize()) {
                    log.warn(" Tamaño del archivo guardado ({}) diferente al original ({})", 
                            savedFileSize, file.getSize());
                }
            } else {
                throw new IOException("El archivo no se guardó correctamente en la ubicación esperada");
            }
            
            // Retornar path relativo para almacenar en BD
            return "products/" + uniqueFileName;
            
        } catch (IOException ex) {
            log.error(" Error al guardar archivo '{}': {}", originalFilename, ex.getMessage());
            throw new IOException("No se pudo guardar el archivo: " + ex.getMessage(), ex);
        }
    }

    /**
     * Elimina un archivo del sistema de archivos
     * @param fileName Nombre del archivo a eliminar (path relativo)
     */
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn(" Intento de eliminar archivo con nombre vacío");
            return;
        }
        
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            log.info(" Intentando eliminar archivo: {}", filePath.toAbsolutePath());
            
            if (Files.exists(filePath)) {
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    log.info(" Archivo eliminado exitosamente: {}", fileName);
                } else {
                    log.warn(" No se pudo eliminar el archivo: {}", fileName);
                }
            } else {
                log.warn(" Archivo no encontrado para eliminar: {}", filePath.toAbsolutePath());
            }
            
        } catch (IOException ex) {
            log.error(" Error al eliminar archivo '{}': {}", fileName, ex.getMessage());
        }
    }

    /**
     * Genera URL pública para acceder al archivo
     * @param fileName Nombre del archivo (path relativo)
     * @return URL para acceder al archivo
     */
    public String getFileUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            log.debug(" getFileUrl called with null/empty fileName");
            return null;
        }
        
        // Asegurar que el path comience con /uploads/
        String url = fileName.startsWith("/uploads/") ? fileName : "/uploads/" + fileName;
        
        log.debug(" URL generada para '{}': {}", fileName, url);
        return url;
    }

    /**
     * Verifica si un archivo existe
     * @param fileName Nombre del archivo (path relativo)
     * @return true si el archivo existe
     */
    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            boolean exists = Files.exists(filePath);
            log.debug(" Verificando existencia de '{}': {}", fileName, exists);
            return exists;
        } catch (Exception e) {
            log.error(" Error verificando existencia del archivo '{}': {}", fileName, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el tamaño de un archivo en bytes
     * @param fileName Nombre del archivo (path relativo)
     * @return Tamaño en bytes, 0 si hay error
     */
    public long getFileSize(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return 0;
        }
        
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            if (Files.exists(filePath)) {
                long size = Files.size(filePath);
                log.debug(" Tamaño del archivo '{}': {} bytes", fileName, size);
                return size;
            } else {
                log.debug(" Archivo '{}' no encontrado para obtener tamaño", fileName);
                return 0;
            }
        } catch (IOException e) {
            log.error(" Error obteniendo tamaño del archivo '{}': {}", fileName, e.getMessage());
            return 0;
        }
    }

    /**
     * Lista todos los archivos en el directorio de productos
     * @return Lista de nombres de archivos
     */
    public List<String> listProductFiles() {
        try {
            Path productsPath = Paths.get(uploadDir, "products");
            if (!Files.exists(productsPath)) {
                log.info("📁 Directorio de productos no existe: {}", productsPath);
                return Arrays.asList();
            }
            
            return Files.list(productsPath)
                    .filter(Files::isRegularFile)
                    .map(path -> "products/" + path.getFileName().toString())
                    .toList();
                    
        } catch (IOException e) {
            log.error(" Error listando archivos de productos: {}", e.getMessage());
            return Arrays.asList();
        }
    }

    /**
     * Obtiene información detallada de un archivo
     * @param fileName Nombre del archivo (path relativo)
     * @return Map con información del archivo
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
            
        } catch (IOException e) {
            info.put("exists", false);
            info.put("error", e.getMessage());
        }
        
        return info;
    }

    // =================== MÉTODOS PRIVADOS ===================

    /**
     * Crea el directorio de uploads si no existe
     */
    private Path createUploadDirectory() throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info(" Directorio principal de uploads creado: {}", uploadPath);
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
        log.debug("🔍 Validando archivo de imagen...");
        
        // Validar Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn(" Content-Type no permitido: {}", contentType);
            throw new IOException("Tipo de archivo no permitido. Tipos permitidos: " + 
                    String.join(", ", ALLOWED_CONTENT_TYPES));
        }

        // Validar tamaño
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn(" Archivo demasiado grande: {} bytes (máximo: {} bytes)", 
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
            log.warn(" Extensión no permitida: {}", fileExtension);
            throw new IOException("Extensión de archivo no permitida. Extensiones permitidas: " + 
                    String.join(", ", ALLOWED_EXTENSIONS));
        }
        
        // Validar tamaño mínimo (evitar archivos corruptos)
        if (file.getSize() < 100) { // 100 bytes mínimo
            throw new IOException("El archivo es demasiado pequeño, puede estar corrupto");
        }
        
        log.debug(" Archivo validado correctamente");
        log.debug("    Content-Type: {}", contentType);
        log.debug("    Extensión: {}", fileExtension);
        log.debug("    Tamaño: {} KB", file.getSize() / 1024);
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
        
        log.debug(" Nombre único generado: {}", uniqueName);
        return uniqueName;
    }

    /**
     * Limpia archivos antiguos (opcional - para mantenimiento)
     */
    public void cleanupOldFiles(int daysOld) {
        try {
            Path productsPath = Paths.get(uploadDir, "products");
            if (!Files.exists(productsPath)) {
                return;
            }
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            log.info("🧹 Iniciando limpieza de archivos anteriores a: {}", cutoffDate);
            
            Files.list(productsPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        return fileTime.isBefore(cutoffDate);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        log.info(" Archivo antiguo eliminado: {}", path.getFileName());
                    } catch (IOException e) {
                        log.warn(" No se pudo eliminar archivo antiguo: {}", path.getFileName());
                    }
                });
                
        } catch (IOException e) {
            log.error(" Error durante limpieza de archivos antiguos: {}", e.getMessage());
        }
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

    /**
     * Crea directorios adicionales si son necesarios
     */
    public void createDirectoriesIfNeeded() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            
            // Directorios que pueden ser necesarios
            String[] subdirectories = {"products", "categories", "profiles", "temp", "reports"};
            
            for (String subDir : subdirectories) {
                Path subDirPath = uploadPath.resolve(subDir);
                if (!Files.exists(subDirPath)) {
                    Files.createDirectories(subDirPath);
                    log.info("📁 Subdirectorio creado: {}", subDirPath);
                }
            }
            
        } catch (IOException e) {
            log.error(" Error creando subdirectorios: {}", e.getMessage());
        }
    }

    /**
     * Método para inicialización del servicio
     */
    @jakarta.annotation.PostConstruct
    public void initialize() {
        log.info(" Inicializando FileService...");
        log.info(" Directorio de uploads configurado: {}", uploadDir);
        
        try {
            createUploadDirectory();
            createDirectoriesIfNeeded();
            
            // Log de configuración
            log.info(" Configuración del FileService:");
            log.info("    Tamaño máximo de archivo: {} MB", MAX_FILE_SIZE / (1024 * 1024));
            log.info("    Extensiones permitidas: {}", String.join(", ", ALLOWED_EXTENSIONS));
            log.info("    Content-Types permitidos: {}", String.join(", ", ALLOWED_CONTENT_TYPES));
            
            // Verificar salud del storage
            java.util.Map<String, Object> health = getStorageHealth();
            log.info(" Estado del storage: {}", health.get("status"));
            log.info(" Espacio libre: {} MB", health.get("freeSpaceMB"));
            
        } catch (Exception e) {
            log.error(" Error inicializando FileService: {}", e.getMessage());
            throw new RuntimeException("No se pudo inicializar el servicio de archivos", e);
        }
        
        log.info(" FileService inicializado correctamente");
    }
}