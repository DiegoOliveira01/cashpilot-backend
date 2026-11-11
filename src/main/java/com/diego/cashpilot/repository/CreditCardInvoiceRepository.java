package com.diego.cashpilot.repository;

import com.diego.cashpilot.model.CreditCardInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditCardInvoiceRepository extends JpaRepository<CreditCardInvoice, Long> {

    // Usado para encontrar a fatura de um mês específico
    Optional<CreditCardInvoice> findByCreditCardIdAndReferenceMonth(Long creditCardId, String referenceMonth);

    // Usado no Dashboard para achar as faturas abertas
    @Query("SELECT i FROM CreditCardInvoice i JOIN i.creditCard c WHERE c.user.id = :userId AND i.isPaid = false")
    List<CreditCardInvoice> findUnpaidByUserId(@Param("userId") Long userId);
}