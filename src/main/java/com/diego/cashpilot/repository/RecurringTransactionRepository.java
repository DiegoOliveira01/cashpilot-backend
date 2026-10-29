package com.diego.cashpilot.repository;

import com.diego.cashpilot.model.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    // Busca todas as recorrências ativas que já passaram ou chegaram no dia de hoje
    List<RecurringTransaction> findByIsActiveTrueAndNextOccurrenceDateLessThanEqual(LocalDate today);
}