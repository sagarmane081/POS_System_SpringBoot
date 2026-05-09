package com.pos.order.controller;

import com.pos.common.response.ApiResponse;
import com.pos.order.dto.CreateOrderRequest;
import com.pos.order.dto.OrderResponse;
import com.pos.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>>
    createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {

        OrderResponse response =
                orderService.createOrder(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Order created successfully",
                        response
                )
        );
    }
}