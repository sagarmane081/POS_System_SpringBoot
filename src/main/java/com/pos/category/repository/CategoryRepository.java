package com.pos.category.repository;

import com.pos.category.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {
}