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
import java.util.List;

import com.pos.analytics.dto.TopProductResponse;

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
                orderRepository.sumTotalAmount();

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

        return orderItemRepository
                .findTopSellingProducts()
                .stream()
                .map(row -> TopProductResponse.builder()
                        .productId((Long) row[0])
                        .productName((String) row[1])
                        .totalQuantitySold(((Number) row[2]).intValue())
                        .totalRevenue((BigDecimal) row[3])
                        .build())
                .toList();
    }
}