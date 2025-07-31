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
            log.warn("Producto no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al obtener producto por ID: ", e);
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductDto productDto) {
        try {
            log.info(" Creando producto: {}", productDto.getName());
            log.debug(" Datos del producto: precio={}, stock={}, categoryId={}", 
                     productDto.getPrice(), productDto.getStock(), productDto.getCategoryId());
            
            Product product = productService.createProduct(productDto);
            ProductDto responseDto = productService.convertToDto(product);
            
            log.info(" Producto creado exitosamente con ID: {}", product.getId());
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

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadProductImage(@PathVariable Long id, 
                                               @RequestParam("file") MultipartFile file) {
        try {
            log.info("Recibiendo upload de imagen para producto {}", id);
            log.info("   Archivo: {}", file.getOriginalFilename());
            log.info("   Tamaño: {} bytes", file.getSize());
            log.info("   Tipo: {}", file.getContentType());
            
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // Validar archivo
            if (file.isEmpty()) {
                log.warn("Archivo vacío recibido");
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("El archivo está vacío"));
            }

            // Guardar archivo
            String fileName = fileService.saveFile(file);
            log.info("Archivo guardado como: {}", fileName);
            
            // Generar URL de acceso
            String imageUrl = fileService.getFileUrl(fileName);
            log.info("URL de imagen generada: {}", imageUrl);

            // Actualizar producto con la nueva imagen
            ProductDto productDto = productService.convertToDto(product);
            productDto.setImageUrl(imageUrl);
            Product updatedProduct = productService.updateProduct(id, productDto);

            log.info("Producto actualizado con imagen: {}", updatedProduct.getImageUrl());

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "Imagen subida exitosamente");
            response.put("fileName", fileName);
            response.put("originalName", file.getOriginalFilename());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error de validación al subir imagen para producto {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al subir imagen para producto {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno al subir imagen: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, 
                                         @Valid @RequestBody ProductDto productDto) {
        try {
            log.info("Actualizando producto con ID: {}", id);
            log.debug("Nuevos datos: precio={}, stock={}, categoryId={}", 
                     productDto.getPrice(), productDto.getStock(), productDto.getCategoryId());
            
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
            
            // Verificar que el producto existe antes de eliminar
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            String productName = product.getName();
            String imageUrl = product.getImageUrl();
            
            // Eliminar producto
            productService.deleteProduct(id);
            
            // Intentar eliminar imagen asociada si existe
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    String fileName = imageUrl.replace("/uploads/", "");
                    fileService.deleteFile(fileName);
                    log.info("Imagen eliminada: {}", fileName);
                } catch (Exception imageError) {
                    log.warn("No se pudo eliminar la imagen: {}", imageError.getMessage());
                }
            }
            
            log.info("Producto eliminado exitosamente: {}", productName);
            return ResponseEntity.ok(Map.of("message", "Producto eliminado exitosamente"));
            
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

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductStats() {
        try {
            log.info("Obteniendo estadísticas de productos");
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProducts", productService.getTotalActiveProducts());
            stats.put("totalStock", productService.getTotalStock());
            stats.put("lowStockProducts", productService.getLowStockProducts(10).size());
            
            // Estadísticas adicionales
            List<Product> allProducts = productService.getAllProducts();
            long inactiveProducts = allProducts.stream()
                    .filter(p -> !p.getActive())
                    .count();
            
            stats.put("inactiveProducts", inactiveProducts);
            stats.put("totalProductsIncludingInactive", allProducts.size());
            
            log.info("Estadísticas generadas: {} productos activos, {} stock total", 
                    stats.get("totalProducts"), stats.get("totalStock"));
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener estadísticas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleProductStatus(@PathVariable Long id) {
        try {
            log.info("Cambiando estado del producto con ID: {}", id);
            
            productService.toggleProductStatus(id);
            
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            log.info("Estado cambiado: {} ahora está {}", 
                    product.getName(), product.getActive() ? "activo" : "inactivo");
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Estado actualizado exitosamente");
            response.put("active", product.getActive());
            response.put("productName", product.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("❌ Error al cambiar estado del producto {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error interno al cambiar estado del producto {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProductStock(@PathVariable Long id, 
                                               @RequestBody Map<String, Integer> request) {
        try {
            Integer newStock = request.get("stock");
            if (newStock == null || newStock < 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Stock debe ser un número mayor o igual a 0"));
            }
            
            log.info("Actualizando stock del producto {}: nuevo stock = {}", id, newStock);
            
            Product product = productService.updateStock(id, newStock);
            
            log.info("Stock actualizado: {} ahora tiene {} unidades", 
                    product.getName(), product.getStock());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Stock actualizado exitosamente");
            response.put("productName", product.getName());
            response.put("oldStock", request.get("oldStock"));
            response.put("newStock", product.getStock());
            
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

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductDto>> getLowStockProducts(@RequestParam(defaultValue = "10") Integer threshold) {
        try {
            log.info("⚠️ Obteniendo productos con stock bajo (umbral: {})", threshold);
            
            List<Product> lowStockProducts = productService.getLowStockProducts(threshold);
            List<ProductDto> productDtos = lowStockProducts.stream()
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            
            log.info("📊 Encontrados {} productos con stock bajo", productDtos.size());
            return ResponseEntity.ok(productDtos);
            
        } catch (Exception e) {
            log.error(" Error al obtener productos con stock bajo: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<List<ProductDto>> getLatestProducts(@RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<Product> latestProducts = productService.getLatestProducts();
            List<ProductDto> productDtos = latestProducts.stream()
                    .limit(limit)
                    .map(productService::convertToDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(productDtos);
        } catch (Exception e) {
            log.error("Error al obtener productos más recientes: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-price-range")
    public ResponseEntity<List<ProductDto>> getProductsByPriceRange(
            @RequestParam java.math.BigDecimal minPrice,
            @RequestParam java.math.BigDecimal maxPrice) {
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

    // Método helper para crear respuestas de error consistentes
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return error;
    }
}