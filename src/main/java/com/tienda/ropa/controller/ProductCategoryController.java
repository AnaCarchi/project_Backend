package com.tienda.ropa.controller;

import com.tienda.ropa.model.Category;
import com.tienda.ropa.model.Product;
import com.tienda.ropa.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/product-categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @PostMapping("/product/{productId}/category/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addCategoryToProduct(@PathVariable Long productId, 
                                                 @PathVariable Long categoryId,
                                                 @RequestParam(defaultValue = "false") Boolean isPrimary) {
        try {
            productCategoryService.addCategoryToProduct(productId, categoryId, isPrimary);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Categoría agregada al producto exitosamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error agregando categoría a producto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @DeleteMapping("/product/{productId}/category/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeCategoryFromProduct(@PathVariable Long productId, 
                                                      @PathVariable Long categoryId) {
        try {
            productCategoryService.removeCategoryFromProduct(productId, categoryId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Categoría removida del producto exitosamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error removiendo categoría de producto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @PutMapping("/product/{productId}/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProductCategories(@PathVariable Long productId,
                                                    @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Set<Long> categoryIds = Set.copyOf((List<Long>) request.get("categoryIds"));
            Long primaryCategoryId = Long.valueOf(request.get("primaryCategoryId").toString());
            
            productCategoryService.updateProductCategories(productId, categoryIds, primaryCategoryId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Categorías del producto actualizadas exitosamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando categorías del producto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @GetMapping("/product/{productId}/categories")
    public ResponseEntity<List<Category>> getCategoriesByProduct(@PathVariable Long productId) {
        try {
            List<Category> categories = productCategoryService.getCategoriesByProduct(productId);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error obteniendo categorías del producto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/category/{categoryId}/products")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            List<Product> products = productCategoryService.getProductsByCategory(categoryId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error obteniendo productos de la categoría: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/product/{productId}/primary-category")
    public ResponseEntity<Category> getPrimaryCategoryByProduct(@PathVariable Long productId) {
        try {
            Category category = productCategoryService.getPrimaryCategoryByProduct(productId);
            if (category != null) {
                return ResponseEntity.ok(category);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error obteniendo categoría primaria del producto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/product/{productId}/secondary-categories")
    public ResponseEntity<List<Category>> getSecondaryCategoriesByProduct(@PathVariable Long productId) {
        try {
            List<Category> categories = productCategoryService.getSecondaryCategoriesByProduct(productId);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error obteniendo categorías secundarias del producto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/product/{productId}/primary-category/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setPrimaryCategory(@PathVariable Long productId, 
                                               @PathVariable Long categoryId) {
        try {
            productCategoryService.setPrimaryCategory(productId, categoryId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Categoría primaria establecida exitosamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error estableciendo categoría primaria: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error interno del servidor"));
        }
    }

    @GetMapping("/stats/products-most-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Object[]>> getProductsWithMostCategories() {
        try {
            List<Object[]> stats = productCategoryService.getProductsWithMostCategories();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de productos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats/most-used-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Object[]>> getMostUsedCategories() {
        try {
            List<Object[]> stats = productCategoryService.getMostUsedCategories();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de categorías: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/category/{categoryId}/product-count")
    public ResponseEntity<Map<String, Long>> getProductCountByCategory(@PathVariable Long categoryId) {
        try {
            Long count = productCategoryService.getProductCountByCategory(categoryId);
            Map<String, Long> response = new HashMap<>();
            response.put("productCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error obteniendo conteo de productos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        return error;
    }
}