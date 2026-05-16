package com.pos.analytics.service;

import com.pos.analytics.dto.DashboardResponse;
import com.pos.order.repository.OrderItemRepository;
import com.pos.order.repository.OrderRepository;
import com.pos.product.dto.ProductResponse;
import com.pos.product.mapper.ProductMapper;
import com.pos.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pos.analytics.dto.TopProductResponse;
import com.pos.order.entity.OrderItem;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl
        implements AnalyticsService {

    private final OrderRepository orderRepository;

    private final ProductRepository productRepository;

    private final ProductMapper productMapper;

    private final OrderItemRepository orderItemRepository;

    @Override
    public DashboardResponse getDashboardData() {

        Long totalOrders =
                orderRepository.count();

        Long totalProducts =
                productRepository.count();

        Long lowStockProducts =
                productRepository.countByStockLessThan(10);

        BigDecimal totalRevenue =
                orderRepository.findAll()
                        .stream()
                        .map(order -> order.getTotalAmount())
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return DashboardResponse.builder()
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .totalRevenue(totalRevenue)
                .build();
    }

    @Override
    public List<ProductResponse> getLowStockProducts() {

        return productRepository
                .findByStockLessThan(10)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<TopProductResponse> getTopSellingProducts() {

        List<OrderItem> orderItems =
                orderItemRepository.findAll();

        Map<Long, TopProductResponse> productMap =
                new HashMap<>();

        for (OrderItem item : orderItems) {

            Long productId =
                    item.getProduct().getId();

            TopProductResponse existing =
                    productMap.get(productId);

            BigDecimal itemRevenue =
                    item.getPrice().multiply(
                            BigDecimal.valueOf(
                                    item.getQuantity()
                            )
                    );

            if (existing == null) {

                productMap.put(
                        productId,
                        TopProductResponse.builder()
                                .productId(productId)
                                .productName(
                                        item.getProduct().getName()
                                )
                                .totalQuantitySold(
                                        item.getQuantity()
                                )
                                .totalRevenue(itemRevenue)
                                .build()
                );

            } else {

                existing.setTotalQuantitySold(
                        existing.getTotalQuantitySold()
                                + item.getQuantity()
                );

                existing.setTotalRevenue(
                        existing.getTotalRevenue()
                                .add(itemRevenue)
                );
            }
        }

        return productMap.values()
                .stream()
                .sorted((a, b) ->
                        b.getTotalQuantitySold()
                                .compareTo(
                                        a.getTotalQuantitySold()
                                )
                )
                .toList();
    }
}