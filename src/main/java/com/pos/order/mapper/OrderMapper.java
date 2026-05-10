package com.pos.order.mapper;

import com.pos.order.dto.*;
import com.pos.order.entity.Order;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {

        return OrderResponse.builder()
                .id(order.getId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(
                        order.getItems()
                                .stream()
                                .map(item ->
                                        OrderItemResponse.builder()
                                                .productName(
                                                        item.getProduct().getName()
                                                )
                                                .quantity(item.getQuantity())
                                                .price(item.getPrice())
                                                .build()
                                )
                                .collect(Collectors.toList())
                )
                .build();
    }
}