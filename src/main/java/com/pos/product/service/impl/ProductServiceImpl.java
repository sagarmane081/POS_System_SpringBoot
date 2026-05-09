package com.pos.product.service.impl;

import com.pos.product.dto.ProductRequest;
import com.pos.product.dto.ProductResponse;
import com.pos.product.entity.Product;
import com.pos.product.enums.ProductStatus;
import com.pos.product.mapper.ProductMapper;
import com.pos.product.repository.ProductRepository;
import com.pos.product.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl
        implements ProductService {

    private final ProductRepository productRepository;

    private final ProductMapper productMapper;

    @Override
    public Page<ProductResponse> getAllProducts(
            int page,
            int size
    ) {

        Pageable pageable =
                PageRequest.of(page, size);

        return productRepository
                .findAll(pageable)
                .map(productMapper::toResponse);
    }

    @Override
    public ProductResponse getProductById(
            Long id
    ) {

        Product product =
                productRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(
            ProductRequest request
    ) {

        Product product =
                productMapper.toEntity(request);

        product.setStatus(ProductStatus.ACTIVE);

        Product savedProduct =
                productRepository.save(product);

        log.info(
                "Product created: {}",
                savedProduct.getName()
        );

        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(
            Long id,
            ProductRequest request
    ) {

        Product product =
                productRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );

        product.setName(request.getName());
        product.setDescription(
                request.getDescription()
        );
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        Product updatedProduct =
                productRepository.save(product);

        return productMapper.toResponse(
                updatedProduct
        );
    }

    @Override
    @Transactional
    public void deleteProduct(
            Long id
    ) {

        Product product =
                productRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );

        productRepository.delete(product);
    }
}