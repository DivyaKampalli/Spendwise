package com.spendwise.spendwise_backend.repo;

import com.spendwise.spendwise_backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {

    // You may still have this from earlier; fine to keep.
    Optional<Transaction> findByHash(String hash);

    // NEW: fetch all transactions in a month
    List<Transaction> findByPostedAtBetweenOrderByPostedAtAsc(LocalDate start, LocalDate end);
}
