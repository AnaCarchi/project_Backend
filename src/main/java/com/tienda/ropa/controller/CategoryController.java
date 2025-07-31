package com.tienda.ropa.controller;

import com.tienda.ropa.dto.CategoryDto;
import com.tienda.ropa.model.Category;
import com.tienda.ropa.service.CategoryService;
import com.tienda.ropa.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;
    private final FileService fileService;

    // =================== ENDPOINTS DE LECTURA ===================

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        try {
            List<Category> categories = categoryService.getAllActiveCategories();
            List<CategoryDto> categoryDtos = categories.stream()
                    .map(categoryService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(categoryDtos);
        } catch (Exception e) {
            log.error("Error al obtener categorías: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryDto>> getAllCategoriesAdmin() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            List<CategoryDto> categoryDtos = categories.stream()
                    .map(categoryService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(categoryDtos);
        } catch (Exception e) {
            log.error("Error al obtener todas las categorías: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        try {
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            return ResponseEntity.ok(categoryService.convertToDto(category));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al obtener categoría: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<CategoryDto>> searchCategories(@RequestParam String name) {
        try {
            List<Category> categories = categoryService.searchCategoriesByName(name);
            List<CategoryDto> categoryDtos = categories.stream()
                    .map(categoryService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(categoryDtos);
        } catch (Exception e) {
            log.error("Error al buscar categorías: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/product-count")
    public ResponseEntity<Map<String, Object>> getCategoryProductCount(@PathVariable Long id) {
        try {
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            
            Long productCount = categoryService.getProductCountByCategory(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("categoryId", id);
            response.put("categoryName", category.getName());
            response.put("productCount", productCount);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error al obtener conteo de productos de categoría: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =================== ENDPOINTS DE ESCRITURA ===================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        try {
            log.info("Creando categoría: {}", categoryDto.getName());
            Category category = categoryService.createCategory(categoryDto);
            CategoryDto responseDto = categoryService.convertToDto(category);
            log.info("Categoría creada exitosamente con ID: {}", category.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (RuntimeException e) {
            log.error("Error de validación al crear categoría: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al crear categoría: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, 
                                          @Valid @RequestBody CategoryDto categoryDto) {
        try {
            log.info("Actualizando categoría con ID: {}", id);
            Category category = categoryService.updateCategory(id, categoryDto);
            CategoryDto responseDto = categoryService.convertToDto(category);
            log.info("Categoría actualizada exitosamente: {}", category.getName());
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException e) {
            log.error("Error de validación al actualizar categoría {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al actualizar categoría {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            log.info("Eliminando categoría con ID: {}", id);
            categoryService.deleteCategory(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Categoría eliminada exitosamente");
            response.put("categoryId", id);
            log.info("Categoría eliminada exitosamente: ID {}", id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error al eliminar categoría {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al eliminar categoría {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleCategoryStatus(@PathVariable Long id) {
        try {
            log.info("Cambiando estado de la categoría con ID: {}", id);
            categoryService.toggleCategoryStatus(id);
            
            // Obtener la categoría actualizada para devolver el nuevo estado
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estado de la categoría actualizado");
            response.put("categoryId", id);
            response.put("newStatus", category.getActive());
            response.put("statusText", category.getActive() ? "Activa" : "Inactiva");
            
            log.info("Estado de la categoría {} cambiado a: {}", id, category.getActive() ? "Activa" : "Inactiva");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error al cambiar estado de la categoría {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al cambiar estado de la categoría {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    // =================== ENDPOINTS DE IMÁGENES ===================

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadCategoryImage(@PathVariable Long id, 
                                               @RequestParam("file") MultipartFile file) {
        log.info("=== INICIO UPLOAD DE IMAGEN DE CATEGORÍA ===");
        log.info("Categoría ID: {}", id);
        log.info("Archivo recibido:");
        log.info("  - Nombre: {}", file.getOriginalFilename());
        log.info("  - Tamaño: {} bytes ({} KB)", file.getSize(), file.getSize() / 1024);
        log.info("  - Content-Type: {}", file.getContentType());
        log.info("  - Vacío: {}", file.isEmpty());
        
        try {
            // 1. VERIFICAR QUE LA CATEGORÍA EXISTE
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> {
                        log.error("Categoría no encontrada con ID: {}", id);
                        return new RuntimeException("Categoría no encontrada");
                    });
            
            log.info("Categoría encontrada: {}", category.getName());

            // 2. VALIDACIONES BÁSICAS
            if (file.isEmpty()) {
                log.warn("Archivo vacío recibido");
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("El archivo está vacío"));
            }

            if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                log.warn("Archivo demasiado grande: {} bytes", file.getSize());
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("El archivo es demasiado grande (máximo 10MB)"));
            }

            // 3. VALIDAR CONTENT TYPE
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Content-Type inválido: {}", contentType);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("El archivo debe ser una imagen"));
            }

            log.info("Validaciones pasadas, procediendo a guardar archivo...");

            // 4. GUARDAR ARCHIVO USANDO EL MÉTODO CORRECTO
            String fileName = fileService.saveCategoryImage(file);
            log.info("Archivo guardado como: {}", fileName);
            
            // 5. GENERAR URL DE ACCESO
            String imageUrl = fileService.getFileUrl(fileName);
            log.info("URL de imagen generada: {}", imageUrl);

            // 6. ACTUALIZAR CATEGORÍA CON LA NUEVA IMAGEN
            CategoryDto categoryDto = categoryService.convertToDto(category);
            categoryDto.setImageUrl(imageUrl);
            Category updatedCategory = categoryService.updateCategory(id, categoryDto);

            log.info("Categoría actualizada con imagen: {}", updatedCategory.getImageUrl());

            // 7. RESPUESTA EXITOSA
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageUrl", imageUrl);
            response.put("message", "Imagen subida exitosamente");
            response.put("fileName", fileName);
            response.put("originalName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("categoryId", id);
            response.put("categoryName", category.getName());
            
            log.info("=== UPLOAD DE CATEGORÍA EXITOSO ===");
            log.info("URL final: {}", imageUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error de validación al subir imagen para categoría {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al subir imagen para categoría {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategoryImage(@PathVariable Long id) {
        try {
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            
            String currentImageUrl = category.getImageUrl();
            if (currentImageUrl == null || currentImageUrl.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("La categoría no tiene imagen para eliminar"));
            }
            
            // Extraer nombre del archivo de la URL
            String fileName = currentImageUrl.replace("/uploads/", "");
            
            // Eliminar archivo del sistema
            fileService.deleteFile(fileName);
            
            // Actualizar categoría sin imagen
            CategoryDto categoryDto = categoryService.convertToDto(category);
            categoryDto.setImageUrl(null);
            categoryService.updateCategory(id, categoryDto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Imagen eliminada exitosamente");
            response.put("categoryId", id);
            
            log.info("Imagen eliminada de la categoría: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error al eliminar imagen de la categoría {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al eliminar imagen de la categoría {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    // =================== ENDPOINTS DE INFORMACIÓN Y ESTADÍSTICAS ===================

    @GetMapping("/{id}/info")
    public ResponseEntity<?> getCategoryInfo(@PathVariable Long id) {
        try {
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            
            Map<String, Object> info = new HashMap<>();
            info.put("id", category.getId());
            info.put("name", category.getName());
            info.put("description", category.getDescription());
            info.put("active", category.getActive());
            info.put("currentImageUrl", category.getImageUrl());
            info.put("hasImage", category.getImageUrl() != null);
            info.put("productCount", categoryService.getProductCountByCategory(id));
            info.put("createdAt", category.getCreatedAt());
            info.put("updatedAt", category.getUpdatedAt());
            info.put("uploadEndpoint", "/api/categories/" + id + "/image");
            
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCategoryStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCategories", categoryService.getAllCategories().size());
            stats.put("activeCategories", categoryService.getAllActiveCategories().size());
            
            // Estadísticas adicionales
            List<Category> allCategories = categoryService.getAllCategories();
            long categoriesWithProducts = allCategories.stream()
                    .mapToLong(cat -> categoryService.getProductCountByCategory(cat.getId()))
                    .filter(count -> count > 0)
                    .count();
            
            stats.put("categoriesWithProducts", categoriesWithProducts);
            stats.put("emptyCategoriesCount", allCategories.size() - categoriesWithProducts);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas de categorías: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =================== MÉTODOS HELPER ===================

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return error;
    }
}