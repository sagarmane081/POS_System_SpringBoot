package com.pos.analytics.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse {

    private Long productId;

    private String productName;

    private Integer totalQuantitySold;

    private BigDecimal totalRevenue;
}