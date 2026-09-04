package com.pos.category.service.impl;

import com.pos.category.dto.CategoryRequest;
import com.pos.category.dto.CategoryResponse;
import com.pos.category.entity.Category;
import com.pos.category.mapper.CategoryMapper;
import com.pos.category.repository.CategoryRepository;
import com.pos.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void getAllCategories_shouldReturnMappedList() {

        Category category = Category.builder().id(1L).name("Beverages").build();
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Beverages").build();

        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertThat(result).containsExactly(response);
    }

    @Test
    void getCategoryById_shouldReturnMapped_whenFound() {

        Category category = Category.builder().id(1L).name("Beverages").build();
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Beverages").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        CategoryResponse result = categoryService.getCategoryById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getCategoryById_shouldThrowResourceNotFoundException_whenMissing() {

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void createCategory_shouldSaveAndReturnMapped() {

        CategoryRequest request = CategoryRequest.builder()
                .name("Beverages")
                .description("Drinks")
                .build();

        Category entity = Category.builder().name("Beverages").description("Drinks").build();
        Category saved = Category.builder().id(1L).name("Beverages").description("Drinks").build();
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Beverages").description("Drinks").build();

        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(response);

        CategoryResponse result = categoryService.createCategory(request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void updateCategory_shouldUpdateFieldsAndReturnMapped() {

        CategoryRequest request = CategoryRequest.builder()
                .name("Updated")
                .description("Updated description")
                .build();

        Category existing = Category.builder().id(1L).name("Old").description("Old description").build();
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Updated").description("Updated description").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);
        when(categoryMapper.toResponse(existing)).thenReturn(response);

        CategoryResponse result = categoryService.updateCategory(1L, request);

        assertThat(existing.getName()).isEqualTo("Updated");
        assertThat(existing.getDescription()).isEqualTo("Updated description");
        assertThat(result).isEqualTo(response);
    }

    @Test
    void updateCategory_shouldThrowResourceNotFoundException_whenMissing() {

        CategoryRequest request = CategoryRequest.builder().name("Updated").build();

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_shouldDelete_whenFound() {

        Category existing = Category.builder().id(1L).name("Beverages").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(existing);
    }

    @Test
    void deleteCategory_shouldThrowResourceNotFoundException_whenMissing() {

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(categoryRepository, never()).delete(any());
    }
}
