package com.spendwise.spendwise_backend.repo;

import com.spendwise.spendwise_backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByHash(String hash);

}
