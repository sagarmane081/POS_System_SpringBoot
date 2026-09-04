package com.pos.product.repository;

import com.pos.category.entity.Category;
import com.pos.product.entity.Product;
import com.pos.product.enums.ProductStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Category persistCategory(String name) {

        Category category = Category.builder().name(name).build();
        entityManager.persist(category);
        return category;
    }

    private Product product(String name, String sku, int stock, Category category) {

        return Product.builder()
                .name(name)
                .sku(sku)
                .mrp(BigDecimal.TEN)
                .sellingPrice(BigDecimal.valueOf(9))
                .stock(stock)
                .status(ProductStatus.ACTIVE)
                .category(category)
                .build();
    }

    @Test
    void findByNameContainingIgnoreCase_shouldReturnCaseInsensitiveMatches() {

        productRepository.save(product("Coca Cola", "SKU-1", 10, null));
        productRepository.save(product("Pepsi", "SKU-2", 10, null));

        List<Product> results = productRepository.findByNameContainingIgnoreCase("cola");

        assertThat(results).extracting(Product::getName).containsExactly("Coca Cola");
    }

    @Test
    void findByNameContainingIgnoreCase_paged_shouldReturnMatchingPage() {

        productRepository.save(product("Coca Cola", "SKU-1", 10, null));
        productRepository.save(product("Diet Cola", "SKU-2", 10, null));
        productRepository.save(product("Pepsi", "SKU-3", 10, null));

        Page<Product> page = productRepository.findByNameContainingIgnoreCase(
                "cola", PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Coca Cola", "Diet Cola");
    }

    @Test
    void findByCategoryId_shouldReturnOnlyProductsInThatCategory() {

        Category drinks = persistCategory("Drinks");
        Category snacks = persistCategory("Snacks");

        productRepository.save(product("Coca Cola", "SKU-1", 10, drinks));
        productRepository.save(product("Chips", "SKU-2", 10, snacks));

        List<Product> results = productRepository.findByCategoryId(drinks.getId());

        assertThat(results).extracting(Product::getName).containsExactly("Coca Cola");
    }

    @Test
    void findByStockLessThan_shouldReturnOnlyLowStockProducts() {

        productRepository.save(product("Coca Cola", "SKU-1", 5, null));
        productRepository.save(product("Pepsi", "SKU-2", 50, null));

        List<Product> results = productRepository.findByStockLessThan(10);

        assertThat(results).extracting(Product::getName).containsExactly("Coca Cola");
    }

    @Test
    void countByStockLessThan_shouldReturnCorrectCount() {

        productRepository.save(product("Coca Cola", "SKU-1", 5, null));
        productRepository.save(product("Sprite", "SKU-2", 8, null));
        productRepository.save(product("Pepsi", "SKU-3", 50, null));

        assertThat(productRepository.countByStockLessThan(10)).isEqualTo(2L);
    }

    @Test
    void findByIdForUpdate_shouldReturnProduct_whenExists() {

        Product saved = productRepository.save(product("Coca Cola", "SKU-1", 10, null));

        Optional<Product> found = productRepository.findByIdForUpdate(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("SKU-1");
    }

    @Test
    void findByIdForUpdate_shouldReturnEmpty_whenMissing() {

        assertThat(productRepository.findByIdForUpdate(999L)).isEmpty();
    }
}
