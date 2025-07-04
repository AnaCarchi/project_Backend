package com.tienda.ropa.service;

import com.tienda.ropa.dto.CategoryDto;
import com.tienda.ropa.model.Category;
import com.tienda.ropa.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductCategoryService productCategoryService;

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findAllActiveCategories();
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    @Transactional
    public Category createCategory(CategoryDto categoryDto) {
        if (categoryRepository.existsByName(categoryDto.getName())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        }

        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setImageUrl(categoryDto.getImageUrl());
        category.setActive(true);

        Category savedCategory = categoryRepository.save(category);
        log.info("Categoría creada: {}", savedCategory.getName());
        
        return savedCategory;
    }

    @Transactional
    public Category updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // Verificar si el nombre ya existe en otra categoría
        if (!category.getName().equals(categoryDto.getName()) && 
            categoryRepository.existsByName(categoryDto.getName())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        }

        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setImageUrl(categoryDto.getImageUrl());
        
        if (categoryDto.getActive() != null) {
            category.setActive(categoryDto.getActive());
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Categoría actualizada: {}", updatedCategory.getName());
        
        return updatedCategory;
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // Verificar si tiene productos asociados
        Long productCount = productCategoryService.getProductCountByCategory(id);
        if (productCount > 0) {
            throw new RuntimeException("No se puede eliminar la categoría porque tiene productos asociados");
        }

        // Limpiar relaciones N:M antes de eliminar
        productCategoryService.cleanupCategoryRelations(id);
        
        categoryRepository.delete(category);
        log.info("Categoría eliminada: {}", category.getName());
    }

    @Transactional
    public void toggleCategoryStatus(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        category.setActive(!category.getActive());
        categoryRepository.save(category);
        log.info("Categoría {} {}", category.getName(), category.getActive() ? "activada" : "desactivada");
    }

    @Transactional(readOnly = true)
    public List<Category> searchCategoriesByName(String name) {
        return categoryRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public Long getProductCountByCategory(Long categoryId) {
        return productCategoryService.getProductCountByCategory(categoryId);
    }

    // Convertir Category a CategoryDto
    public CategoryDto convertToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setImageUrl(category.getImageUrl());
        dto.setActive(category.getActive());
        dto.setProductCount(getProductCountByCategory(category.getId()));
        return dto;
    }
}