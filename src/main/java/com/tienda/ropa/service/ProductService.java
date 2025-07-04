package com.tienda.ropa.service;

import com.tienda.ropa.dto.ProductDto;
import com.tienda.ropa.model.Category;
import com.tienda.ropa.model.Product;
import com.tienda.ropa.repository.CategoryRepository;
import com.tienda.ropa.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryService productCategoryService;

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productRepository.findAllActiveProducts();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsInStock() {
        return productRepository.findByStockGreaterThanZeroAndActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(Integer minStock) {
        return productRepository.findByLowStock(minStock);
    }

    @Transactional
    public Product createProduct(ProductDto productDto) {
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setStock(productDto.getStock());
        product.setImageUrl(productDto.getImageUrl());
        product.setCategory(category);
        product.setActive(true);

        Product savedProduct = productRepository.save(product);
        
        // Crear relación N:M con la categoría principal
        productCategoryService.addCategoryToProduct(savedProduct.getId(), category.getId(), true);

        log.info("Producto creado: {} en categoría: {}", product.getName(), category.getName());
        return savedProduct;
    }

    @Transactional
    public Product updateProduct(Long id, ProductDto productDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setStock(productDto.getStock());
        product.setImageUrl(productDto.getImageUrl());
        product.setCategory(category);
        
        if (productDto.getActive() != null) {
            product.setActive(productDto.getActive());
        }

        Product updatedProduct = productRepository.save(product);
        
        // Actualizar relación principal en ProductCategory
        productCategoryService.setPrimaryCategory(id, category.getId());

        log.info("Producto actualizado: {} en categoría: {}", product.getName(), category.getName());
        return updatedProduct;
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Limpiar relaciones N:M antes de eliminar
        productCategoryService.cleanupProductRelations(id);
        
        productRepository.delete(product);
        log.info("Producto eliminado: {}", product.getName());
    }

    @Transactional
    public void toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        product.setActive(!product.getActive());
        productRepository.save(product);
        log.info("Producto {} {}", product.getName(), product.getActive() ? "activado" : "desactivado");
    }

    @Transactional
    public Product updateStock(Long id, Integer newStock) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Integer oldStock = product.getStock();
        product.setStock(newStock);
        Product updatedProduct = productRepository.save(product);
        
        log.info("Stock actualizado para producto {}: {} → {}", product.getName(), oldStock, newStock);
        return updatedProduct;
    }

    @Transactional(readOnly = true)
    public Long getTotalActiveProducts() {
        return productRepository.countActiveProducts();
    }

    @Transactional(readOnly = true)
    public Long getTotalStock() {
        return productRepository.getTotalStock();
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsOrderByPrice(String order) {
        if ("desc".equalsIgnoreCase(order)) {
            return productRepository.findAllActiveProductsOrderByPriceDesc();
        } else {
            return productRepository.findAllActiveProductsOrderByPriceAsc();
        }
    }

    @Transactional(readOnly = true)
    public List<Product> getLatestProducts() {
        return productRepository.findAllActiveProductsOrderByCreatedAtDesc();
    }

    // Convertir Product a ProductDto
    public ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setImageUrl(product.getImageUrl());
        dto.setActive(product.getActive());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getName());
        return dto;
    }
}