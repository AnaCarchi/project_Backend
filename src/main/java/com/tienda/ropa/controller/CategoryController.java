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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        try {
            Category category = categoryService.createCategory(categoryDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(categoryService.convertToDto(category));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadCategoryImage(@PathVariable Long id, 
                                               @RequestParam("file") MultipartFile file) {
        try {
            Category category = categoryService.getCategoryById(id)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

            String fileName = fileService.saveFile(file);
            String imageUrl = fileService.getFileUrl(fileName);

            CategoryDto categoryDto = categoryService.convertToDto(category);
            categoryDto.setImageUrl(imageUrl);
            categoryService.updateCategory(id, categoryDto);

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
    public ResponseEntity<?> updateCategory(@PathVariable Long id, 
                                          @Valid @RequestBody CategoryDto categoryDto) {
        try {
            Category category = categoryService.updateCategory(id, categoryDto);
            return ResponseEntity.ok(categoryService.convertToDto(category));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(Map.of("message", "Categoría eliminada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        return Map.of("error", true, "message", message);
    }
}
