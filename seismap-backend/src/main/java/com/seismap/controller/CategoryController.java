package com.seismap.controller;

import com.seismap.model.entity.Category;
import com.seismap.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<Category> list() {
        return categoryService.list();
    }

    @PostMapping
    public Category create(@RequestBody Category category) {
        return categoryService.create(category);
    }
}
