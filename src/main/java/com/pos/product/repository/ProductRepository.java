package com.pos.product.repository;

import com.pos.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String keyword);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByStockLessThan(Integer stock);

    Long countByStockLessThan(Integer stock);

    Page<Product> findByNameContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );
}