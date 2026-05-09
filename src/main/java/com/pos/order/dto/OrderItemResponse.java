package com.pos.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {

    private String productName;
    private Integer quantity;
    private BigDecimal price;
}