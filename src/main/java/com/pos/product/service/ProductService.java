package com.pos.product.service;

import com.pos.product.dto.ProductRequest;
import com.pos.product.dto.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {

    Page<ProductResponse> getAllProducts(
            int page,
            int size
    );

    ProductResponse getProductById(
            Long id
    );

    ProductResponse createProduct(
            ProductRequest request
    );

    ProductResponse updateProduct(
            Long id,
            ProductRequest request
    );

    void deleteProduct(
            Long id
    );

    List<ProductResponse> searchProducts(String keyword);

    List<ProductResponse> getProductsByCategory(Long categoryId);

    List<ProductResponse> getLowStockProducts();

    ProductResponse increaseStock(
            Long productId,
            Integer quantity
    );

    ProductResponse decreaseStock(
            Long productId,
            Integer quantity
    );
}