package com.spendwise.spendwise_backend.repo;

import com.spendwise.spendwise_backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepo extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
}
