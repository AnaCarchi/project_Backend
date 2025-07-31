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
     * Por defecto usa el subdirectorio 'products'
     */
    public String saveFile(MultipartFile file) throws IOException {
        return saveFile(file, "products");
    }

    /**
     * Guarda un archivo en el sistema de archivos con subdirectorio específico
     */
    public String saveFile(MultipartFile file, String subdirectory) throws IOException {
        log.info("=== INICIANDO GUARDADO DE ARCHIVO ===");
        log.info("Subdirectorio: {}", subdirectory);
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
        
        // Crear subdirectorio específico
        Path subDirectoryPath = uploadPath.resolve(subdirectory);
        if (!Files.exists(subDirectoryPath)) {
            Files.createDirectories(subDirectoryPath);
            log.info("Subdirectorio '{}' creado: {}", subdirectory, subDirectoryPath);
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
            Path targetLocation = subDirectoryPath.resolve(uniqueFileName);
            log.info("Guardando en: {}", targetLocation.toAbsolutePath());
            
            // Copiar archivo
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Verificar que el archivo se guardó correctamente
            if (Files.exists(targetLocation)) {
                long savedFileSize = Files.size(targetLocation);
                log.info("=== ARCHIVO GUARDADO EXITOSAMENTE ===");
                log.info("  - Ubicación: {}", targetLocation.toAbsolutePath());
                log.info("  - Tamaño verificado: {} bytes", savedFileSize);
                log.info("  - URL relativa: /uploads/{}/{}", subdirectory, uniqueFileName);
                log.info("  - URL completa: http://localhost:8080/uploads/{}/{}", subdirectory, uniqueFileName);
                
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
            String relativePath = subdirectory + "/" + uniqueFileName;
            log.info("Path relativo retornado: {}", relativePath);
            return relativePath;
            
        } catch (IOException ex) {
            log.error("Error al guardar archivo '{}': {}", originalFilename, ex.getMessage());
            throw new IOException("No se pudo guardar el archivo: " + ex.getMessage(), ex);
        }
    }

    /**
     * Método específico para guardar imágenes de productos
     */
    public String saveProductImage(MultipartFile file) throws IOException {
        return saveFile(file, "products");
    }

    /**
     * Método específico para guardar imágenes de categorías
     */
    public String saveCategoryImage(MultipartFile file) throws IOException {
        return saveFile(file, "categories");
    }

    /**
     * Método específico para guardar imágenes de perfiles
     */
    public String saveProfileImage(MultipartFile file) throws IOException {
        return saveFile(file, "profiles");
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

    /**
     * Mueve un archivo de un subdirectorio a otro
     */
    public String moveFile(String currentFileName, String fromSubdir, String toSubdir) throws IOException {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path currentPath = uploadPath.resolve(fromSubdir).resolve(currentFileName);
            
            if (!Files.exists(currentPath)) {
                throw new IOException("Archivo origen no existe: " + currentPath);
            }
            
            // Crear directorio destino si no existe
            Path destDir = uploadPath.resolve(toSubdir);
            if (!Files.exists(destDir)) {
                Files.createDirectories(destDir);
            }
            
            Path destPath = destDir.resolve(currentFileName);
            Files.move(currentPath, destPath, StandardCopyOption.REPLACE_EXISTING);
            
            String newRelativePath = toSubdir + "/" + currentFileName;
            log.info("Archivo movido de '{}' a '{}'", fromSubdir + "/" + currentFileName, newRelativePath);
            
            return newRelativePath;
            
        } catch (IOException ex) {
            log.error("Error moviendo archivo de '{}' a '{}': {}", fromSubdir, toSubdir, ex.getMessage());
            throw ex;
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
                } else {
                    log.info("Subdirectorio ya existe: {}", subDirPath);
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
            
            // Contar archivos existentes por subdirectorio
            for (String subDir : subdirectories) {
                Path subDirPath = uploadPath.resolve(subDir);
                if (Files.exists(subDirPath)) {
                    try {
                        long fileCount = Files.list(subDirPath)
                            .filter(Files::isRegularFile)
                            .count();
                        log.info("  - Archivos en {}: {}", subDir, fileCount);
                    } catch (IOException e) {
                        log.warn("  - No se pudo contar archivos en {}: {}", subDir, e.getMessage());
                    }
                }
            }
            
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
                
                // Determinar subdirectorio
                String subdirectory = "unknown";
                if (fileName.startsWith("products/")) subdirectory = "products";
                else if (fileName.startsWith("categories/")) subdirectory = "categories";
                else if (fileName.startsWith("profiles/")) subdirectory = "profiles";
                else if (fileName.startsWith("temp/")) subdirectory = "temp";
                
                info.put("subdirectory", subdirectory);
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
                
                // Conteo de archivos por subdirectorio
                java.util.Map<String, Long> fileCounts = new java.util.HashMap<>();
                String[] subdirectories = {"products", "categories", "profiles", "temp"};
                
                for (String subDir : subdirectories) {
                    Path subDirPath = uploadPath.resolve(subDir);
                    if (Files.exists(subDirPath)) {
                        try {
                            long fileCount = Files.list(subDirPath)
                                .filter(Files::isRegularFile)
                                .count();
                            fileCounts.put(subDir, fileCount);
                        } catch (IOException e) {
                            fileCounts.put(subDir, 0L);
                        }
                    } else {
                        fileCounts.put(subDir, 0L);
                    }
                }
                
                health.put("fileCountsByDirectory", fileCounts);
                
                // Total de archivos
                long totalFiles = fileCounts.values().stream().mapToLong(Long::longValue).sum();
                health.put("totalFiles", totalFiles);
                
                // Estado general
                boolean healthy = Files.isWritable(uploadPath) && 
                                freeSpace > (100 * 1024 * 1024); // 100MB mínimo
                health.put("healthy", healthy);
                health.put("status", healthy ? "UP" : "DOWN");
                
                // Información adicional para debugging
                health.put("subdirectoriesStatus", checkSubdirectoriesStatus(uploadPath, subdirectories));
                
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
     * Verifica el estado de los subdirectorios
     */
    private java.util.Map<String, java.util.Map<String, Object>> checkSubdirectoriesStatus(Path uploadPath, String[] subdirectories) {
        java.util.Map<String, java.util.Map<String, Object>> subdirStatus = new java.util.HashMap<>();
        
        for (String subDir : subdirectories) {
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            Path subDirPath = uploadPath.resolve(subDir);
            
            status.put("exists", Files.exists(subDirPath));
            status.put("writable", Files.exists(subDirPath) && Files.isWritable(subDirPath));
            status.put("readable", Files.exists(subDirPath) && Files.isReadable(subDirPath));
            status.put("path", subDirPath.toString());
            
            try {
                if (Files.exists(subDirPath)) {
                    long fileCount = Files.list(subDirPath)
                        .filter(Files::isRegularFile)
                        .count();
                    status.put("fileCount", fileCount);
                    
                    // Calcular tamaño total del directorio
                    long totalSize = Files.walk(subDirPath)
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .sum();
                    
                    status.put("totalSizeBytes", totalSize);
                    status.put("totalSizeMB", totalSize / (1024 * 1024));
                } else {
                    status.put("fileCount", 0);
                    status.put("totalSizeBytes", 0);
                    status.put("totalSizeMB", 0);
                }
            } catch (IOException e) {
                status.put("error", e.getMessage());
                status.put("fileCount", 0);
                status.put("totalSizeBytes", 0);
                status.put("totalSizeMB", 0);
            }
            
            subdirStatus.put(subDir, status);
        }
        
        return subdirStatus;
    }

    /**
     * Limpia archivos temporales antiguos
     */
    public void cleanupTempFiles() {
        try {
            Path tempPath = Paths.get(uploadDir).resolve("temp");
            if (!Files.exists(tempPath)) {
                return;
            }
            
            log.info("Iniciando limpieza de archivos temporales...");
            
            // Eliminar archivos más antiguos de 24 horas
            long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            
            Files.list(tempPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.debug("Archivo temporal eliminado: {}", path.getFileName());
                    } catch (IOException e) {
                        log.warn("No se pudo eliminar archivo temporal: {}", path.getFileName());
                    }
                });
                
            log.info("Limpieza de archivos temporales completada");
            
        } catch (IOException e) {
            log.error("Error durante limpieza de archivos temporales: {}", e.getMessage());
        }
    }

    /**
     * Obtiene estadísticas de uso de almacenamiento
     */
    public java.util.Map<String, Object> getStorageStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            
            if (!Files.exists(uploadPath)) {
                stats.put("error", "Upload directory does not exist");
                return stats;
            }
            
            String[] subdirectories = {"products", "categories", "profiles", "temp"};
            long totalFiles = 0;
            long totalSize = 0;
            
            java.util.Map<String, java.util.Map<String, Object>> directoryStats = new java.util.HashMap<>();
            
            for (String subDir : subdirectories) {
                Path subDirPath = uploadPath.resolve(subDir);
                java.util.Map<String, Object> dirStats = new java.util.HashMap<>();
                
                if (Files.exists(subDirPath)) {
                    long fileCount = Files.walk(subDirPath)
                        .filter(Files::isRegularFile)
                        .count();
                    
                    long dirSize = Files.walk(subDirPath)
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .sum();
                    
                    dirStats.put("fileCount", fileCount);
                    dirStats.put("sizeBytes", dirSize);
                    dirStats.put("sizeMB", dirSize / (1024 * 1024));
                    
                    totalFiles += fileCount;
                    totalSize += dirSize;
                } else {
                    dirStats.put("fileCount", 0);
                    dirStats.put("sizeBytes", 0);
                    dirStats.put("sizeMB", 0);
                }
                
                directoryStats.put(subDir, dirStats);
            }
            
            stats.put("directoryStatistics", directoryStats);
            stats.put("totalFiles", totalFiles);
            stats.put("totalSizeBytes", totalSize);
            stats.put("totalSizeMB", totalSize / (1024 * 1024));
            stats.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Información del sistema de archivos
            long freeSpace = uploadPath.toFile().getFreeSpace();
            long totalSpace = uploadPath.toFile().getTotalSpace();
            
            stats.put("systemFreeSpaceBytes", freeSpace);
            stats.put("systemTotalSpaceBytes", totalSpace);
            stats.put("systemFreeSpaceMB", freeSpace / (1024 * 1024));
            stats.put("systemTotalSpaceMB", totalSpace / (1024 * 1024));
            stats.put("systemUsagePercent", ((totalSpace - freeSpace) * 100.0) / totalSpace);
            
        } catch (IOException e) {
            stats.put("error", "Error calculating storage statistics: " + e.getMessage());
        }
        
        return stats;
    }
}