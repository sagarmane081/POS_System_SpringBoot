package com.pos.product.dto;

import com.pos.product.enums.ProductStatus;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;

    private String name;

    private String sku;

    private String description;

    private BigDecimal mrp;

    private BigDecimal sellingPrice;

    private Integer stock;

    private String brand;

    private String color;

    private String image;

    private ProductStatus status;

    private Long categoryId;

    private String categoryName;
}