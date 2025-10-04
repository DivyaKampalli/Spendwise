package com.spendwise.spendwise_backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportPreviewRow(
        LocalDate date,
        String description,
        BigDecimal amount,
        String suggestedCategory,
        String categoryGroup,   // ESSENTIAL / SURPLUS / DEBT / null for income
        boolean duplicate,      // always false now (we removed de-dupe)
        boolean inTargetMonth,
        String hash,
        boolean wouldImport     // == inTargetMonth
) {}
