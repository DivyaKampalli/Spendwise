package com.spendwise.spendwise_backend.web;

import com.spendwise.spendwise_backend.model.Category;
import com.spendwise.spendwise_backend.repo.CategoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryRepo categoryRepo;

    @GetMapping
    public List<Category> list() {
        return categoryRepo.findAll();
    }
}
