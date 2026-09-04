package com.pos.product.service.impl;

import com.pos.category.entity.Category;
import com.pos.category.repository.CategoryRepository;
import com.pos.common.exception.InsufficientStockException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.product.dto.ProductRequest;
import com.pos.product.dto.ProductResponse;
import com.pos.product.entity.Product;
import com.pos.product.enums.ProductStatus;
import com.pos.product.mapper.ProductMapper;
import com.pos.product.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct() {

        return Product.builder()
                .id(1L)
                .name("Coke")
                .sku("SKU-1")
                .mrp(BigDecimal.TEN)
                .sellingPrice(BigDecimal.valueOf(9))
                .stock(20)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void getAllProducts_shouldReturnPagedMappedResults() {

        Product product = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").build();
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product)));
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.getAllProducts(0, 10);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getProductById_shouldReturnMapped_whenFound() {

        Product product = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        assertThat(productService.getProductById(1L)).isEqualTo(response);
    }

    @Test
    void getProductById_shouldThrowResourceNotFoundException_whenMissing() {

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void createProduct_shouldSetCategoryAndActiveStatus_thenReturnMapped() {

        ProductRequest request = ProductRequest.builder()
                .name("Coke")
                .sku("SKU-1")
                .mrp(BigDecimal.TEN)
                .sellingPrice(BigDecimal.valueOf(9))
                .stock(20)
                .categoryId(5L)
                .build();

        Product mappedEntity = Product.builder().name("Coke").sku("SKU-1").build();
        Category category = Category.builder().id(5L).name("Drinks").build();
        Product saved = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").build();

        when(productMapper.toEntity(request)).thenReturn(mappedEntity);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(productRepository.save(mappedEntity)).thenReturn(saved);
        when(productMapper.toResponse(saved)).thenReturn(response);

        ProductResponse result = productService.createProduct(request);

        assertThat(mappedEntity.getCategory()).isEqualTo(category);
        assertThat(mappedEntity.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void createProduct_shouldThrowResourceNotFoundException_whenCategoryMissing() {

        ProductRequest request = ProductRequest.builder()
                .name("Coke")
                .categoryId(5L)
                .build();

        when(productMapper.toEntity(request)).thenReturn(new Product());
        when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_shouldUpdateFieldsAndReturnMapped() {

        Product existing = sampleProduct();
        Category category = Category.builder().id(7L).name("Snacks").build();

        ProductRequest request = ProductRequest.builder()
                .name("Pepsi")
                .sku("SKU-2")
                .description("Updated desc")
                .mrp(BigDecimal.valueOf(15))
                .sellingPrice(BigDecimal.valueOf(12))
                .stock(50)
                .brand("BrandX")
                .color("Blue")
                .image("img.png")
                .categoryId(7L)
                .build();

        ProductResponse response = ProductResponse.builder().id(1L).name("Pepsi").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));
        when(productRepository.save(existing)).thenReturn(existing);
        when(productMapper.toResponse(existing)).thenReturn(response);

        ProductResponse result = productService.updateProduct(1L, request);

        assertThat(existing.getName()).isEqualTo("Pepsi");
        assertThat(existing.getSku()).isEqualTo("SKU-2");
        assertThat(existing.getStock()).isEqualTo(50);
        assertThat(existing.getCategory()).isEqualTo(category);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void updateProduct_shouldThrowResourceNotFoundException_whenProductMissing() {

        ProductRequest request = ProductRequest.builder().categoryId(1L).build();

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void updateProduct_shouldThrowResourceNotFoundException_whenCategoryMissing() {

        Product existing = sampleProduct();
        ProductRequest request = ProductRequest.builder().categoryId(7L).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_shouldDelete_whenFound() {

        Product existing = sampleProduct();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(1L);

        verify(productRepository).delete(existing);
    }

    @Test
    void deleteProduct_shouldThrowResourceNotFoundException_whenMissing() {

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void searchProducts_shouldReturnMappedMatches() {

        Product product = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").build();

        when(productRepository.findByNameContainingIgnoreCase("coke")).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        assertThat(productService.searchProducts("coke")).containsExactly(response);
    }

    @Test
    void getProductsByCategory_shouldReturnMapped() {

        Product product = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").build();

        when(productRepository.findByCategoryId(5L)).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        assertThat(productService.getProductsByCategory(5L)).containsExactly(response);
    }

    @Test
    void getLowStockProducts_shouldReturnProductsBelowThreshold() {

        Product product = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").build();

        when(productRepository.findByStockLessThan(10)).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        assertThat(productService.getLowStockProducts()).containsExactly(response);
    }

    @Test
    void increaseStock_shouldIncreaseStockAndReturnMapped() {

        Product product = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").stock(25).build();

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.increaseStock(1L, 5);

        assertThat(product.getStock()).isEqualTo(25);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void increaseStock_shouldThrowResourceNotFoundException_whenMissing() {

        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.increaseStock(99L, 5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void decreaseStock_shouldDecreaseStock_whenSufficientStockAvailable() {

        Product product = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").stock(15).build();

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.decreaseStock(1L, 5);

        assertThat(product.getStock()).isEqualTo(15);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void decreaseStock_shouldThrowInsufficientStockException_whenNotEnoughStock() {

        Product product = sampleProduct();
        product.setStock(3);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.decreaseStock(1L, 5))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Coke");

        verify(productRepository, never()).save(any());
    }

    @Test
    void decreaseStock_shouldThrowResourceNotFoundException_whenMissing() {

        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.decreaseStock(99L, 5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void getProducts_withBlankKeyword_shouldUseFindAll() {

        Product product = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").build();

        when(productRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(product)));
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.getProducts(" ", 0, 10, "name");

        assertThat(result.getContent()).containsExactly(response);
        verify(productRepository, never()).findByNameContainingIgnoreCase(anyString(), any());
    }

    @Test
    void getProducts_withKeyword_shouldUseSearchQuery() {

        Product product = sampleProduct();
        ProductResponse response = ProductResponse.builder().id(1L).name("Coke").build();

        when(productRepository.findByNameContainingIgnoreCase(eq("coke"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.getProducts("coke", 0, 10, "name");

        assertThat(result.getContent()).containsExactly(response);
        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getProducts_withInvalidSortField_shouldDefaultToName() {

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(productRepository.findAll(pageableCaptor.capture())).thenReturn(Page.empty());

        productService.getProducts(null, 0, 10, "not-a-real-field");

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getSort().getOrderFor("name")).isNotNull();
    }
}
