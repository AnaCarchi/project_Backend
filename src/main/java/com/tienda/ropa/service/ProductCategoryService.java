package com.tienda.ropa.service;

import com.tienda.ropa.model.Category;
import com.tienda.ropa.model.Product;
import com.tienda.ropa.model.ProductCategory;
import com.tienda.ropa.repository.CategoryRepository;
import com.tienda.ropa.repository.ProductCategoryRepository;
import com.tienda.ropa.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductCategory addCategoryToProduct(Long productId, Long categoryId, Boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // Verificar si ya existe la relación
        if (productCategoryRepository.existsByProductIdAndCategoryId(productId, categoryId)) {
            throw new RuntimeException("El producto ya está asociado a esta categoría");
        }

        // Si es categoría primaria, remover la primaria anterior
        if (isPrimary) {
            productCategoryRepository.findPrimaryByProductId(productId)
                    .ifPresent(pc -> {
                        pc.setIsPrimary(false);
                        productCategoryRepository.save(pc);
                    });
        }

        ProductCategory productCategory = new ProductCategory(product, category, isPrimary);
        ProductCategory saved = productCategoryRepository.save(productCategory);
        
        log.info("Categoría '{}' agregada al producto '{}' como {}", 
                category.getName(), product.getName(), isPrimary ? "primaria" : "secundaria");
        
        return saved;
    }

    @Transactional
    public void removeCategoryFromProduct(Long productId, Long categoryId) {
        if (!productCategoryRepository.existsByProductIdAndCategoryId(productId, categoryId)) {
            throw new RuntimeException("No existe asociación entre el producto y la categoría");
        }
        
        productCategoryRepository.deleteByProductIdAndCategoryId(productId, categoryId);
        log.info("Relación eliminada entre producto {} y categoría {}", productId, categoryId);
    }

    @Transactional
    public void updateProductCategories(Long productId, Set<Long> categoryIds, Long primaryCategoryId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Eliminar todas las relaciones existentes
        productCategoryRepository.deleteByProductId(productId);

        // Agregar nuevas relaciones
        for (Long categoryId : categoryIds) {
            Boolean isPrimary = categoryId.equals(primaryCategoryId);
            addCategoryToProduct(productId, categoryId, isPrimary);
        }

        log.info("Categorías actualizadas para el producto: {}", product.getName());
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesByProduct(Long productId) {
        return productCategoryRepository.findByProductId(productId)
                .stream()
                .map(ProductCategory::getCategory)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        return productCategoryRepository.findByCategoryId(categoryId)
                .stream()
                .map(ProductCategory::getProduct)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Category getPrimaryCategoryByProduct(Long productId) {
        return productCategoryRepository.findPrimaryByProductId(productId)
                .map(ProductCategory::getCategory)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Category> getSecondaryCategoriesByProduct(Long productId) {
        return productCategoryRepository.findSecondaryByProductId(productId)
                .stream()
                .map(ProductCategory::getCategory)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long getProductCountByCategory(Long categoryId) {
        return productCategoryRepository.countProductsByCategoryId(categoryId);
    }

    @Transactional
    public void setPrimaryCategory(Long productId, Long categoryId) {
        // Verificar que existe la relación
        ProductCategory productCategory = productCategoryRepository
                .findByProductIdAndCategoryId(productId, categoryId)
                .orElseThrow(() -> new RuntimeException("No existe asociación entre el producto y la categoría"));

        // Remover categoría primaria anterior
        productCategoryRepository.findPrimaryByProductId(productId)
                .ifPresent(pc -> {
                    pc.setIsPrimary(false);
                    productCategoryRepository.save(pc);
                });

        // Establecer nueva categoría primaria
        productCategory.setIsPrimary(true);
        productCategoryRepository.save(productCategory);
        
        log.info("Categoría {} establecida como primaria para producto {}", categoryId, productId);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getProductsWithMostCategories() {
        return productCategoryRepository.findProductsWithMostCategories();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getMostUsedCategories() {
        return productCategoryRepository.findMostUsedCategories();
    }

    @Transactional
    public void cleanupCategoryRelations(Long categoryId) {
        // Limpiar todas las relaciones cuando se elimina una categoría
        productCategoryRepository.deleteByCategoryId(categoryId);
        log.info("Relaciones de categoría {} eliminadas", categoryId);
    }

    @Transactional
    public void cleanupProductRelations(Long productId) {
        // Limpiar todas las relaciones cuando se elimina un producto
        productCategoryRepository.deleteByProductId(productId);
        log.info("Relaciones de producto {} eliminadas", productId);
    }
}
