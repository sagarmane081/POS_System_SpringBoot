package com.pos.order.repository;

import com.pos.order.entity.Order;
import com.pos.order.entity.OrderItem;
import com.pos.order.enums.OrderStatus;
import com.pos.product.entity.Product;
import com.pos.product.enums.ProductStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private EntityManager entityManager;

    private Product persistProduct(String name, String sku) {

        Product product = Product.builder()
                .name(name)
                .sku(sku)
                .mrp(BigDecimal.TEN)
                .sellingPrice(BigDecimal.valueOf(9))
                .stock(100)
                .status(ProductStatus.ACTIVE)
                .build();

        entityManager.persist(product);
        return product;
    }

    private Order persistOrder(BigDecimal totalAmount) {

        Order order = Order.builder()
                .totalAmount(totalAmount)
                .status(OrderStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persist(order);
        return order;
    }

    private void persistOrderItem(Order order, Product product, int quantity, BigDecimal price) {

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .price(price)
                .build();

        entityManager.persist(item);
    }

    @Test
    void findTopSellingProducts_shouldAggregateAndOrderByQuantityDescending() {

        Product cola = persistProduct("Cola", "SKU-1");
        Product chips = persistProduct("Chips", "SKU-2");

        Order order1 = persistOrder(BigDecimal.valueOf(90));
        Order order2 = persistOrder(BigDecimal.valueOf(45));

        persistOrderItem(order1, cola, 5, BigDecimal.valueOf(10));
        persistOrderItem(order2, cola, 3, BigDecimal.valueOf(10));
        persistOrderItem(order1, chips, 2, BigDecimal.valueOf(5));

        entityManager.flush();

        List<Object[]> results = orderItemRepository.findTopSellingProducts();

        assertThat(results).hasSize(2);

        Object[] top = results.get(0);
        assertThat(top[1]).isEqualTo("Cola");
        assertThat(((Number) top[2]).intValue()).isEqualTo(8);
        assertThat((BigDecimal) top[3]).isEqualByComparingTo(BigDecimal.valueOf(80));

        Object[] second = results.get(1);
        assertThat(second[1]).isEqualTo("Chips");
        assertThat(((Number) second[2]).intValue()).isEqualTo(2);
        assertThat((BigDecimal) second[3]).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void findTopSellingProducts_shouldReturnEmptyList_whenNoOrderItemsExist() {

        assertThat(orderItemRepository.findTopSellingProducts()).isEmpty();
    }
}
