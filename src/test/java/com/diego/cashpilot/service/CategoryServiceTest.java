package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.CategoryRequestDTO;
import com.diego.cashpilot.dto.CategoryResponseDTO;
import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.model.enums.TransactionType;
import com.diego.cashpilot.repository.CategoryRepository;
import com.diego.cashpilot.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CategoryRequestDTO request(String name, TransactionType type, String icon) {
        CategoryRequestDTO dto = new CategoryRequestDTO();
        dto.setName(name);
        dto.setType(type);
        dto.setIcon(icon);
        return dto;
    }

    @Test
    void createsOwnCategoryWithTrimmedName() {
        when(categoryRepository.existsByUserIsNullAndNameIgnoreCaseAndType("Pets", TransactionType.EXPENSE)).thenReturn(false);
        when(categoryRepository.existsByUserAndNameIgnoreCaseAndType(user, "Pets", TransactionType.EXPENSE)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        CategoryResponseDTO result = service.create(request("  Pets ", TransactionType.EXPENSE, "🐶"));

        assertEquals(10L, result.getId());
        assertEquals("Pets", result.getName());
        assertFalse(result.isBuiltIn());
    }

    @Test
    void rejectsNameThatMatchesADefaultCategory() {
        when(categoryRepository.existsByUserIsNullAndNameIgnoreCaseAndType("alimentação", TransactionType.EXPENSE)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(request("alimentação", TransactionType.EXPENSE, null)));

        assertEquals(409, ex.getStatusCode().value());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void defaultCategoryCannotBeEditedOrDeleted() {
        Category defaultCategory = Category.builder().id(1L).name("Alimentação").type(TransactionType.EXPENSE).build();
        when(categoryRepository.findAvailableById(1L, user)).thenReturn(Optional.of(defaultCategory));

        ResponseStatusException onUpdate = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, request("Comida", TransactionType.EXPENSE, null)));
        ResponseStatusException onDelete = assertThrows(ResponseStatusException.class,
                () -> service.delete(1L));

        assertEquals(403, onUpdate.getStatusCode().value());
        assertEquals(403, onDelete.getStatusCode().value());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void typeCannotBeChanged() {
        Category own = Category.builder().id(5L).name("Pets").type(TransactionType.EXPENSE).user(user).build();
        when(categoryRepository.findAvailableById(5L, user)).thenReturn(Optional.of(own));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(5L, request("Pets", TransactionType.INCOME, null)));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void deletingOwnCategoryKeepsItsTransactionsWithoutCategory() {
        Category own = Category.builder().id(5L).name("Pets").type(TransactionType.EXPENSE).user(user).build();
        when(categoryRepository.findAvailableById(5L, user)).thenReturn(Optional.of(own));

        service.delete(5L);

        verify(transactionRepository).clearCategory(own);
        verify(categoryRepository).delete(own);
    }

    @Test
    void categoryOfAnotherUserIsNotFound() {
        when(categoryRepository.findAvailableById(99L, user)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.delete(99L));

        assertEquals(404, ex.getStatusCode().value());
    }
}
