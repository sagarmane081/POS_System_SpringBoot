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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse createOrder(
            CreateOrderRequest request
    ) {

        List<OrderItem> orderItems = new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = new Order();

        for (OrderItemRequest itemRequest : request.getItems()) {

            Product product =
                    productRepository.findById(
                            itemRequest.getProductId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found"
                            )
                    );

            if (product.getStock()
                    < itemRequest.getQuantity()) {

                throw new InsufficientStockException(
                        "Insufficient stock for "
                                + product.getName()
                );
            }

            product.setStock(
                    product.getStock()
                            - itemRequest.getQuantity()
            );

            productRepository.save(product);

            OrderItem orderItem =
                    OrderItem.builder()
                            .product(product)
                            .quantity(itemRequest.getQuantity())
                            .price(product.getPrice())
                            .order(order)
                            .build();

            orderItems.add(orderItem);

            totalAmount =
                    totalAmount.add(
                            product.getPrice().multiply(
                                    BigDecimal.valueOf(
                                            itemRequest.getQuantity()
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

        return OrderMapper.toResponse(savedOrder);
    }
}