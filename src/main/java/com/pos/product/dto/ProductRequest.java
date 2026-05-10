package com.pos.product.dto;

import jakarta.validation.constraints.*;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String sku;

    private String description;

    @NotNull
    @Positive
    private BigDecimal mrp;

    @NotNull
    @Positive
    private BigDecimal sellingPrice;

    @NotNull
    @PositiveOrZero
    private Integer stock;

    private String brand;

    private String color;

    private String image;

    @NotNull
    private Long categoryId;
}