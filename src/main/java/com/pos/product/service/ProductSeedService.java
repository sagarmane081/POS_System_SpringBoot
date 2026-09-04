package com.pos.product.service;

import com.pos.product.dto.*;
import com.pos.product.entity.Product;
import com.pos.product.enums.ProductStatus;
import com.pos.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductSeedService {

    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;

    public String seedProducts() {

        DummyProductResponse response;

        try {

            response = restTemplate.getForObject(
                    "https://dummyjson.com/products?limit=400",
                    DummyProductResponse.class);

        } catch (RestClientException ex) {

            throw new IllegalStateException(
                    "Failed to fetch products from seed source", ex
            );
        }

        if (response == null || response.getProducts() == null) {

            throw new IllegalStateException(
                    "Seed source returned no products"
            );
        }

        var products = response.getProducts()
                .stream()
                .map(p -> Product.builder()

                        .name(p.getTitle())

                        .description(p.getDescription())

                        .sellingPrice(
                                BigDecimal.valueOf(p.getPrice()))

                        .mrp(
                                BigDecimal.valueOf(
                                        p.getPrice()+100))

                        .stock(p.getStock())

                        .brand(p.getBrand())

                        .image(p.getThumbnail())

                        .sku(
                                UUID.randomUUID()
                                        .toString()
                                        .substring(0,8))

                        .status(ProductStatus.ACTIVE)

                        .build()

                ).toList();

        productRepository.saveAll(products);

        return "400 products inserted";
    }
}