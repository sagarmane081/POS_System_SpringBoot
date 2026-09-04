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

import com.pos.common.exception.InsufficientStockException;
import com.pos.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl
        implements ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    private final ProductMapper productMapper;

    @Override
    public Page<ProductResponse> getAllProducts(
            int page,
            int size
    ) {

        Pageable pageable =
                PageRequest.of(
                        clampPage(page),
                        clampSize(size)
                );

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
                                new ResourceNotFoundException(
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
                        new ResourceNotFoundException(
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
                                new ResourceNotFoundException(
                                        "Product not found"
                                )
                        );

        Category category =
                categoryRepository
                        .findById(
                                request.getCategoryId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                )
                        );

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setMrp(request.getMrp());
        product.setSellingPrice(request.getSellingPrice());
        product.setStock(request.getStock());
        product.setBrand(request.getBrand());
        product.setColor(request.getColor());
        product.setImage(request.getImage());

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
                                new ResourceNotFoundException(
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
    public List<ProductResponse> searchProducts(
            String keyword
    ) {

        return productRepository
                .findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getProductsByCategory(
            Long categoryId
    ) {

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
    @Transactional
    public ProductResponse increaseStock(
            Long productId,
            Integer quantity
    ) {

        Product product =
                productRepository
                        .findByIdForUpdate(productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product not found"
                                )
                        );

        product.setStock(
                product.getStock() + quantity
        );

        Product updatedProduct =
                productRepository.save(product);

        return productMapper
                .toResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse decreaseStock(
            Long productId,
            Integer quantity
    ) {

        Product product =
                productRepository
                        .findByIdForUpdate(productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product not found"
                                )
                        );

        if (product.getStock() < quantity) {

            throw new InsufficientStockException(
                    "Insufficient stock for " + product.getName()
            );
        }

        product.setStock(
                product.getStock() - quantity
        );

        Product updatedProduct =
                productRepository.save(product);

        return productMapper
                .toResponse(updatedProduct);
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "sellingPrice", "mrp", "stock",
            "brand", "createdAt", "updatedAt"
    );

    @Override
    public Page<ProductResponse> getProducts(
            String keyword,
            int page,
            int size,
            String sortBy
    ) {

        String sortField =
                ALLOWED_SORT_FIELDS.contains(sortBy)
                        ? sortBy
                        : "name";

        Pageable pageable =
                PageRequest.of(
                        clampPage(page),
                        clampSize(size),
                        Sort.by(sortField)
                );

        Page<Product> products;

        if (keyword == null ||
                keyword.isBlank()) {

            products =
                    productRepository
                            .findAll(pageable);

        } else {

            products =
                    productRepository
                            .findByNameContainingIgnoreCase(
                                    keyword,
                                    pageable
                            );
        }

        return products.map(
                productMapper::toResponse
        );
    }

    private int clampPage(int page) {

        return Math.max(page, 0);
    }

    private int clampSize(int size) {

        if (size < 1) {

            return 1;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }
}