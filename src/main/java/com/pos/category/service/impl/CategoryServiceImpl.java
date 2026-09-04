package com.pos.category.service.impl;

import com.pos.category.dto.*;
import com.pos.category.entity.Category;
import com.pos.category.mapper.CategoryMapper;
import com.pos.category.repository.CategoryRepository;
import com.pos.category.service.CategoryService;
import com.pos.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl
        implements CategoryService {

    private final CategoryRepository
            categoryRepository;

    private final CategoryMapper
            categoryMapper;

    @Override
    public List<CategoryResponse>
    getAllCategories() {

        return categoryRepository
                .findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(
            Long id
    ) {

        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                )
                        );

        return categoryMapper
                .toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(
            CategoryRequest request
    ) {

        Category category =
                categoryMapper
                        .toEntity(request);

        Category savedCategory =
                categoryRepository
                        .save(category);

        log.info(
                "Category created: {}",
                savedCategory.getName()
        );

        return categoryMapper
                .toResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(
            Long id,
            CategoryRequest request
    ) {

        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                )
                        );

        category.setName(
                request.getName()
        );

        category.setDescription(
                request.getDescription()
        );

        Category updatedCategory =
                categoryRepository
                        .save(category);

        return categoryMapper
                .toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(
            Long id
    ) {

        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                )
                        );

        categoryRepository.delete(category);

        log.info(
                "Category deleted: {}",
                category.getName()
        );
    }
}