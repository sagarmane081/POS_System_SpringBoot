package com.pos.category.repository;

import com.pos.category.entity.Category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void save_shouldPersistAndAssignId() {

        Category saved = categoryRepository.save(
                Category.builder().name("Beverages").description("Drinks").build()
        );

        assertThat(saved.getId()).isNotNull();

        Optional<Category> found = categoryRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Beverages");
    }

    @Test
    void save_shouldThrowDataIntegrityViolationException_whenNameNotUnique() {

        categoryRepository.saveAndFlush(
                Category.builder().name("Beverages").build()
        );

        assertThatThrownBy(() ->
                categoryRepository.saveAndFlush(
                        Category.builder().name("Beverages").build()
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void delete_shouldRemoveCategory() {

        Category saved = categoryRepository.save(
                Category.builder().name("Beverages").build()
        );

        categoryRepository.delete(saved);

        assertThat(categoryRepository.findById(saved.getId())).isEmpty();
    }
}
