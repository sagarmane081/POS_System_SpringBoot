package com.pos.analytics.service;

import com.pos.analytics.dto.DashboardResponse;
import com.pos.analytics.dto.TopProductResponse;
import com.pos.order.repository.OrderItemRepository;
import com.pos.order.repository.OrderRepository;
import com.pos.product.dto.ProductResponse;
import com.pos.product.entity.Product;
import com.pos.product.mapper.ProductMapper;
import com.pos.product.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void getDashboardData_shouldAggregateCountsAndRevenue() {

        when(orderRepository.count()).thenReturn(42L);
        when(productRepository.count()).thenReturn(15L);
        when(productRepository.countByStockLessThan(10)).thenReturn(3L);
        when(orderRepository.sumTotalAmount()).thenReturn(BigDecimal.valueOf(1234.56));

        DashboardResponse response = analyticsService.getDashboardData();

        assertThat(response.getTotalOrders()).isEqualTo(42L);
        assertThat(response.getTotalProducts()).isEqualTo(15L);
        assertThat(response.getLowStockProducts()).isEqualTo(3L);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(1234.56));
    }

    @Test
    void getLowStockProducts_shouldReturnMappedProducts() {

        Product product = Product.builder().id(1L).name("Coke").stock(2).build();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").build();

        when(productRepository.findByStockLessThan(10)).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        assertThat(analyticsService.getLowStockProducts()).containsExactly(response);
    }

    @Test
    void getTopSellingProducts_shouldMapRawRowsToResponse() {

        Object[] row = new Object[] {
                1L, "Coke", 25L, BigDecimal.valueOf(250)
        };

        List<Object[]> rows = java.util.Collections.singletonList(row);

        when(orderItemRepository.findTopSellingProducts()).thenReturn(rows);

        List<TopProductResponse> result = analyticsService.getTopSellingProducts();

        assertThat(result).hasSize(1);

        TopProductResponse response = result.get(0);
        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("Coke");
        assertThat(response.getTotalQuantitySold()).isEqualTo(25);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(250));
    }
}
