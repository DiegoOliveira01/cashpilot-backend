package com.diego.cashpilot.controller;

import com.diego.cashpilot.dto.CategoryRequestDTO;
import com.diego.cashpilot.dto.CategoryResponseDTO;
import com.diego.cashpilot.model.enums.TransactionType;
import com.diego.cashpilot.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponseDTO> list(@RequestParam(required = false) TransactionType type){
        return categoryService.list(type);
    }

    @PostMapping
    public CategoryResponseDTO create(@RequestBody @Valid CategoryRequestDTO dto){
        return categoryService.create(dto);
    }

    @PutMapping("/{id}")
    public CategoryResponseDTO update(@PathVariable Long id, @RequestBody @Valid CategoryRequestDTO dto){
        return categoryService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        categoryService.delete(id);
    }
}
