package com.pos.category.controller;

import com.pos.category.dto.*;
import com.pos.category.service.CategoryService;
import com.pos.common.response.ApiResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService
            categoryService;

    @GetMapping
    public ResponseEntity<
            ApiResponse<?>
            > getAllCategories() {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Categories fetched successfully",
                        categoryService
                                .getAllCategories()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<
            ApiResponse<?>
            > getCategoryById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Category fetched successfully",
                        categoryService
                                .getCategoryById(id)
                )
        );
    }

    @PostMapping
    public ResponseEntity<
            ApiResponse<?>
            > createCategory(
            @Valid
            @RequestBody
            CategoryRequest request
    ) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Category created successfully",
                        categoryService
                                .createCategory(request)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<
            ApiResponse<?>
            > updateCategory(
            @PathVariable Long id,

            @Valid
            @RequestBody
            CategoryRequest request
    ) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Category updated successfully",
                        categoryService
                                .updateCategory(
                                        id,
                                        request
                                )
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<
            ApiResponse<?>
            > deleteCategory(
            @PathVariable Long id
    ) {

        categoryService.deleteCategory(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Category deleted successfully",
                        null
                )
        );
    }
}