package com.pos.product.service;

import com.pos.product.dto.ProductRequest;
import com.pos.product.dto.ProductResponse;
import org.springframework.data.domain.Page;

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
}