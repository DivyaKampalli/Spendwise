package com.spendwise.spendwise_backend.web;

import com.spendwise.spendwise_backend.model.Category;
import com.spendwise.spendwise_backend.model.Transaction;
import com.spendwise.spendwise_backend.repo.CategoryRepo;
import com.spendwise.spendwise_backend.repo.TransactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionRepo txRepo;
    private final CategoryRepo categoryRepo;

    @GetMapping
    public List<Transaction> list() {
        return txRepo.findAll();
    }

    // quick test endpoint: inserts a sample transaction
    @PostMapping("/sample")
    public Transaction addSample() {
        Category groceries = categoryRepo.findByNameIgnoreCase("Groceries")
                .orElseThrow(() -> new IllegalStateException("No Groceries category"));
        Transaction t = Transaction.builder()
                .postedAt(LocalDate.now())
                .description("Whole Foods")
                .amount(new BigDecimal("-23.45")) // expense
                .category(groceries)
                .build();
        return txRepo.save(t);
    }
}
