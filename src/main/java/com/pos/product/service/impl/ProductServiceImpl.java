package com.pos.product.service.impl;

import com.pos.product.dto.ProductRequest;
import com.pos.product.dto.ProductResponse;
import com.pos.product.entity.Product;
import com.pos.product.enums.ProductStatus;
import com.pos.product.mapper.ProductMapper;
import com.pos.product.repository.ProductRepository;
import com.pos.product.service.ProductService;

import com.pos.category.entity.Category;
import com.pos.category.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl
        implements ProductService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

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

        Category category =
                categoryRepository.findById(
                        request.getCategoryId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Category not found"
                        )
                );

        product.setCategory(category);

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

        Category category =
                categoryRepository
                        .findById(
                                request.getCategoryId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found"
                                )
                        );

        product.setName(
                request.getName()
        );

        product.setSku(
                request.getSku()
        );

        product.setDescription(
                request.getDescription()
        );

        product.setMrp(
                request.getMrp()
        );

        product.setSellingPrice(
                request.getSellingPrice()
        );

        product.setStock(
                request.getStock()
        );

        product.setBrand(
                request.getBrand()
        );

        product.setColor(
                request.getColor()
        );

        product.setImage(
                request.getImage()
        );

        product.setCategory(category);

        Product updatedProduct =
                productRepository
                        .save(product);

        log.info(
                "Product updated successfully: {}",
                updatedProduct.getId()
        );

        return productMapper
                .toResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {

        Product product =
                productRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );

        productRepository.delete(product);

        log.info(
                "Product deleted successfully: {}",
                product.getId()
        );
    }

    @Override
    public List<ProductResponse> searchProducts(String keyword) {

        return productRepository
                .findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getProductsByCategory(Long categoryId) {

        return productRepository
                .findByCategoryId(categoryId)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getLowStockProducts() {

        return productRepository
                .findByStockLessThan(10)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public ProductResponse increaseStock(
            Long productId,
            Integer quantity
    ) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        product.setStock(
                product.getStock() + quantity
        );

        Product updatedProduct =
                productRepository.save(product);

        return productMapper.toResponse(updatedProduct);
    }

    @Override
    public ProductResponse decreaseStock(
            Long productId,
            Integer quantity
    ) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        if (product.getStock() < quantity) {
            throw new RuntimeException(
                    "Insufficient stock"
            );
        }

        product.setStock(
                product.getStock() - quantity
        );

        Product updatedProduct =
                productRepository.save(product);

        return productMapper.toResponse(updatedProduct);
    }
}