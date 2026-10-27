package com.diego.cashpilot.repository;

import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Padrões do sistema + as do usuário. */
    @Query("select c from Category c where c.user is null or c.user = :user order by c.name")
    List<Category> findAvailable(@Param("user") User user);

    @Query("select c from Category c where (c.user is null or c.user = :user) and c.type = :type order by c.name")
    List<Category> findAvailableByType(@Param("user") User user, @Param("type") TransactionType type);

    /** Só encontra se for padrão ou do próprio usuário (categoria de outro usuário = não encontrada). */
    @Query("select c from Category c where c.id = :id and (c.user is null or c.user = :user)")
    Optional<Category> findAvailableById(@Param("id") Long id, @Param("user") User user);

    boolean existsByUserIsNullAndNameIgnoreCaseAndType(String name, TransactionType type);

    boolean existsByUserAndNameIgnoreCaseAndType(User user, String name, TransactionType type);
}
