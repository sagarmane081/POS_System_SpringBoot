package com.pos.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.auth.security.CustomUserDetailsService;
import com.pos.auth.security.JwtProvider;
import com.pos.category.dto.CategoryRequest;
import com.pos.category.dto.CategoryResponse;
import com.pos.category.service.CategoryService;
import com.pos.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllCategories_shouldReturn200WithList() throws Exception {

        when(categoryService.getAllCategories())
                .thenReturn(List.of(CategoryResponse.builder().id(1L).name("Beverages").build()));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Beverages"));
    }

    @Test
    void getCategoryById_shouldReturn200_whenFound() throws Exception {

        when(categoryService.getCategoryById(1L))
                .thenReturn(CategoryResponse.builder().id(1L).name("Beverages").build());

        mockMvc.perform(get("/api/categories/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Beverages"));
    }

    @Test
    void getCategoryById_shouldReturn404_whenMissing() throws Exception {

        when(categoryService.getCategoryById(99L))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(get("/api/categories/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    void createCategory_shouldReturn200_whenRequestValid() throws Exception {

        CategoryRequest request = CategoryRequest.builder().name("Beverages").description("Drinks").build();

        when(categoryService.createCategory(any()))
                .thenReturn(CategoryResponse.builder().id(1L).name("Beverages").description("Drinks").build());

        mockMvc.perform(post("/api/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void createCategory_shouldReturn400_whenNameBlank() throws Exception {

        CategoryRequest request = CategoryRequest.builder().name("").build();

        mockMvc.perform(post("/api/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_shouldReturn200_whenFound() throws Exception {

        CategoryRequest request = CategoryRequest.builder().name("Updated").description("Updated desc").build();

        when(categoryService.updateCategory(eq(1L), any()))
                .thenReturn(CategoryResponse.builder().id(1L).name("Updated").description("Updated desc").build());

        mockMvc.perform(put("/api/categories/{id}", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    void updateCategory_shouldReturn404_whenMissing() throws Exception {

        CategoryRequest request = CategoryRequest.builder().name("Updated").build();

        when(categoryService.updateCategory(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(put("/api/categories/{id}", 99L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_shouldReturn200_whenFound() throws Exception {

        mockMvc.perform(delete("/api/categories/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(categoryService).deleteCategory(1L);
    }

    @Test
    void deleteCategory_shouldReturn404_whenMissing() throws Exception {

        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Category not found"))
                .when(categoryService).deleteCategory(99L);

        mockMvc.perform(delete("/api/categories/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}
