package com.pos.product.mapper;

import com.pos.product.dto.*;
import com.pos.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Product toEntity(ProductRequest request);

    ProductResponse toResponse(Product product);
}