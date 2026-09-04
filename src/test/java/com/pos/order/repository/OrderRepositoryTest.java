package com.pos.order.repository;

import com.pos.order.entity.Order;
import com.pos.order.enums.OrderStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order order(BigDecimal totalAmount) {

        return Order.builder()
                .totalAmount(totalAmount)
                .status(OrderStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void sumTotalAmount_shouldReturnZero_whenNoOrdersExist() {

        assertThat(orderRepository.sumTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sumTotalAmount_shouldReturnSumOfAllOrderTotals() {

        orderRepository.save(order(BigDecimal.valueOf(100)));
        orderRepository.save(order(BigDecimal.valueOf(250.50)));

        assertThat(orderRepository.sumTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(350.50));
    }
}
