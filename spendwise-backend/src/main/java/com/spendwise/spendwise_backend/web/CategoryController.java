package com.spendwise.spendwise_backend.web;

import com.spendwise.spendwise_backend.model.Category;
import com.spendwise.spendwise_backend.model.CategoryGroup;
import com.spendwise.spendwise_backend.repo.CategoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepo categoryRepo;

    record CategoryDto(Long id, String name, String group, boolean isIncome) {
        static CategoryDto from(Category c) {
            return new CategoryDto(
                    c.getId(),
                    c.getName(),
                    c.getGroup() == null ? null : c.getGroup().name(),
                    Boolean.TRUE.equals(c.getIsIncome())
            );
        }
    }

    record CreateCategoryRequest(String name, String group, Boolean isIncome) {}

    @GetMapping
    public Map<String, Object> list() {
        var all = categoryRepo.findAll()
                .stream()
                .sorted(Comparator.comparing(c -> c.getName().toLowerCase()))
                .map(CategoryDto::from)
                .collect(Collectors.toList());
        return Map.of("categories", all);
    }

    @PostMapping
    public CategoryDto create(@RequestBody CreateCategoryRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }

        // idempotent: return existing if present
        var existing = categoryRepo.findByNameIgnoreCase(req.name().trim());
        if (existing.isPresent()) return CategoryDto.from(existing.get());

        boolean income = Boolean.TRUE.equals(req.isIncome());
        CategoryGroup group = null;
        if (!income && req.group() != null && !req.group().isBlank()) {
            group = CategoryGroup.valueOf(req.group().trim().toUpperCase());
        }

        var c = Category.builder()
                .name(req.name().trim())
                .isIncome(income)
                .group(income ? null : group) // income must have null group
                .build();

        return CategoryDto.from(categoryRepo.save(c));
    }
}
