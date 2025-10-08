package com.spendwise.spendwise_backend.web;

import com.spendwise.spendwise_backend.model.Transaction;
import com.spendwise.spendwise_backend.repo.TransactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepo transactionRepo;

    public record TransactionDto(
            Long id,
            LocalDate date,
            String description,
            BigDecimal amount,
            String category,
            String group,
            String hash
    ) {
        public static TransactionDto from(Transaction t) {
            String categoryName = (t.getCategory() != null) ? t.getCategory().getName() : null;
            String groupName = (t.getCategory() != null && t.getCategory().getGroup() != null)
                    ? t.getCategory().getGroup().name()
                    : null;
            return new TransactionDto(
                    t.getId(),
                    t.getPostedAt(),
                    t.getDescription(),
                    t.getAmount(),
                    categoryName,
                    groupName,
                    t.getHash()
            );
        }
    }

    @GetMapping
    public Map<String, Object> listByMonth(@RequestParam("month") String month) {
        // month format: YYYY-MM
        YearMonth ym = YearMonth.parse(month); // ISO "yyyy-MM"
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Transaction> entities = transactionRepo.findByPostedAtBetweenOrderByPostedAtAsc(start, end);
        List<TransactionDto> txs = entities.stream().map(TransactionDto::from).collect(Collectors.toList());

        BigDecimal income = entities.stream()
                .map(Transaction::getAmount)
                .filter(a -> a.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenses = entities.stream()
                .map(Transaction::getAmount)
                .filter(a -> a.signum() < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "month", month,
                "count", txs.size(),
                "income", income,
                "expenses", expenses,
                "net", income.subtract(expenses),
                "transactions", txs
        );
    }
}
