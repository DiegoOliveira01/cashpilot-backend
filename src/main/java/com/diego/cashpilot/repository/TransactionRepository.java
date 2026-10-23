package com.diego.cashpilot.repository;

import com.diego.cashpilot.model.Transaction;
import com.diego.cashpilot.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository  extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    Optional<Transaction> findByIdAndUser(Long id, User user);

    List<Transaction> findByUserOrderByDateDescIdDesc(User user, Pageable pageable);

    Optional<Transaction> findFirstByUserOrderByIdDesc(User user);
}
