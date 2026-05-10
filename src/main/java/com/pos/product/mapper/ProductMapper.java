package com.pos.product.mapper;

import com.pos.product.dto.*;
import com.pos.product.entity.Product;

import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id",
            ignore = true)

    @Mapping(target = "status",
            ignore = true)

    @Mapping(target = "category",
            ignore = true)

    @Mapping(target = "createdAt",
            ignore = true)

    @Mapping(target = "updatedAt",
            ignore = true)

    Product toEntity(ProductRequest request);

    @Mapping(target = "categoryId",
            source = "category.id")

    @Mapping(target = "categoryName",
            source = "category.name")

    ProductResponse toResponse(Product product);
}