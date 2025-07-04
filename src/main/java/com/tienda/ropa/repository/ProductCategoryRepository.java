package com.tienda.ropa.repository;

import com.tienda.ropa.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    
    // Encontrar todas las relaciones de un producto
    @Query("SELECT pc FROM ProductCategory pc WHERE pc.product.id = :productId")
    List<ProductCategory> findByProductId(@Param("productId") Long productId);
    
    // Encontrar todas las relaciones de una categoría
    @Query("SELECT pc FROM ProductCategory pc WHERE pc.category.id = :categoryId")
    List<ProductCategory> findByCategoryId(@Param("categoryId") Long categoryId);
    
    // Encontrar relación específica entre producto y categoría
    @Query("SELECT pc FROM ProductCategory pc WHERE pc.product.id = :productId AND pc.category.id = :categoryId")
    Optional<ProductCategory> findByProductIdAndCategoryId(@Param("productId") Long productId, @Param("categoryId") Long categoryId);
    
    // Verificar si existe relación entre producto y categoría
    @Query("SELECT COUNT(pc) > 0 FROM ProductCategory pc WHERE pc.product.id = :productId AND pc.category.id = :categoryId")
    Boolean existsByProductIdAndCategoryId(@Param("productId") Long productId, @Param("categoryId") Long categoryId);
    
    // Encontrar categoría primaria de un producto
    @Query("SELECT pc FROM ProductCategory pc WHERE pc.product.id = :productId AND pc.isPrimary = true")
    Optional<ProductCategory> findPrimaryByProductId(@Param("productId") Long productId);
    
    // Encontrar categorías secundarias de un producto
    @Query("SELECT pc FROM ProductCategory pc WHERE pc.product.id = :productId AND pc.isPrimary = false")
    List<ProductCategory> findSecondaryByProductId(@Param("productId") Long productId);
    
    // Contar productos por categoría
    @Query("SELECT COUNT(pc) FROM ProductCategory pc WHERE pc.category.id = :categoryId")
    Long countProductsByCategoryId(@Param("categoryId") Long categoryId);
    
    // Eliminar todas las relaciones de un producto
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductCategory pc WHERE pc.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
    
    // Eliminar todas las relaciones de una categoría
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductCategory pc WHERE pc.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
    
    // Eliminar relación específica
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductCategory pc WHERE pc.product.id = :productId AND pc.category.id = :categoryId")
    void deleteByProductIdAndCategoryId(@Param("productId") Long productId, @Param("categoryId") Long categoryId);
    
    // Productos más populares por número de categorías
    @Query("SELECT pc.product.id, COUNT(pc) as categoryCount FROM ProductCategory pc GROUP BY pc.product.id ORDER BY categoryCount DESC")
    List<Object[]> findProductsWithMostCategories();
    
    // Categorías más usadas
    @Query("SELECT pc.category.id, COUNT(pc) as productCount FROM ProductCategory pc GROUP BY pc.category.id ORDER BY productCount DESC")
    List<Object[]> findMostUsedCategories();
}