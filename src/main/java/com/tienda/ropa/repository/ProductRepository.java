package com.tienda.ropa.repository;

import com.tienda.ropa.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p WHERE p.active = true")
    List<Product> findAllActiveProducts();
    
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.active = true")
    List<Product> findByCategoryIdAndActiveTrue(@Param("categoryId") Long categoryId);
    
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.active = true")
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(@Param("name") String name);
    
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.active = true")
    List<Product> findByPriceBetweenAndActiveTrue(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.active = true")
    List<Product> findByStockGreaterThanZeroAndActiveTrue();
    
    @Query("SELECT p FROM Product p WHERE p.stock <= :minStock AND p.active = true")
    List<Product> findByLowStock(@Param("minStock") Integer minStock);
    
    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.createdAt DESC")
    List<Product> findAllActiveProductsOrderByCreatedAtDesc();
    
    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.price ASC")
    List<Product> findAllActiveProductsOrderByPriceAsc();
    
    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.price DESC")
    List<Product> findAllActiveProductsOrderByPriceDesc();
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    Long countActiveProducts();
    
    @Query("SELECT SUM(p.stock) FROM Product p WHERE p.active = true")
    Long getTotalStock();
}
