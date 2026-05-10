package com.pos.category.service;

import com.pos.category.dto.*;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(
            Long id
    );

    CategoryResponse createCategory(
            CategoryRequest request
    );

    CategoryResponse updateCategory(
            Long id,
            CategoryRequest request
    );

    void deleteCategory(
            Long id
    );
}