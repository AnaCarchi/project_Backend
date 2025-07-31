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

    // ENDPOINT MEJORADO PARA UPLOAD DE IMAGENES
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
            String fileName = fileService.saveFile(file);
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

    // ENDPOINT DE PRUEBA PARA VERIFICAR CONECTIVIDAD
    @GetMapping("/test-upload")
    public ResponseEntity<Map<String, Object>> testUpload() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Endpoint de upload funcionando");
        response.put("uploadEndpoint", "/api/products/{id}/image");
        response.put("method", "POST");
        response.put("contentType", "multipart/form-data");
        response.put("parameterName", "file");
        return ResponseEntity.ok(response);
    }

    // ENDPOINT PARA VERIFICAR UN PRODUCTO ESPECÍFICO
    @GetMapping("/{id}/info")
    public ResponseEntity<?> getProductInfo(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
            Map<String, Object> info = new HashMap<>();
            info.put("id", product.getId());
            info.put("name", product.getName());
            info.put("currentImageUrl", product.getImageUrl());
            info.put("hasImage", product.getImageUrl() != null);
            info.put("uploadEndpoint", "/api/products/" + id + "/image");
            
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // Método helper para crear respuestas de error consistentes
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return error;
    }

    // RESTO DE ENDPOINTS ORIGINALES...
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
}