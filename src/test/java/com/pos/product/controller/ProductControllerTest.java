package com.pos.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.auth.security.CustomUserDetailsService;
import com.pos.auth.security.JwtProvider;
import com.pos.common.exception.InsufficientStockException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.inventory.dto.StockUpdateRequest;
import com.pos.product.dto.ProductRequest;
import com.pos.product.dto.ProductResponse;
import com.pos.product.enums.ProductStatus;
import com.pos.product.service.ProductService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private ProductRequest validRequest() {

        return ProductRequest.builder()
                .name("Coke")
                .sku("SKU-1")
                .mrp(BigDecimal.TEN)
                .sellingPrice(BigDecimal.valueOf(9))
                .stock(20)
                .categoryId(1L)
                .build();
    }

    private ProductResponse response() {

        return ProductResponse.builder()
                .id(1L)
                .name("Coke")
                .sku("SKU-1")
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void createProduct_shouldReturn200_whenRequestValid() throws Exception {

        when(productService.createProduct(any())).thenReturn(response());

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Coke"));
    }

    @Test
    void createProduct_shouldReturn400_whenRequiredFieldsMissing() throws Exception {

        ProductRequest request = ProductRequest.builder().build();

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_shouldReturn404_whenCategoryMissing() throws Exception {

        when(productService.createProduct(any()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllProducts_shouldReturn200WithPagedData() throws Exception {

        when(productService.getAllProducts(0, 10))
                .thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Coke"));
    }

    @Test
    void getAllProducts_shouldUseProvidedPageAndSize() throws Exception {

        when(productService.getAllProducts(2, 5)).thenReturn(Page.empty());

        mockMvc.perform(get("/api/products").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(productService).getAllProducts(2, 5);
    }

    @Test
    void getProductById_shouldReturn200_whenFound() throws Exception {

        when(productService.getProductById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getProductById_shouldReturn404_whenMissing() throws Exception {

        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/products/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_shouldReturn200_whenFound() throws Exception {

        when(productService.updateProduct(eq(1L), any())).thenReturn(response());

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Coke"));
    }

    @Test
    void updateProduct_shouldReturn404_whenMissing() throws Exception {

        when(productService.updateProduct(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(put("/api/products/{id}", 99L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_shouldReturn200_whenFound() throws Exception {

        mockMvc.perform(delete("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).deleteProduct(1L);
    }

    @Test
    void deleteProduct_shouldReturn404_whenMissing() throws Exception {

        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Product not found"))
                .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductsByCategory_shouldReturn200WithList() throws Exception {

        when(productService.getProductsByCategory(1L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/products/category/{categoryId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Coke"));
    }

    @Test
    void getLowStockProducts_shouldReturn200WithList() throws Exception {

        when(productService.getLowStockProducts()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Coke"));
    }

    @Test
    void increaseStock_shouldReturn200_whenQuantityValid() throws Exception {

        StockUpdateRequest request = new StockUpdateRequest();
        request.setQuantity(5);

        when(productService.increaseStock(1L, 5)).thenReturn(response());

        mockMvc.perform(patch("/api/products/{id}/increase-stock", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void increaseStock_shouldReturn400_whenQuantityInvalid() throws Exception {

        StockUpdateRequest request = new StockUpdateRequest();
        request.setQuantity(0);

        mockMvc.perform(patch("/api/products/{id}/increase-stock", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void decreaseStock_shouldReturn200_whenSufficientStock() throws Exception {

        StockUpdateRequest request = new StockUpdateRequest();
        request.setQuantity(5);

        when(productService.decreaseStock(1L, 5)).thenReturn(response());

        mockMvc.perform(patch("/api/products/{id}/decrease-stock", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void decreaseStock_shouldReturn409_whenInsufficientStock() throws Exception {

        StockUpdateRequest request = new StockUpdateRequest();
        request.setQuantity(999);

        when(productService.decreaseStock(1L, 999))
                .thenThrow(new InsufficientStockException("Insufficient stock for Coke"));

        mockMvc.perform(patch("/api/products/{id}/decrease-stock", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void searchProducts_shouldUseProvidedQueryParams() throws Exception {

        when(productService.getProducts("cola", 1, 20, "sellingPrice")).thenReturn(Page.empty());

        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "cola")
                        .param("page", "1")
                        .param("size", "20")
                        .param("sortBy", "sellingPrice"))
                .andExpect(status().isOk());

        verify(productService).getProducts("cola", 1, 20, "sellingPrice");
    }

    @Test
    void searchProducts_shouldUseDefaults_whenParamsOmitted() throws Exception {

        when(productService.getProducts(anyString(), eq(0), eq(5), anyString())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isOk());

        verify(productService).getProducts("", 0, 5, "name");
    }
}
