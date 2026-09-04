package com.pos.order.service;

import com.pos.common.exception.InsufficientStockException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.order.dto.CreateOrderRequest;
import com.pos.order.dto.OrderItemRequest;
import com.pos.order.dto.OrderResponse;
import com.pos.order.entity.Order;
import com.pos.order.mapper.OrderMapper;
import com.pos.order.repository.OrderRepository;
import com.pos.product.entity.Product;
import com.pos.product.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Product product(Long id, String name, BigDecimal price, int stock) {

        return Product.builder()
                .id(id)
                .name(name)
                .sellingPrice(price)
                .stock(stock)
                .build();
    }

    private OrderItemRequest itemRequest(Long productId, int quantity) {

        OrderItemRequest request = new OrderItemRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }

    @Test
    void createOrder_shouldDeductStockAndCalculateTotal_forSingleItem() {

        Product product = product(1L, "Coke", BigDecimal.valueOf(10), 20);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest(1L, 3)));

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(OrderResponse.builder().totalAmount(BigDecimal.valueOf(30)).build());

        OrderResponse response = orderService.createOrder(request);

        assertThat(product.getStock()).isEqualTo(17);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(30));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(savedOrder.getItems()).hasSize(1);
    }

    @Test
    void createOrder_shouldDeductStockAndCalculateTotal_forMultipleItems() {

        Product cola = product(1L, "Cola", BigDecimal.valueOf(10), 20);
        Product chips = product(2L, "Chips", BigDecimal.valueOf(5), 10);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest(1L, 2), itemRequest(2L, 4)));

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(cola));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(chips));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(OrderResponse.builder().totalAmount(BigDecimal.valueOf(40)).build());

        OrderResponse response = orderService.createOrder(request);

        assertThat(cola.getStock()).isEqualTo(18);
        assertThat(chips.getStock()).isEqualTo(6);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(40));
    }

    @Test
    void createOrder_shouldThrowResourceNotFoundException_whenProductMissing() {

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest(99L, 1)));

        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_shouldThrowInsufficientStockException_whenStockTooLow() {

        Product product = product(1L, "Coke", BigDecimal.valueOf(10), 2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest(1L, 5)));

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Coke");

        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void getAllOrders_shouldReturnMappedList() {

        Order order = new Order();
        OrderResponse response = OrderResponse.builder().id(1L).build();

        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        assertThat(orderService.getAllOrders()).containsExactly(response);
    }

    @Test
    void getOrderById_shouldReturnMapped_whenFound() {

        Order order = new Order();
        OrderResponse response = OrderResponse.builder().id(1L).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        assertThat(orderService.getOrderById(1L)).isEqualTo(response);
    }

    @Test
    void getOrderById_shouldThrowResourceNotFoundException_whenMissing() {

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");
    }
}
