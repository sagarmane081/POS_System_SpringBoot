package com.pos.category.mapper;

import com.pos.category.dto.*;
import com.pos.category.entity.Category;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toEntity(
            CategoryRequest request
    );

    CategoryResponse toResponse(
            Category category
    );
}