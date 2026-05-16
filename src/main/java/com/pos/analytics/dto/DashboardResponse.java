package com.pos.analytics.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private Long totalOrders;

    private BigDecimal totalRevenue;

    private Long totalProducts;

    private Long lowStockProducts;
}