package com.pos.order.mapper;

import com.pos.order.dto.OrderItemResponse;
import com.pos.order.dto.OrderResponse;
import com.pos.order.entity.Order;
import com.pos.order.entity.OrderItem;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    @Mapping(target = "productName", source = "product.name")
    OrderItemResponse toItemResponse(OrderItem item);
}
