package com.pos.analytics.controller;

import com.pos.analytics.dto.DashboardResponse;
import com.pos.analytics.dto.TopProductResponse;
import com.pos.analytics.service.AnalyticsService;
import com.pos.common.response.ApiResponse;
import com.pos.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Dashboard fetched successfully",
                        analyticsService.getDashboardData()
                )
        );
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Low stock products fetched successfully",
                        analyticsService.getLowStockProducts()
                )
        );
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Top selling products fetched successfully",
                        analyticsService.getTopSellingProducts()
                )
        );
    }
}