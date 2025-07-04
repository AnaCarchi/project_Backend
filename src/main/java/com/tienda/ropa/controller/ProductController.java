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
            Product product = productService.createProduct(productDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(productService.convertToDto(product));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadProductImage(@PathVariable Long id, 
                                              @RequestParam("file") MultipartFile file) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            String fileName = fileService.saveFile(file);
            String imageUrl = fileService.getFileUrl(fileName);

            ProductDto productDto = productService.convertToDto(product);
            productDto.setImageUrl(imageUrl);
            productService.updateProduct(id, productDto);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "Imagen subida exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error al subir imagen"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, 
                                         @Valid @RequestBody ProductDto productDto) {
        try {
            Product product = productService.updateProduct(id, productDto);
            return ResponseEntity.ok(productService.convertToDto(product));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(Map.of("message", "Producto eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProducts", productService.getTotalActiveProducts());
            stats.put("totalStock", productService.getTotalStock());
            stats.put("lowStockProducts", productService.getLowStockProducts(10).size());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        return Map.of("error", true, "message", message);
    }
}
