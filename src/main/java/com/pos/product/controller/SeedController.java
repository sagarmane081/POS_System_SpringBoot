package com.pos.product.controller;

import com.pos.common.response.ApiResponse;
import com.pos.product.service.ProductSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedController {

    private final ProductSeedService productSeedService;

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Void>> seedProducts() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        productSeedService.seedProducts(),
                        null
                )
        );
    }
}
