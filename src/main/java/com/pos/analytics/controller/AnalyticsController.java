package com.pos.analytics.controller;

import com.pos.analytics.service.AnalyticsService;
import com.pos.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<?>> getDashboard() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Dashboard fetched successfully",
                        analyticsService.getDashboardData()
                )
        );
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<?>> getLowStockProducts() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Low stock products fetched successfully",
                        analyticsService.getLowStockProducts()
                )
        );
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<?>> getTopProducts() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Top selling products fetched successfully",
                        analyticsService.getTopSellingProducts()
                )
        );
    }
}