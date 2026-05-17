package com.pos.product.service;

import com.pos.product.dto.*;
import com.pos.product.entity.Product;
import com.pos.product.enums.ProductStatus;
import com.pos.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductSeedService {

    private final ProductRepository productRepository;

    public String seedProducts() {

        RestTemplate restTemplate = new RestTemplate();

        DummyProductResponse response =
                restTemplate.getForObject(
                        "https://dummyjson.com/products?limit=400",
                        DummyProductResponse.class);

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