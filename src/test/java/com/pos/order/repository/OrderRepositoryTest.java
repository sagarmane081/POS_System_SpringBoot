package com.pos.order.repository;

import com.pos.order.entity.Order;
import com.pos.order.entity.OrderItem;
import com.pos.order.enums.OrderStatus;
import com.pos.payment.entity.Payment;
import com.pos.payment.enums.PaymentMethod;
import com.pos.payment.enums.PaymentStatus;
import com.pos.product.entity.Product;
import com.pos.product.enums.ProductStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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

    private Order persistOrderWithItem(Product product, BigDecimal total) {

        Order order = order(total);
        entityManager.persist(order);

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(2)
                .price(product.getSellingPrice())
                .build();

        entityManager.persist(item);
        return order;
    }

    @Test
    void findAllWithItems_shouldEagerLoadItemsAndProducts() {

        Product cola = persistProduct("Cola", "SKU-1");
        Product chips = persistProduct("Chips", "SKU-2");

        persistOrderWithItem(cola, BigDecimal.valueOf(18));
        persistOrderWithItem(chips, BigDecimal.valueOf(20));

        entityManager.flush();
        entityManager.clear();

        List<Order> orders = orderRepository.findAllWithItems();

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getItems()).hasSize(1);
        assertThat(orders.get(0).getItems().get(0).getProduct().getName()).isNotBlank();
        assertThat(orders.get(1).getItems().get(0).getProduct().getName()).isNotBlank();
    }

    @Test
    void findAllWithItems_shouldExecuteExactlyOneQuery_regardlessOfOrderCount() {

        Product cola = persistProduct("Cola", "SKU-1");
        Product chips = persistProduct("Chips", "SKU-2");
        Product juice = persistProduct("Juice", "SKU-3");

        persistOrderWithItem(cola, BigDecimal.valueOf(18));
        persistOrderWithItem(chips, BigDecimal.valueOf(20));
        persistOrderWithItem(juice, BigDecimal.valueOf(15));

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();

        statistics.clear();

        List<Order> orders = orderRepository.findAllWithItems();

        // Touch every association; if this were still lazy/N+1 it would
        // trigger additional queries here and the assertion below would fail.
        orders.forEach(o -> o.getItems()
                .forEach(i -> assertThat(i.getProduct().getName()).isNotBlank()));

        assertThat(orders).hasSize(3);
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(1);
    }

    @Test
    void findAllWithItems_shouldNotEagerlyLoadPayment_evenWhenPaymentExists() {

        Product cola = persistProduct("Cola", "SKU-1");
        Order order = persistOrderWithItem(cola, BigDecimal.valueOf(18));

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn-1")
                .paidAt(LocalDateTime.now())
                .build();

        entityManager.persist(payment);
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();

        statistics.clear();

        List<Order> orders = orderRepository.findAllWithItems();

        assertThat(orders).hasSize(1);
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(1);
    }

    @Test
    void findByIdWithItems_shouldEagerLoadItemsAndProduct() {

        Product cola = persistProduct("Cola", "SKU-1");
        Order order = persistOrderWithItem(cola, BigDecimal.valueOf(18));

        entityManager.flush();
        entityManager.clear();

        Optional<Order> found = orderRepository.findByIdWithItems(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().get(0).getProduct().getName()).isEqualTo("Cola");
    }

    @Test
    void findByIdWithItems_shouldReturnEmpty_whenMissing() {

        assertThat(orderRepository.findByIdWithItems(999L)).isEmpty();
    }
}
