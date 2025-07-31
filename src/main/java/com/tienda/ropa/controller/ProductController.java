package com.tienda.ropa.controller;

import com.tienda.ropa.dto.ProductDto;
import com.tienda.ropa.model.Product;
import com.tienda.ropa.service.FileService;
import com.tienda.ropa.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final FileService fileService;

    // =================== ENDPOINTS DE LECTURA ===================

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        try {
            List<Product> products = productService.getAllActiveProducts();
            List<ProductDto> productDtos = products.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al obtener productos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductDto>> getAllProductsAdmin() {
        try {
            List<Product> products = productService.getAllProducts();
            List<ProductDto> productDtos = products.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al obtener todos los productos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            return ResponseEntity.ok(productService.convertToDto(product));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al obtener producto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            List<Product> products = productService.getProductsByCategory(categoryId);
            List<ProductDto> productDtos = products.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al obtener productos por categoría: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> searchProducts(@RequestParam String name) {
        try {
            List<Product> products = productService.searchProductsByName(name);
            List<ProductDto> productDtos = products.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al buscar productos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/price-range")
    public ResponseEntity<List<ProductDto>> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice, 
            @RequestParam BigDecimal maxPrice) {
        try {
            List<Product> products = productService.getProductsByPriceRange(minPrice, maxPrice);
            List<ProductDto> productDtos = products.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al obtener productos por rango de precio: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/in-stock")
    public ResponseEntity<List<ProductDto>> getProductsInStock() {
        try {
            List<Product> products = productService.getProductsInStock();
            List<ProductDto> productDtos = products.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al obtener productos en stock: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductDto>> getLowStockProducts(@RequestParam(defaultValue = "5") Integer minStock) {
        try {
            List<Product> products = productService.getLowStockProducts(minStock);
            List<ProductDto> productDtos = products.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al obtener productos con stock bajo: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<List<ProductDto>> getLatestProducts() {
        try {
            List<Product> products = productService.getLatestProducts();
            List<ProductDto> productDtos = products.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al obtener productos más recientes: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/sorted-by-price")
    public ResponseEntity<List<ProductDto>> getProductsSortedByPrice(@RequestParam(defaultValue = "asc") String order) {
        try {
            List<Product> products = productService.getProductsOrderByPrice(order);
            List<ProductDto> productDtos = products.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al obtener productos ordenados por precio: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =================== ENDPOINTS DE ESCRITURA ===================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductDto productDto) {
        try {
            log.info("Creando producto: {}", productDto.getName());
            Product product = productService.createProduct(productDto);
            ProductDto responseDto = productService.convertToDto(product);
            log.info("Producto creado exitosamente con ID: {}", product.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (RuntimeException e) {
            log.error("Error de validación al crear producto: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al crear producto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDto productDto) {
        try {
            log.info("Actualizando producto con ID: {}", id);
            Product product = productService.updateProduct(id, productDto);
            ProductDto responseDto = productService.convertToDto(product);
            log.info("Producto actualizado exitosamente: {}", product.getName());
            return ResponseEntity.ok(responseDto);
        } catch (RuntimeException e) {
            log.error("Error de validación al actualizar producto {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al actualizar producto {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            log.info("Eliminando producto con ID: {}", id);
            productService.deleteProduct(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Producto eliminado exitosamente");
            response.put("productId", id);
            log.info("Producto eliminado exitosamente: ID {}", id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error al eliminar producto {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al eliminar producto {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleProductStatus(@PathVariable Long id) {
        try {
            log.info("Cambiando estado del producto con ID: {}", id);
            productService.toggleProductStatus(id);
            
            // Obtener el producto actualizado para devolver el nuevo estado
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estado del producto actualizado");
            response.put("productId", id);
            response.put("newStatus", product.getActive());
            response.put("statusText", product.getActive() ? "Activo" : "Inactivo");
            
            log.info("Estado del producto {} cambiado a: {}", id, product.getActive() ? "Activo" : "Inactivo");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error al cambiar estado del producto {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al cambiar estado del producto {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProductStock(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            Integer newStock = request.get("stock");
            if (newStock == null || newStock < 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Stock debe ser un número mayor o igual a 0"));
            }
            
            log.info("Actualizando stock del producto {} a {}", id, newStock);
            Product product = productService.updateStock(id, newStock);
            ProductDto responseDto = productService.convertToDto(product);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock actualizado exitosamente");
            response.put("product", responseDto);
            
            log.info("Stock del producto {} actualizado a {}", id, newStock);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error al actualizar stock del producto {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al actualizar stock del producto {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    // =================== ENDPOINTS DE IMÁGENES ===================

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadProductImage(@PathVariable Long id, 
                                               @RequestParam("file") MultipartFile file) {
        log.info("=== INICIO UPLOAD DE IMAGEN ===");
        log.info("Producto ID: {}", id);
        log.info("Archivo recibido:");
        log.info("  - Nombre: {}", file.getOriginalFilename());
        log.info("  - Tamaño: {} bytes ({} KB)", file.getSize(), file.getSize() / 1024);
        log.info("  - Content-Type: {}", file.getContentType());
        log.info("  - Vacío: {}", file.isEmpty());
        
        try {
            // 1. VERIFICAR QUE EL PRODUCTO EXISTE
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> {
                        log.error("Producto no encontrado con ID: {}", id);
                        return new RuntimeException("Producto no encontrado");
                    });
            
            log.info("Producto encontrado: {}", product.getName());

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

            // 4. GUARDAR ARCHIVO
            String fileName = fileService.saveProductImage(file);
            log.info("Archivo guardado como: {}", fileName);
            
            // 5. GENERAR URL DE ACCESO
            String imageUrl = fileService.getFileUrl(fileName);
            log.info("URL de imagen generada: {}", imageUrl);

            // 6. ACTUALIZAR PRODUCTO CON LA NUEVA IMAGEN
            ProductDto productDto = productService.convertToDto(product);
            productDto.setImageUrl(imageUrl);
            Product updatedProduct = productService.updateProduct(id, productDto);

            log.info("Producto actualizado con imagen: {}", updatedProduct.getImageUrl());

            // 7. RESPUESTA EXITOSA
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageUrl", imageUrl);
            response.put("message", "Imagen subida exitosamente");
            response.put("fileName", fileName);
            response.put("originalName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("productId", id);
            response.put("productName", product.getName());
            
            log.info("=== UPLOAD EXITOSO ===");
            log.info("URL final: {}", imageUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error de validación al subir imagen para producto {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al subir imagen para producto {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProductImage(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            String currentImageUrl = product.getImageUrl();
            if (currentImageUrl == null || currentImageUrl.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("El producto no tiene imagen para eliminar"));
            }
            
            // Extraer nombre del archivo de la URL
            String fileName = currentImageUrl.replace("/uploads/", "");
            
            // Eliminar archivo del sistema
            fileService.deleteFile(fileName);
            
            // Actualizar producto sin imagen
            ProductDto productDto = productService.convertToDto(product);
            productDto.setImageUrl(null);
            productService.updateProduct(id, productDto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Imagen eliminada exitosamente");
            response.put("productId", id);
            
            log.info("Imagen eliminada del producto: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error al eliminar imagen del producto {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al eliminar imagen del producto {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    // =================== ENDPOINTS DE INFORMACIÓN Y ESTADÍSTICAS ===================

    @GetMapping("/{id}/info")
    public ResponseEntity<?> getProductInfo(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            Map<String, Object> info = new HashMap<>();
            info.put("id", product.getId());
            info.put("name", product.getName());
            info.put("description", product.getDescription());
            info.put("price", product.getPrice());
            info.put("stock", product.getStock());
            info.put("active", product.getActive());
            info.put("categoryId", product.getCategory().getId());
            info.put("categoryName", product.getCategory().getName());
            info.put("currentImageUrl", product.getImageUrl());
            info.put("hasImage", product.getImageUrl() != null);
            info.put("createdAt", product.getCreatedAt());
            info.put("updatedAt", product.getUpdatedAt());
            info.put("uploadEndpoint", "/api/products/" + id + "/image");
            
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProducts", productService.getAllProducts().size());
            stats.put("activeProducts", productService.getTotalActiveProducts());
            stats.put("totalStock", productService.getTotalStock());
            stats.put("lowStockProducts", productService.getLowStockProducts(5).size());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas de productos: ", e);
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