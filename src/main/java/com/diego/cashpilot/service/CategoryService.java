package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.CategoryRequestDTO;
import com.diego.cashpilot.dto.CategoryResponseDTO;
import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.model.enums.TransactionType;
import com.diego.cashpilot.repository.CategoryRepository;
import com.diego.cashpilot.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> list(TransactionType type) {
        User user = getLoggedUser();

        List<Category> categories = type == null
                ? categoryRepository.findAvailable(user)
                : categoryRepository.findAvailableByType(user, type);

        return categories.stream().map(CategoryResponseDTO::of).toList();
    }

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO dto) {
        User user = getLoggedUser();
        String name = dto.getName().trim();

        assertNameAvailable(user, name, dto.getType());

        Category category = Category.builder()
                .name(name)
                .type(dto.getType())
                .icon(blankToNull(dto.getIcon()))
                .user(user)
                .build();

        return CategoryResponseDTO.of(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDTO update(Long id, CategoryRequestDTO dto) {
        User user = getLoggedUser();
        Category category = findOwnEditable(id, user);

        // Mudar o tipo deixaria transações antigas com categoria incompatível.
        if (category.getType() != dto.getType()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo da categoria não pode ser alterado");
        }

        String name = dto.getName().trim();
        if (!category.getName().equalsIgnoreCase(name)) {
            assertNameAvailable(user, name, category.getType());
        }

        category.setName(name);
        category.setIcon(blankToNull(dto.getIcon()));

        return CategoryResponseDTO.of(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        User user = getLoggedUser();
        Category category = findOwnEditable(id, user);

        // As transações não são apagadas: só perdem a categoria.
        transactionRepository.clearCategory(category);
        categoryRepository.delete(category);
    }

    /** Categoria acessível ao usuário e que ele pode alterar (padrões do sistema não podem). */
    private Category findOwnEditable(Long id, User user) {
        Category category = categoryRepository.findAvailableById(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));

        if (category.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Categorias padrão não podem ser alteradas");
        }
        return category;
    }

    private void assertNameAvailable(User user, String name, TransactionType type) {
        boolean taken = categoryRepository.existsByUserIsNullAndNameIgnoreCaseAndType(name, type)
                || categoryRepository.existsByUserAndNameIgnoreCaseAndType(user, name, type);

        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma categoria com esse nome");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private User getLoggedUser() {
        return (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
