package com.spendwise.spendwise_backend.config;

import com.spendwise.spendwise_backend.model.Category;
import com.spendwise.spendwise_backend.model.CategoryGroup;
import com.spendwise.spendwise_backend.repo.CategoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepo categoryRepo;

    @Override
    public void run(String... args) {
        record Seed(String name, boolean isIncome, CategoryGroup group) {}

        var seeds = java.util.List.of(
                // ESSENTIAL
                new Seed("Rent", false, CategoryGroup.ESSENTIAL),
                new Seed("Utilities", false, CategoryGroup.ESSENTIAL),
                new Seed("Groceries", false, CategoryGroup.ESSENTIAL),
                new Seed("Fuel", false, CategoryGroup.ESSENTIAL),
                new Seed("Transport", false, CategoryGroup.ESSENTIAL),
                new Seed("Health Insurance", false, CategoryGroup.ESSENTIAL),
                new Seed("Medical", false, CategoryGroup.ESSENTIAL),
                new Seed("Internet", false, CategoryGroup.ESSENTIAL),
                new Seed("Phone", false, CategoryGroup.ESSENTIAL),

                // SURPLUS
                new Seed("Eating Out", false, CategoryGroup.SURPLUS),
                new Seed("Coffee", false, CategoryGroup.SURPLUS),
                new Seed("Shopping", false, CategoryGroup.SURPLUS),
                new Seed("Entertainment", false, CategoryGroup.SURPLUS),
                new Seed("Travel", false, CategoryGroup.SURPLUS),
                new Seed("Subscriptions", false, CategoryGroup.SURPLUS),
                new Seed("Uncategorized", false, CategoryGroup.SURPLUS),

                // DEBT
                new Seed("Mortgage", false, CategoryGroup.DEBT),
                new Seed("Home Loan", false, CategoryGroup.DEBT),
                new Seed("Car Loan", false, CategoryGroup.DEBT),
                new Seed("Student Loan", false, CategoryGroup.DEBT),
                new Seed("Personal Loan", false, CategoryGroup.DEBT),
                new Seed("Credit Card Payment", false, CategoryGroup.DEBT),
                new Seed("Debt Repayment", false, CategoryGroup.DEBT),

                // INCOME (no group)
                new Seed("Income", true, null)
        );

        for (var s : seeds) {
            var existing = categoryRepo.findByNameIgnoreCase(s.name());
            if (existing.isEmpty()) {
                categoryRepo.save(Category.builder()
                        .name(s.name())
                        .isIncome(s.isIncome())
                        .group(s.group()) // may be null for income
                        .build());
            } else {
                var c = existing.get();
                boolean changed = false;
                if (c.getIsIncome() != s.isIncome()) { c.setIsIncome(s.isIncome()); changed = true; }
                if (s.group() != null && c.getGroup() != s.group()) { c.setGroup(s.group()); changed = true; }
                if (changed) categoryRepo.save(c);
            }
        }
    }

}
