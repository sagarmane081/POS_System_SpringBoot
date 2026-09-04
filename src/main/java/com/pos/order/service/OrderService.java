package com.pos.order.service;

import com.pos.common.exception.InsufficientStockException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.order.dto.*;
import com.pos.order.entity.Order;
import com.pos.order.entity.OrderItem;
import com.pos.order.enums.OrderStatus;
import com.pos.order.mapper.OrderMapper;
import com.pos.order.repository.OrderRepository;
import com.pos.product.entity.Product;
import com.pos.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(
            CreateOrderRequest request
    ) {

        List<OrderItem> orderItems = new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = new Order();

        // Merge repeated productIds into a single line so the same product
        // is locked/checked/decremented once, with quantities combined,
        // instead of once per occurrence in the request.
        Map<Long, Integer> quantityByProductId = request.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::getProductId,
                        OrderItemRequest::getQuantity,
                        Integer::sum,
                        LinkedHashMap::new
                ));

        for (Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {

            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product =
                    productRepository.findByIdForUpdate(
                            productId
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found"
                            )
                    );

            if (product.getStock()
                    < quantity) {

                throw new InsufficientStockException(
                        "Insufficient stock for "
                                + product.getName()
                );
            }

            product.setStock(
                    product.getStock()
                            - quantity
            );

            productRepository.save(product);

            OrderItem orderItem =
                    OrderItem.builder()
                            .product(product)
                            .quantity(quantity)
                            .price(product.getSellingPrice())
                            .order(order)
                            .build();

            orderItems.add(orderItem);

            totalAmount =
                    totalAmount.add(
                            product.getSellingPrice().multiply(
                                    BigDecimal.valueOf(
                                            quantity
                                    )
                            )
                    );
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder =
                orderRepository.save(order);

        log.info(
                "Order created successfully: {}",
                savedOrder.getId()
        );

        return orderMapper.toResponse(savedOrder);
    }

    public List<OrderResponse> getAllOrders() {

        return orderRepository.findAllWithItems()
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public OrderResponse getOrderById(Long orderId) {

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found"
                        )
                );
        return orderMapper.toResponse(order);
    }
}