package com.diego.cashpilot.repository;

import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.Transaction;
import com.diego.cashpilot.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository  extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    Optional<Transaction> findByIdAndUser(Long id, User user);

    List<Transaction> findByUserOrderByDateDescIdDesc(User user, Pageable pageable);

    Optional<Transaction> findFirstByUserOrderByIdDesc(User user);

    List<Transaction> findByUserAndDateBetween(User user, LocalDate start, LocalDate end);

    // método para verificar a existência da ocorrência (Prevenção de Duplicidade)
    boolean existsByRecurringTransactionIdAndRecurrenceDate(Long recurringTransactionId, LocalDate recurrenceDate);

    /** Ao apagar uma categoria, as transações dela ficam "sem categoria". */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Transaction t set t.category = null where t.category = :category")
    int clearCategory(@Param("category") Category category);
}
